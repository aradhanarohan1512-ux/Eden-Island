package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.OndemandVideo
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.database.RatingEntity
import com.example.ui.components.DynamicIsland
import com.example.ui.components.YouTubeBrowser
import com.example.ui.components.getScoreColor
import com.example.ui.viewmodel.DiscernmentViewModel
import com.example.ui.viewmodel.IslandState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun MainScreen(
    viewModel: DiscernmentViewModel,
    modifier: Modifier = Modifier
) {
    val allRatings by viewModel.allRatings.collectAsState()
    val analyzingUrl by viewModel.analyzingUrl.collectAsState()
    val islandState by viewModel.islandState.collectAsState()
    val showBrowser by viewModel.showBrowser.collectAsState()
    val browserUrl by viewModel.browserUrl.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    val context = LocalContext.current

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = Color(0xFF07070B)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Simulated Phone Bezel Notch Area for Dynamic Island
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF000000))
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                DynamicIsland(
                    state = islandState,
                    onToggleExpand = { viewModel.toggleIslandExpanded() }
                )
            }

            // Tab bar to switch views
            TabRow(
                selectedTabIndex = if (showBrowser) 1 else 0,
                containerColor = Color(0xFF11111A),
                contentColor = Color.White,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[if (showBrowser) 1 else 0]),
                        color = Color(0xFFFFD700)
                    )
                },
                divider = {}
            ) {
                Tab(
                    selected = !showBrowser,
                    onClick = { viewModel.setShowBrowser(false) },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.MenuBook,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Discernment Portal", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    },
                    selectedContentColor = Color(0xFFFFD700),
                    unselectedContentColor = Color.Gray,
                    modifier = Modifier.testTag("tab_portal")
                )
                Tab(
                    selected = showBrowser,
                    onClick = { viewModel.setShowBrowser(true) },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.OndemandVideo,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Christian YouTube TV", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    },
                    selectedContentColor = Color(0xFFFFD700),
                    unselectedContentColor = Color.Gray,
                    modifier = Modifier.testTag("tab_youtube")
                )
            }

            // Content view switching
            if (showBrowser) {
                // Live YouTube TV webview
                YouTubeBrowser(
                    initialUrl = browserUrl,
                    onUrlChanged = { url, title ->
                        viewModel.setBrowserUrl(url)
                        viewModel.analyzeContent(url, isUrl = true)
                    },
                    modifier = Modifier.weight(1f)
                )
            } else {
                // Main portal screen
                PortalDashboard(
                    allRatings = allRatings,
                    analyzingUrl = analyzingUrl,
                    errorMessage = errorMessage,
                    onUrlChanged = { viewModel.setAnalyzingUrl(it) },
                    onAnalyze = { input ->
                        val isUrl = input.startsWith("http://") || input.startsWith("https://") || input.contains(".")
                        viewModel.analyzeContent(input, isUrl = isUrl)
                    },
                    onSelectHistory = { rating ->
                        viewModel.selectHistoryItem(rating)
                    },
                    onDeleteHistory = { id ->
                        viewModel.deleteRating(id)
                    },
                    onClearHistory = {
                        viewModel.clearHistory()
                    },
                    onClearError = {
                        viewModel.clearError()
                    },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun PortalDashboard(
    allRatings: List<RatingEntity>,
    analyzingUrl: String,
    errorMessage: String?,
    onUrlChanged: (String) -> Unit,
    onAnalyze: (String) -> Unit,
    onSelectHistory: (RatingEntity) -> Unit,
    onDeleteHistory: (Int) -> Unit,
    onClearHistory: () -> Unit,
    onClearError: () -> Unit,
    modifier: Modifier = Modifier
) {
    val quickSuggestions = listOf(
        "Sermon on the Mount alignment",
        "https://www.youtube.com/watch?v=dQw4w9WgXcQ", // Rick Astley
        "Apostles' Creed details",
        "Scientific theories on Noah's Ark",
        "Christian values in family building"
    )

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Welcoming card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF11111C)),
                border = BorderStroke(1.dp, Color(0xFFFFD700).copy(alpha = 0.15f)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "✝ Eden Discernment Portal",
                        color = Color(0xFFFFD700),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.5.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Test videos and topics against the Light of Scripture. Receive instant theological ratings, scripture references, and insights in the Eden Island notch above.",
                        color = Color.LightGray,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 16.sp
                    )
                }
            }
        }

        // Search Input Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF161626)),
                border = BorderStroke(0.5.dp, Color.Gray.copy(alpha = 0.3f)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Biblical Assessment",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )

                    OutlinedTextField(
                        value = analyzingUrl,
                        onValueChange = onUrlChanged,
                        placeholder = {
                            Text(
                                "Enter YouTube URL or type a topic/statement...",
                                fontSize = 12.sp,
                                color = Color.Gray
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                tint = Color(0xFFFFD700)
                            )
                        },
                        trailingIcon = {
                            if (analyzingUrl.isNotEmpty()) {
                                IconButton(onClick = { onUrlChanged("") }) {
                                    Icon(
                                        imageVector = Icons.Default.Clear,
                                        contentDescription = "Clear text",
                                        tint = Color.Gray
                                    )
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("search_input_field"),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFFFFD700),
                            unfocusedBorderColor = Color.DarkGray,
                            focusedContainerColor = Color(0xFF0C0C12),
                            unfocusedContainerColor = Color(0xFF0C0C12),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Button(
                            onClick = { onAnalyze(analyzingUrl) },
                            enabled = analyzingUrl.isNotBlank(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFFFD700),
                                contentColor = Color.Black,
                                disabledContainerColor = Color.Gray.copy(alpha = 0.2f),
                                disabledContentColor = Color.Gray
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                                .testTag("analyze_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Inspect Spiritual Alignment",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // Quick suggestions list
        item {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "Quick Discovers",
                    color = Color.Gray,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val sample1 = "Sermon on the Mount"
                    val sample2 = "Christian charity"
                    
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(Color(0xFF111118), RoundedCornerShape(10.dp))
                            .border(0.5.dp, Color.Gray.copy(alpha = 0.2f), RoundedCornerShape(10.dp))
                            .clickable {
                                onUrlChanged(sample1)
                                onAnalyze(sample1)
                            }
                            .padding(10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(sample1, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(Color(0xFF111118), RoundedCornerShape(10.dp))
                            .border(0.5.dp, Color.Gray.copy(alpha = 0.2f), RoundedCornerShape(10.dp))
                            .clickable {
                                onUrlChanged(sample2)
                                onAnalyze(sample2)
                            }
                            .padding(10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(sample2, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }

        // Error message popup
        if (errorMessage != null) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF2C1616)),
                    border = BorderStroke(1.dp, Color.Red.copy(alpha = 0.4f)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Error",
                            tint = Color.Red,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = errorMessage,
                            color = Color.LightGray,
                            fontSize = 11.sp,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = onClearError) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Dismiss error",
                                tint = Color.LightGray,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }

        // Discernment Log (History)
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Discernment Log (${allRatings.size})",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )

                if (allRatings.isNotEmpty()) {
                    Text(
                        text = "Clear Log",
                        color = Color(0xFFFFD700),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .clickable { onClearHistory() }
                            .padding(4.dp)
                            .testTag("clear_log_btn")
                    )
                }
            }
        }

        if (allRatings.isEmpty()) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.MenuBook,
                        contentDescription = null,
                        tint = Color.DarkGray,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Your Discernment Log is empty.",
                        color = Color.Gray,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "Enter a video topic or use YouTube TV to start logging.",
                        color = Color.DarkGray,
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            items(allRatings, key = { it.id }) { rating ->
                RatingHistoryCard(
                    rating = rating,
                    onSelect = { onSelectHistory(rating) },
                    onDelete = { onDeleteHistory(rating.id) }
                )
            }
        }

        // Bible reminder text
        item {
            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = Color.DarkGray.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "“Test all things; hold fast what is good.” — 1 Thessalonians 5:21",
                color = Color.Gray,
                fontSize = 11.sp,
                fontStyle = FontStyle.Italic,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun RatingHistoryCard(
    rating: RatingEntity,
    onSelect: () -> Unit,
    onDelete: () -> Unit
) {
    val scoreColor = getScoreColor(rating.score)
    val formattedDate = remember(rating.timestamp) {
        try {
            val sdf = SimpleDateFormat("MMM dd, yyyy · hh:mm a", Locale.getDefault())
            sdf.format(Date(rating.timestamp))
        } catch (e: Exception) {
            "Just now"
        }
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF111118)),
        border = BorderStroke(0.5.dp, scoreColor.copy(alpha = 0.2f)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect() }
            .testTag("history_item_card_${rating.id}")
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Small Score Badge
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .background(scoreColor.copy(alpha = 0.1f), CircleShape)
                    .border(1.5.dp, scoreColor, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "${rating.score}",
                    color = scoreColor,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Main info
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = rating.verdict,
                        color = scoreColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = rating.emojis,
                        fontSize = 12.sp
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = rating.title,
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = formattedDate,
                    color = Color.Gray,
                    fontSize = 9.sp
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete rating",
                    tint = Color.Gray.copy(alpha = 0.6f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

// BorderStroke helper
@Composable
fun BorderStroke(width: androidx.compose.ui.unit.Dp, color: Color): androidx.compose.foundation.BorderStroke {
    return remember(width, color) {
        androidx.compose.foundation.BorderStroke(width, color)
    }
}
