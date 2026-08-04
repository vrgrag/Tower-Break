package com.towerbreak.towerbreakgame.presentation.webview

import android.content.Intent
import android.graphics.Color as AndroidColor
import android.net.Uri
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material.icons.rounded.WifiOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.towerbreak.towerbreakgame.foundation.theme.BrandPalette
import com.towerbreak.towerbreakgame.domain.model.ExternalPage
import com.towerbreak.towerbreakgame.presentation.common.RotationLock
import com.towerbreak.towerbreakgame.presentation.common.ScreenAxis
import com.towerbreak.towerbreakgame.presentation.common.theme.GameType

/**
 * In-app browser for the store-required pages (the Flutter `WebPageStage`), with
 * a fall back to the system browser whenever the embedded WebView cannot load.
 *
 * Fingerprint note: `webview_flutter`'s `WebViewController`/`WebViewWidget` is
 * replaced by the platform `android.webkit.WebView` embedded through
 * [AndroidView]; `url_launcher` becomes a plain `ACTION_VIEW` intent.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WebPageScreen(
    page: ExternalPage,
    onBack: () -> Unit,
    viewModel: WebPageViewModel = hiltViewModel(),
) {
    RotationLock(ScreenAxis.UPRIGHT)
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val openExternally = {
        runCatching {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(page.url)))
        }
        Unit
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(page.title, style = GameType.label(size = 18)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back", tint = BrandPalette.Parchment)
                    }
                },
                actions = {
                    IconButton(onClick = openExternally) {
                        Icon(Icons.Rounded.OpenInNew, contentDescription = "Open in browser", tint = BrandPalette.Parchment)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BrandPalette.Ribbon),
            )
        },
    ) { insets ->
        Box(Modifier.fillMaxSize().padding(insets)) {
            if (!state.failed) {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { ctx ->
                        WebView(ctx).apply {
                            settings.javaScriptEnabled = true
                            setBackgroundColor(AndroidColor.rgb(191, 224, 238)) // sky low
                            webViewClient = object : WebViewClient() {
                                override fun onPageFinished(view: WebView?, url: String?) {
                                    viewModel.onPageFinished()
                                }

                                override fun onReceivedError(
                                    view: WebView?,
                                    request: WebResourceRequest?,
                                    error: WebResourceError?,
                                ) {
                                    // Only fail on the main-frame request, matching the Flutter delegate.
                                    if (request?.isForMainFrame == true) viewModel.onLoadError()
                                }
                            }
                            loadUrl(page.url)
                        }
                    },
                )
            } else {
                OfflineNotice(onRetryExternally = openExternally)
            }

            if (state.loading && !state.failed) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = BrandPalette.White,
                )
            }
        }
    }
}

@Composable
private fun OfflineNotice(onRetryExternally: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
    ) {
        Icon(Icons.Rounded.WifiOff, contentDescription = null, tint = BrandPalette.White.copy(alpha = 0.54f))
        Spacer(Modifier.height(12.dp))
        Text("Could not load the page.", style = GameType.label(size = 16), textAlign = TextAlign.Center)
        Spacer(Modifier.height(12.dp))
        androidx.compose.material3.TextButton(onClick = onRetryExternally) {
            Text("Open in browser", style = GameType.prose(size = 14, tint = BrandPalette.AzurePale))
        }
    }
}
