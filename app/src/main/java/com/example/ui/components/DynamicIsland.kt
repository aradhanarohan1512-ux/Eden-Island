package com.example.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocalAtm
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.database.RatingEntity
import com.example.data.network.RetrofitClient
import com.example.ui.viewmodel.IslandState
import com.squareup.moshi.Types

@Composable
fun DynamicIsland(
    state: IslandState,
    onToggleExpand: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Dynamic styles based on island state
    val (targetWidth, targetHeight) = when (state) {
        is IslandState.IDLE -> Pair(140.dp, 36.dp)
        is IslandState.LOADING -> Pair(220.dp, 40.dp)
        is IslandState.COMPACT -> Pair(240.dp, 44.dp)
        is IslandState.EXPANDED -> Pair(330.dp, 360.dp)
    }

    val cornerRadius = when (state) {
        is IslandState.EXPANDED -> 28.dp
        else -> 22.dp
    }

    val width by animateDpAsState(targetValue = targetWidth, label = "island_width")
    val height by animateDpAsState(targetValue = targetHeight, label = "island_height")

    // Pulsing loading border transition
    val infiniteTransition = rememberInfiniteTransition(label = "pulse_loading")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    // Border highlights depending on status
    val borderBrush = when (state) {
        is IslandState.LOADING -> Brush.linearGradient(
            listOf(
                Color(0xFFFFD700).copy(alpha = pulseAlpha),
                Color(0xFFFFFFFF).copy(alpha = pulseAlpha),
                Color(0xFFFFD700).copy(alpha = pulseAlpha)
            )
        )
        is IslandState.COMPACT -> {
            val scoreColor = getScoreColor(state.score)
            Brush.linearGradient(listOf(scoreColor.copy(alpha = 0.8f), scoreColor.copy(alpha = 0.3f)))
        }
        is IslandState.EXPANDED -> {
            val scoreColor = getScoreColor(state.rating.score)
            Brush.linearGradient(listOf(scoreColor.copy(alpha = 0.9f), Color(0xFF1E1E2C)))
        }
        else -> Brush.linearGradient(listOf(Color(0xFF3A3A4A), Color(0xFF1A1A24)))
    }

    Box(
        modifier = modifier
            .width(width)
            .height(height)
            .clip(RoundedCornerShape(cornerRadius))
            .background(Color(0xFF0C0C12))
            .border(
                width = if (state is IslandState.IDLE) 1.dp else 1.5.dp,
                brush = borderBrush,
                shape = RoundedCornerShape(cornerRadius)
            )
            .clickable {
                if (state is IslandState.COMPACT || state is IslandState.EXPANDED) {
                    onToggleExpand()
                }
            }
            .testTag("dynamic_island")
            .padding(horizontal = 14.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        AnimatedContent(
            targetState = state,
            transitionSpec = {
                (fadeIn(animationSpec = tween(220, delayMillis = 90)) togetherWith
                        fadeOut(animationSpec = tween(90)))
                    .using(SizeTransform(clip = false))
            },
            label = "island_content"
        ) { targetState ->
            when (targetState) {
                is IslandState.IDLE -> {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "✝",
                            color = Color(0xFFFFD700),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Eden Island",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            letterSpacing = 0.5.sp
                        )
                    }
                }
                is IslandState.LOADING -> {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(
                            color = Color(0xFFFFD700),
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Scriptural Discernment...",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Normal
                        )
                    }
                }
                is IslandState.COMPACT -> {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = "✝",
                                color = getScoreColor(targetState.score),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = targetState.title,
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${targetState.score}%",
                                color = getScoreColor(targetState.score),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.testTag("compact_score")
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = targetState.emojis,
                                fontSize = 12.sp,
                                maxLines = 1
                            )
                        }
                    }
                }
                is IslandState.EXPANDED -> {
                    val rating = targetState.rating
                    ExpandedIslandContent(
                        rating = rating,
                        onCollapse = onToggleExpand
                    )
                }
            }
        }
    }
}

@Composable
fun ExpandedIslandContent(
    rating: RatingEntity,
    onCollapse: () -> Unit
) {
    val scoreColor = getScoreColor(rating.score)
    val verses = getBibleVersesList(rating.versesJson)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top Notch Bar
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "BIBLICAL DISCERNMENT REPORT",
                color = Color.Gray,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Text(
                text = rating.emojis,
                fontSize = 16.sp
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Title text
        Text(
            text = rating.title,
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 4.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Score row
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier
                .background(Color(0xFF161622), RoundedCornerShape(12.dp))
                .padding(horizontal = 14.dp, vertical = 8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .background(scoreColor.copy(alpha = 0.15f), CircleShape)
                    .border(2.dp, scoreColor, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "${rating.score}",
                    color = scoreColor,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Black
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = rating.verdict,
                    color = scoreColor,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.testTag("expanded_verdict")
                )
                Text(
                    text = "Scriptural Alignment",
                    color = Color.LightGray,
                    fontSize = 10.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Theological explanation
        Text(
            text = rating.explanation,
            color = Color.White,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            textAlign = TextAlign.Start,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Relevant Bible Verses
        if (verses.isNotEmpty()) {
            Text(
                text = "Supporting Scripture",
                color = Color(0xFFFFD700),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp,
                modifier = Modifier.align(Alignment.Start)
            )
            Spacer(modifier = Modifier.height(4.dp))
            verses.forEach { verse ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF1E1E2C).copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                        .border(0.5.dp, Color(0xFFFFD700).copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                        .padding(8.dp)
                ) {
                    Text(
                        text = "“${verse["text"]}”",
                        color = Color.LightGray,
                        fontSize = 11.sp,
                        fontStyle = FontStyle.Italic,
                        lineHeight = 14.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "— ${verse["reference"]}",
                        color = Color(0xFFFFD700),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.End,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Tap to collapse
        Icon(
            imageVector = Icons.Default.ExpandLess,
            contentDescription = "Collapse report",
            tint = Color.Gray,
            modifier = Modifier
                .size(24.dp)
                .clickable { onCollapse() }
        )
    }
}

// Helper to deserialize verses from JSON stored in Room DB
fun getBibleVersesList(json: String): List<Map<String, String>> {
    return try {
        val type = Types.newParameterizedType(List::class.java, Map::class.java)
        val adapter = RetrofitClient.resultMoshi.adapter<List<Map<String, String>>>(type)
        adapter.fromJson(json) ?: emptyList()
    } catch (e: Exception) {
        emptyList()
    }
}

// Helper to determine score color based on alignment
fun getScoreColor(score: Int): Color {
    return when {
        score >= 90 -> Color(0xFF00E676) // Radiant Green (Highly alignment)
        score >= 70 -> Color(0xFF4CAF50) // Balanced Green (Good)
        score >= 50 -> Color(0xFFFFB300) // Discernment Amber/Gold (Neutral/Mixed)
        score >= 30 -> Color(0xFFFB8C00) // Caution Orange (Misaligned)
        else -> Color(0xFFE53935) // Urgent Red (Scripturally contradictory)
    }
}
