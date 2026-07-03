package com.example.ui.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import com.example.data.database.AppDatabase
import com.example.data.database.RatingEntity
import com.example.data.network.DiscernmentResult
import com.example.data.network.GeminiRequest
import com.example.data.network.ContentPart
import com.example.data.network.PartText
import com.example.data.network.GenerationConfig
import com.example.data.network.RetrofitClient
import com.example.data.repository.RatingRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed class IslandState {
    object IDLE : IslandState()
    object LOADING : IslandState()
    data class COMPACT(val score: Int, val emojis: String, val title: String) : IslandState()
    data class EXPANDED(val rating: RatingEntity) : IslandState()
}

class DiscernmentViewModel(
    application: Application,
    private val repository: RatingRepository
) : AndroidViewModel(application) {

    private val _allRatings = MutableStateFlow<List<RatingEntity>>(emptyList())
    val allRatings: StateFlow<List<RatingEntity>> = _allRatings.asStateFlow()

    private val _analyzingUrl = MutableStateFlow("")
    val analyzingUrl: StateFlow<String> = _analyzingUrl.asStateFlow()

    private val _activeRating = MutableStateFlow<RatingEntity?>(null)
    val activeRating: StateFlow<RatingEntity?> = _activeRating.asStateFlow()

    private val _islandState = MutableStateFlow<IslandState>(IslandState.IDLE)
    val islandState: StateFlow<IslandState> = _islandState.asStateFlow()

    private val _showBrowser = MutableStateFlow(false)
    val showBrowser: StateFlow<Boolean> = _showBrowser.asStateFlow()

    private val _browserUrl = MutableStateFlow("https://www.youtube.com")
    val browserUrl: StateFlow<String> = _browserUrl.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    init {
        // Observe database flow
        viewModelScope.launch {
            repository.allRatings.collectLatest { ratings ->
                _allRatings.value = ratings
            }
        }
    }

    fun setAnalyzingUrl(url: String) {
        _analyzingUrl.value = url
    }

    fun setBrowserUrl(url: String) {
        _browserUrl.value = url
    }

    fun setShowBrowser(show: Boolean) {
        _showBrowser.value = show
    }

    fun setIslandState(state: IslandState) {
        _islandState.value = state
    }

    fun clearError() {
        _errorMessage.value = null
    }

    fun toggleIslandExpanded() {
        val current = _islandState.value
        if (current is IslandState.COMPACT) {
            val rating = _activeRating.value
            if (rating != null) {
                _islandState.value = IslandState.EXPANDED(rating)
            }
        } else if (current is IslandState.EXPANDED) {
            val rating = current.rating
            _islandState.value = IslandState.COMPACT(rating.score, rating.emojis, rating.title)
        }
    }

    fun collapseIsland() {
        val current = _islandState.value
        if (current is IslandState.EXPANDED) {
            val rating = current.rating
            _islandState.value = IslandState.COMPACT(rating.score, rating.emojis, rating.title)
        }
    }

    fun deleteRating(ratingId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteById(ratingId)
            if (_activeRating.value?.id == ratingId) {
                _activeRating.value = null
                _islandState.value = IslandState.IDLE
            }
        }
    }

    fun clearHistory() {
        viewModelScope.launch(Dispatchers.IO) {
            repository.clearAll()
            _activeRating.value = null
            _islandState.value = IslandState.IDLE
        }
    }

    fun selectHistoryItem(rating: RatingEntity) {
        _activeRating.value = rating
        _islandState.value = IslandState.COMPACT(rating.score, rating.emojis, rating.title)
    }

    fun analyzeContent(input: String, isUrl: Boolean = false) {
        if (input.isBlank()) return

        _islandState.value = IslandState.LOADING
        _errorMessage.value = null

        viewModelScope.launch {
            try {
                val apiKey = BuildConfig.GEMINI_API_KEY
                if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
                    throw IllegalStateException("Gemini API Key is not set. Please add it via the Secrets Panel in AI Studio.")
                }

                val prompt = buildTheologicalPrompt(input, isUrl)
                val request = GeminiRequest(
                    contents = listOf(ContentPart(parts = listOf(PartText(text = prompt)))),
                    generationConfig = GenerationConfig(
                        responseMimeType = "application/json",
                        temperature = 0.2
                    )
                )

                val response = withContext(Dispatchers.IO) {
                    RetrofitClient.service.generateContent(apiKey, request)
                }

                val rawText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                    ?: throw IllegalStateException("Received an empty response from Gemini.")

                val cleanedJson = cleanJsonResponse(rawText)
                
                val resultAdapter = RetrofitClient.resultMoshi.adapter(DiscernmentResult::class.java)
                val result = withContext(Dispatchers.Default) {
                    resultAdapter.fromJson(cleanedJson)
                } ?: throw IllegalStateException("Failed to parse discernment evaluation.")

                // Infer a title from url or keywords
                val displayTitle = if (isUrl) {
                    extractTitleFromUrl(input, result.verdict)
                } else {
                    input
                }

                // Serialize verses back to JSON string for DB
                val versesAdapter = RetrofitClient.resultMoshi.adapter(List::class.java)
                // Let's create a customized map structure for the DB storage
                val versesListMap = result.verses.map {
                    mapOf("reference" to it.reference, "text" to it.text)
                }
                val versesJson = withContext(Dispatchers.Default) {
                    versesAdapter.toJson(versesListMap)
                }

                val entity = RatingEntity(
                    url = input,
                    title = displayTitle,
                    score = result.score,
                    verdict = result.verdict,
                    emojis = result.emojis,
                    explanation = result.explanation,
                    versesJson = versesJson,
                    timestamp = System.currentTimeMillis()
                )

                // Save to Room DB
                withContext(Dispatchers.IO) {
                    repository.insert(entity)
                }

                // Update active state
                _activeRating.value = entity
                _islandState.value = IslandState.COMPACT(entity.score, entity.emojis, entity.title)

            } catch (e: Exception) {
                Log.e("DiscernmentViewModel", "Analysis failed", e)
                _errorMessage.value = e.localizedMessage ?: "Unknown theological evaluation error."
                _islandState.value = IslandState.IDLE
            }
        }
    }

    private fun cleanJsonResponse(rawResponse: String): String {
        var cleaned = rawResponse.trim()
        if (cleaned.startsWith("```json")) {
            cleaned = cleaned.removePrefix("```json")
        } else if (cleaned.startsWith("```")) {
            cleaned = cleaned.removePrefix("```")
        }
        if (cleaned.endsWith("```")) {
            cleaned = cleaned.removeSuffix("```")
        }
        return cleaned.trim()
    }

    private fun extractTitleFromUrl(url: String, verdict: String): String {
        return try {
            if (url.contains("youtube.com/watch") || url.contains("youtu.be/")) {
                val videoId = if (url.contains("v=")) {
                    url.split("v=")[1].split("&")[0]
                } else {
                    url.substringAfterLast("/")
                }
                "YouTube Video ($videoId)"
            } else {
                val cleanUrl = url.removePrefix("https://").removePrefix("http://").removePrefix("www.")
                val domain = cleanUrl.split("/").firstOrNull() ?: "Web Content"
                "Web Content ($domain)"
            }
        } catch (e: Exception) {
            "Analyzed URL Source"
        }
    }

    private fun buildTheologicalPrompt(input: String, isUrl: Boolean): String {
        val intro = if (isUrl) {
            "Analyze the contents of the following URL: '$input'."
        } else {
            "Analyze the following topic or statement: '$input'."
        }

        return """
            You are a theological expert in Christian Discernment, Biblical Scholarship, and Christian Knowledge.
            Your task is to analyze the content/URL described below and rate how well it aligns with Biblical truths, Christian values, and God's word.
            
            $intro
            
            Evaluate this carefully based on standard Christian doctrines, scriptures, and teachings.
            
            Please provide a structured analysis containing:
            1. "score": An integer between 0 and 100.
               - 90 to 100: Highly edifying, biblically sound, affirmation of faith, promotes love, charity, and truth.
               - 70 to 89: Good, constructive secular topic, moral/virtuous themes, aligned general truth.
               - 50 to 69: Neutral, secular content. Missing spiritual focus but harmless, or mixed values requiring discernment.
               - 30 to 49: Misaligned, minor unbiblical elements, worldly distractions, mild vanity, or slight deceptions.
               - 0 to 29: Scripturally contradictory, promotes pride, hatred, blasphemy, severe deception, or explicit anti-biblical teachings.
            2. "verdict": A short, elegant, impactful phrase describing the rating status (e.g. "Highly Edifying", "Biblically Sound", "Secular / Discernment Advised", "Worldly / Use Caution", "Scripturally Contradictory").
            3. "emojis": A string of 3-4 appropriate Christian/Biblical/thematic emojis corresponding to the score (e.g. 😇🕊️✨, 🙏📖👍, 🤔⚠️⚖️, 🚫📉👿, 🚨❌🔥).
            4. "explanation": A detailed, beautiful theological description (3-4 sentences) outlining the specific Christian virtues upheld or scriptural principles contradicted. Be encouraging yet faithful to truth.
            5. "verses": A list of 1-2 relevant Bible verses (with "reference" and "text") that provide direct spiritual insight, guidance, or refutation for this topic.
            
            You MUST output the result in a STRICT JSON format with exactly the schema below. Do not include markdown code block formatting (like ```json) in your actual response text, just return the plain JSON string.
            
            JSON Schema:
            {
              "score": Int,
              "verdict": "String",
              "emojis": "String",
              "explanation": "String",
              "verses": [
                {
                  "reference": "String",
                  "text": "String"
                }
              ]
            }
        """.trimIndent()
    }
}

@Suppress("UNCHECKED_CAST")
class DiscernmentViewModelFactory(
    private val application: Application,
    private val repository: RatingRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DiscernmentViewModel::class.java)) {
            return DiscernmentViewModel(application, repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
