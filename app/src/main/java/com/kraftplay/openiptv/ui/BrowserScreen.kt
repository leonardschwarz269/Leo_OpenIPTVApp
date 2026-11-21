package com.kraftplay.openiptv.ui

import android.annotation.SuppressLint
import android.net.http.SslError
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.webkit.SslErrorHandler
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import java.net.URLEncoder

@SuppressLint("SetJavaScriptEnabled", "ClickableViewAccessibility")
@Composable
fun BrowserScreen(
    initialUrl: String = "https://google.com",
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var webView: WebView? by remember { mutableStateOf(null) }
    var currentUrl by remember { mutableStateOf(initialUrl) }
    var pageTitle by remember { mutableStateOf("Браузер") }
    var canGoBack by remember { mutableStateOf(false) }
    var canGoForward by remember { mutableStateOf(false) }
    var showKeyboardDialog by remember { mutableStateOf(false) }

    var isLoading by remember { mutableStateOf(true) }
    var loadError by remember { mutableStateOf<String?>(null) }
    var mainFrameError by remember { mutableStateOf(false) }
    var failedUrl by remember(initialUrl) { mutableStateOf(initialUrl) }
    var mainFrameUrl by remember(initialUrl) { mutableStateOf(initialUrl) }

    var customView: View? by remember { mutableStateOf(null) }
    var customViewCallback: WebChromeClient.CustomViewCallback? by remember { mutableStateOf(null) }

    BackHandler {
        if (customView != null) {
            customViewCallback?.onCustomViewHidden()
            customView = null
            customViewCallback = null
        } else if (webView?.canGoBack() == true) {
            webView?.goBack()
        } else {
            com.kraftplay.openiptv.service.SharedPlayerManager.stopPlayback()
            onBack()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        if (customView != null) {
            // Полноэкранный видеоплеер
            AndroidView(
                factory = { customView!! },
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Панель управления браузера
                Surface(
                    color = Color(0xFF181818),
                    contentColor = Color.White,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            IconButton(onClick = {
                                com.kraftplay.openiptv.service.SharedPlayerManager.stopPlayback()
                                onBack()
                            }) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                    contentDescription = "Назад к каналам",
                                    tint = Color.White
                                )
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Default.Public,
                                contentDescription = "Browser",
                                tint = Color(0xFF4285F4),
                                modifier = Modifier.size(26.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = pageTitle.ifEmpty { currentUrl },
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = {
                                showKeyboardDialog = true
                            }) {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = "Поиск / Ввод адреса",
                                    tint = Color.White
                                )
                            }
                            IconButton(onClick = {
                                com.kraftplay.openiptv.util.VoiceInputHelper.startSpeechRecognition(
                                    context = context,
                                    onResult = { query ->
                                        val targetUrl = if (query.startsWith("http://") || query.startsWith("https://")) {
                                            query
                                        } else if (query.contains(".")) {
                                            "https://$query"
                                        } else {
                                            val encoded = runCatching { URLEncoder.encode(query, "UTF-8") }.getOrDefault(query)
                                            "https://www.google.com/search?q=$encoded"
                                        }
                                        webView?.loadUrl(targetUrl)
                                    }
                                )
                            }) {
                                Icon(
                                    imageVector = Icons.Default.Mic,
                                    contentDescription = "Голосовой поиск",
                                    tint = Color.White
                                )
                            }
                            IconButton(
                                onClick = { webView?.goBack() },
                                enabled = canGoBack
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.ChevronLeft,
                                    contentDescription = "Назад в браузере",
                                    tint = if (canGoBack) Color.White else Color.Gray
                                )
                            }
                            IconButton(
                                onClick = { webView?.goForward() },
                                enabled = canGoForward
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.ChevronRight,
                                    contentDescription = "Вперед в браузере",
                                    tint = if (canGoForward) Color.White else Color.Gray
                                )
                            }
                            IconButton(onClick = {
                                webView?.loadUrl(initialUrl)
                            }) {
                                Icon(
                                    imageVector = Icons.Rounded.Home,
                                    contentDescription = "Главная страница",
                                    tint = Color.White
                                )
                            }
                            IconButton(onClick = { webView?.reload() }) {
                                Icon(
                                    imageVector = Icons.Rounded.Refresh,
                                    contentDescription = "Обновить страницу",
                                    tint = Color.White
                                )
                            }
                        }
                    }
                }

                // Встроенный WebView плеера браузера
                AndroidView(
                    factory = { ctx ->
                        WebView(ctx).apply {
                            setBackgroundColor(android.graphics.Color.BLACK)
                            layoutParams = ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )
                            isFocusable = true
                            isFocusableInTouchMode = true
                            settings.javaScriptEnabled = true
                            settings.domStorageEnabled = true
                            settings.javaScriptCanOpenWindowsAutomatically = true
                            settings.mediaPlaybackRequiresUserGesture = false
                            settings.useWideViewPort = true
                            settings.loadWithOverviewMode = true
                            settings.userAgentString =
                                "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"

                            addJavascriptInterface(
                                object {
                                    @android.webkit.JavascriptInterface
                                    fun onInputFocused(initialValue: String) {
                                        post {
                                            showKeyboardDialog = true
                                        }
                                    }
                                },
                                "CarBridge"
                            )

                            setOnTouchListener { v, event ->
                                if (event.action == MotionEvent.ACTION_UP) {
                                    v.requestFocus()
                                    postDelayed({
                                        loadUrl("javascript:(function() {" +
                                                "var e = document.activeElement;" +
                                                "if (e && (e.tagName === 'INPUT' || e.tagName === 'TEXTAREA' || e.isContentEditable || e.getAttribute('contenteditable') === 'true')) {" +
                                                "  var val = e.value || e.innerText || '';" +
                                                "  window.CarBridge.onInputFocused(val);" +
                                                "}" +
                                                "})();")
                                    }, 200)
                                }
                                false
                            }

                            webViewClient = object : WebViewClient() {
                                override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                                    super.onPageStarted(view, url, favicon)
                                    mainFrameUrl = url ?: initialUrl
                                    isLoading = true
                                    loadError = null
                                    mainFrameError = false
                                }

                                override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
                                    super.onReceivedError(view, request, error)
                                    if (request?.isForMainFrame == true) {
                                        mainFrameError = true
                                        failedUrl = request.url?.toString() ?: view?.url ?: initialUrl
                                        loadError = com.kraftplay.openiptv.util.browserLoadErrorMessage(
                                            isSsl = false,
                                            description = error?.description?.toString()
                                        )
                                        isLoading = false
                                    }
                                }

                                override fun onReceivedSslError(view: WebView?, handler: SslErrorHandler?, error: SslError?) {
                                    // Always cancel the failing resource; only main-frame error replaces the page.
                                    handler?.cancel()
                                    if (com.kraftplay.openiptv.util.shouldShowMainFrameSslError(mainFrameUrl, error?.url)) {
                                        mainFrameError = true
                                        failedUrl = view?.url ?: initialUrl
                                        loadError = com.kraftplay.openiptv.util.browserLoadErrorMessage(isSsl = true, description = null)
                                        isLoading = false
                                    }
                                }

                                override fun onPageFinished(view: WebView?, url: String?) {
                                    super.onPageFinished(view, url)
                                    isLoading = false
                                    if (!mainFrameError) {
                                        loadError = null
                                        canGoBack = view?.canGoBack() == true
                                        canGoForward = view?.canGoForward() == true
                                        currentUrl = url ?: ""
                                        pageTitle = view?.title ?: "Браузер"
                                    }
                                }
                            }
                            webChromeClient = object : WebChromeClient() {
                                override fun onReceivedTitle(view: WebView?, title: String?) {
                                    super.onReceivedTitle(view, title)
                                    if (!title.isNullOrEmpty()) {
                                        pageTitle = title
                                    }
                                }

                                override fun onShowCustomView(
                                    view: View?,
                                    callback: CustomViewCallback?
                                ) {
                                    customView = view
                                    customViewCallback = callback
                                }

                                override fun onHideCustomView() {
                                    customViewCallback?.onCustomViewHidden()
                                    customView = null
                                    customViewCallback = null
                                }
                            }
                            loadUrl(initialUrl)
                            webView = this
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        if (loadError != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.94f))
                    .clickable(onClick = {}),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(24.dp)
                ) {
                    Text(
                        text = loadError.orEmpty(),
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = {
                        val target = failedUrl
                        loadError = null
                        mainFrameError = false
                        isLoading = true
                        webView?.loadUrl(target)
                    }) {
                        Text("Повторить")
                    }
                }
            }
        } else if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color.White)
            }
        }

        if (showKeyboardDialog) {
            CarOnScreenKeyboardDialog(
                initialText = currentUrl,
                onDismiss = { showKeyboardDialog = false },
                onConfirm = { query ->
                    val safeText = query.replace("'", "\\'").replace("\n", " ")
                    val targetSearchUrl = if (query.startsWith("http://") || query.startsWith("https://")) {
                        query
                    } else if (query.contains(".") && !query.contains(" ")) {
                        "https://$query"
                    } else {
                        val encoded = runCatching { URLEncoder.encode(query, "UTF-8") }.getOrDefault(query)
                        "https://www.google.com/search?q=$encoded"
                    }
                    webView?.loadUrl("javascript:(function() {" +
                            "var e = document.activeElement;" +
                            "if (e && (e.tagName === 'INPUT' || e.tagName === 'TEXTAREA' || e.isContentEditable || e.getAttribute('contenteditable') === 'true')) {" +
                            "  if (e.isContentEditable) e.innerText = '$safeText';" +
                            "  else e.value = '$safeText';" +
                            "  e.dispatchEvent(new InputEvent('input', { bubbles: true, data: '$safeText', inputType: 'insertText' }));" +
                            "  e.dispatchEvent(new Event('change', { bubbles: true }));" +
                            "} else {" +
                            "  window.location.href = '$targetSearchUrl';" +
                            "}" +
                            "})();")
                }
            )
        }
    }
}
