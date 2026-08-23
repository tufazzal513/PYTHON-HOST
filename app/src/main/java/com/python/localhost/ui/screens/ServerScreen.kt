package com.python.localhost.ui.screens

import android.webkit.WebView
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavHostController
import com.python.localhost.di.AppContainer
import com.python.localhost.ui.components.IdeTopBar
import java.net.URLDecoder

@Composable
fun ServerScreen(nav: NavHostController, container: AppContainer, projectId: String, encodedUrl: String) {
    val url = try {
        URLDecoder.decode(encodedUrl, "UTF-8")
    } catch (_: Exception) {
        encodedUrl
    }
    Scaffold(
        topBar = { IdeTopBar(title = "Local Server", subtitle = url, onBack = { nav.popBackStack() }) },
    ) { padding ->
        AndroidView(
            factory = { context -> WebView(context).apply { loadUrl(url) } },
            modifier = Modifier.fillMaxSize().padding(padding),
        )
    }
}
