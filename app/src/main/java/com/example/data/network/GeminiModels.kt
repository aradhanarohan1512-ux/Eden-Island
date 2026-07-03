package com.example.data.network

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class GeminiRequest(
    @Json(name = "contents") val contents: List<ContentPart>,
    @Json(name = "generationConfig") val generationConfig: GenerationConfig? = null
)

@JsonClass(generateAdapter = true)
data class ContentPart(
    @Json(name = "parts") val parts: List<PartText>
)

@JsonClass(generateAdapter = true)
data class PartText(
    @Json(name = "text") val text: String
)

@JsonClass(generateAdapter = true)
data class GenerationConfig(
    @Json(name = "responseMimeType") val responseMimeType: String? = null,
    @Json(name = "temperature") val temperature: Double? = null
)

// --- Response Classes ---

@JsonClass(generateAdapter = true)
data class GeminiResponse(
    @Json(name = "candidates") val candidates: List<Candidate>?
)

@JsonClass(generateAdapter = true)
data class Candidate(
    @Json(name = "content") val content: ResponseContent?
)

@JsonClass(generateAdapter = true)
data class ResponseContent(
    @Json(name = "parts") val parts: List<ResponsePart>?
)

@JsonClass(generateAdapter = true)
data class ResponsePart(
    @Json(name = "text") val text: String?
)

// --- Structured Discernment Output Class ---

@JsonClass(generateAdapter = true)
data class DiscernmentResult(
    @Json(name = "score") val score: Int,
    @Json(name = "verdict") val verdict: String,
    @Json(name = "emojis") val emojis: String,
    @Json(name = "explanation") val explanation: String,
    @Json(name = "verses") val verses: List<BibleVerseModel>
)

@JsonClass(generateAdapter = true)
data class BibleVerseModel(
    @Json(name = "reference") val reference: String,
    @Json(name = "text") val text: String
)
