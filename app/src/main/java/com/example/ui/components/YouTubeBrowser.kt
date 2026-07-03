package com.example.ui.components

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SmartDisplay
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun YouTubeBrowser(
    initialUrl: String,
    onUrlChanged: (String, String) -> Unit, // passes (url, title)
    modifier: Modifier = Modifier
) {
    var webViewInstance by remember { mutableStateOf<WebView?>(null) }
    var currentUrl by remember { mutableStateOf(initialUrl) }
    var currentTitle by remember { mutableStateOf("YouTube Browser") }
    var isLoading by remember { mutableStateOf(false) }
    var progress by remember { mutableStateOf(0f) }

    // Sync external changes to webview if needed
    LaunchedEffect(initialUrl) {
        if (initialUrl != currentUrl) {
            webViewInstance?.loadUrl(initialUrl)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0F0F14))
    ) {
        // Simple Browser Controls
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF161622))
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = {
                    if (webViewInstance?.canGoBack() == true) {
                        webViewInstance?.goBack()
                    }
                },
                enabled = webViewInstance?.canGoBack() == true,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = if (webViewInstance?.canGoBack() == true) Color.White else Color.Gray,
                    modifier = Modifier.size(20.dp)
                )
            }

            IconButton(
                onClick = {
                    webViewInstance?.loadUrl("https://m.youtube.com")
                },
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Home,
                    contentDescription = "YouTube Home",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(4.dp))

            // Address Card
            Row(
                modifier = Modifier
                    .weight(1f)
                    .height(36.dp)
                    .background(Color(0xFF0C0C12), RoundedCornerShape(18.dp))
                    .border(0.5.dp, Color.DarkGray, RoundedCornerShape(18.dp))
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.SmartDisplay,
                    contentDescription = "Secure Video connection",
                    tint = Color(0xFFFFD700),
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (currentUrl.contains("watch?v=")) {
                        "📺 YouTube Video Loaded"
                    } else {
                        currentUrl.removePrefix("https://").removePrefix("http://")
                    },
                    color = Color.LightGray,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.width(4.dp))

            IconButton(
                onClick = { webViewInstance?.reload() },
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Reload",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        if (isLoading) {
            LinearProgressIndicator(
                progress = { progress },
                color = Color(0xFFFFD700),
                trackColor = Color(0xFF161622),
                modifier = Modifier.fillMaxWidth().height(2.dp)
            )
        } else {
            Spacer(modifier = Modifier.height(2.dp))
        }

        // The real Android Webview container
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .testTag("youtube_webview")
        ) {
            AndroidView(
                factory = { context ->
                    WebView(context).apply {
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.mediaPlaybackRequiresUserGesture = false
                        settings.userAgentString = "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/114.0.0.0 Mobile Safari/537.36"

                        webViewClient = object : WebViewClient() {
                            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                super.onPageStarted(view, url, favicon)
                                isLoading = true
                                url?.let { currentUrl = it }
                            }

                            override fun onPageFinished(view: WebView?, url: String?) {
                                super.onPageFinished(view, url)
                                isLoading = false
                                url?.let {
                                    currentUrl = it
                                    val title = view?.title ?: "YouTube Video"
                                    currentTitle = title
                                    // Trigger theological analysis if url changed and is a watchable video
                                    if (it.contains("youtube.com/watch") || it.contains("youtu.be/")) {
                                        onUrlChanged(it, title)
                                    }
                                }
                            }
                        }

                        webChromeClient = object : WebChromeClient() {
                            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                super.onProgressChanged(view, newProgress)
                                progress = newProgress / 100f
                            }
                        }

                        loadUrl(currentUrl)
                        webViewInstance = this
                    }
                },
                update = { webView ->
                    webViewInstance = webView
                },
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}
