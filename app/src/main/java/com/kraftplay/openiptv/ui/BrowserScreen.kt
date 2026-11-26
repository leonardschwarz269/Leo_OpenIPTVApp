package com.kraftplay.openiptv.ui

import android.annotation.SuppressLint
import android.net.http.SslError
import android.os.Message
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
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.filled.Keyboard
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
import androidx.compose.runtime.DisposableEffect
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
import com.kraftplay.openiptv.util.FocusedWebInput
import com.kraftplay.openiptv.util.WebKeyboardMode
import com.kraftplay.openiptv.util.attachCarInputBridge
import com.kraftplay.openiptv.util.attachCarTouchListener
import com.kraftplay.openiptv.util.buildDetectFocusedInputScript
import com.kraftplay.openiptv.util.buildInstallAutoFocusListenerScript
import com.kraftplay.openiptv.util.configureAuthWebView
import com.kraftplay.openiptv.util.createAuthPopupWebView
import com.kraftplay.openiptv.util.flushWebViewCookies
import com.kraftplay.openiptv.util.handleWebNavAction
import com.kraftplay.openiptv.util.shouldAutoShowCarKeyboard
import java.net.URLEncoder

@SuppressLint("SetJavaScriptEnabled", "ClickableViewAccessibility")
@Composable
fun BrowserScreen(
    initialUrl: String = "https://google.com",
    isAuto: Boolean = false,
    onFullscreenVideoChanged: (Boolean) -> Unit = {},
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var webView: WebView? by remember { mutableStateOf(null) }
    var popupWebView: WebView? by remember { mutableStateOf(null) }
    var currentUrl by remember { mutableStateOf(initialUrl) }
    var pageTitle by remember { mutableStateOf("Браузер") }
    var canGoBack by remember { mutableStateOf(false) }
    var canGoForward by remember { mutableStateOf(false) }
    var showKeyboardDialog by remember { mutableStateOf(false) }
    var keyboardMode by remember { mutableStateOf(WebKeyboardMode.URL) }
    var focusedInput by remember { mutableStateOf(FocusedWebInput("", "text", "", false)) }

    var isLoading by remember { mutableStateOf(true) }
    var loadError by remember { mutableStateOf<String?>(null) }
    var mainFrameError by remember { mutableStateOf(false) }
    var failedUrl by remember(initialUrl) { mutableStateOf(initialUrl) }
    var mainFrameUrl by remember(initialUrl) { mutableStateOf(initialUrl) }

    var customView: View? by remember { mutableStateOf(null) }
    var customViewCallback: WebChromeClient.CustomViewCallback? by remember { mutableStateOf(null) }

    fun closePopup() {
        flushWebViewCookies()
        popupWebView?.stopLoading()
        popupWebView?.destroy()
        popupWebView = null
        webView?.requestFocus()
    }

    val onFieldFocused: (FocusedWebInput) -> Unit = { info ->
        focusedInput = info
        keyboardMode = WebKeyboardMode.WEB_FIELD
        showKeyboardDialog = true
    }
    val onNoFieldFocused: () -> Unit = {
        focusedInput = FocusedWebInput("", "text", "", true)
        keyboardMode = WebKeyboardMode.WEB_FIELD
        showKeyboardDialog = true
    }

    DisposableEffect(Unit) {
        onDispose {
            flushWebViewCookies()
            onFullscreenVideoChanged(false)
            popupWebView?.destroy()
        }
    }

    BackHandler {
        if (customView != null) {
            customViewCallback?.onCustomViewHidden()
            customView = null
            customViewCallback = null
            onFullscreenVideoChanged(false)
        } else if (popupWebView != null) {
            if (popupWebView?.canGoBack() == true) {
                popupWebView?.goBack()
            } else {
                closePopup()
            }
        } else if (webView?.canGoBack() == true) {
            webView?.goBack()
        } else {
            flushWebViewCookies()
            onFullscreenVideoChanged(false)
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
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .imePadding()
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
                                flushWebViewCookies()
                                onFullscreenVideoChanged(false)
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

                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .padding(start = 8.dp)
                                .horizontalScroll(rememberScrollState()),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.End
                        ) {
                            IconButton(onClick = {
                                val target = popupWebView ?: webView
                                if (target != null) {
                                    target.evaluateJavascript(
                                        buildDetectFocusedInputScript(notifyIfMissing = true),
                                        null
                                    )
                                } else {
                                    onNoFieldFocused()
                                }
                            }) {
                                Icon(
                                    imageVector = Icons.Default.Keyboard,
                                    contentDescription = "Клавиатура для поля",
                                    tint = Color.White
                                )
                            }
                            IconButton(onClick = {
                                keyboardMode = WebKeyboardMode.URL
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
                            configureAuthWebView(this)
                            attachCarInputBridge(this, onFieldFocused, onNoFieldFocused)
                            attachCarTouchListener(this, isAuto)

                            webViewClient = object : WebViewClient() {
                                override fun shouldOverrideUrlLoading(
                                    view: WebView?,
                                    request: WebResourceRequest?
                                ): Boolean = handleWebNavAction(view, request?.url?.toString())

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
                                    flushWebViewCookies()
                                    if (shouldAutoShowCarKeyboard(isAuto)) {
                                        view?.evaluateJavascript(buildInstallAutoFocusListenerScript(), null)
                                    }
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

                                override fun onCreateWindow(
                                    view: WebView?,
                                    isDialog: Boolean,
                                    isUserGesture: Boolean,
                                    resultMsg: Message?
                                ): Boolean {
                                    val transport = resultMsg?.obj as? WebView.WebViewTransport ?: return false
                                    val child = createAuthPopupWebView(
                                        context = ctx,
                                        isAuto = isAuto,
                                        onInputFocused = onFieldFocused,
                                        onNoInputFocused = onNoFieldFocused,
                                        onClosePopup = { closePopup() }
                                    )
                                    popupWebView = child
                                    transport.webView = child
                                    resultMsg.sendToTarget()
                                    return true
                                }

                                override fun onCloseWindow(window: WebView?) {
                                    closePopup()
                                }

                                override fun onShowCustomView(
                                    view: View?,
                                    callback: CustomViewCallback?
                                ) {
                                    customView = view
                                    customViewCallback = callback
                                    onFullscreenVideoChanged(true)
                                }

                                override fun onHideCustomView() {
                                    customViewCallback?.onCustomViewHidden()
                                    customView = null
                                    customViewCallback = null
                                    onFullscreenVideoChanged(false)
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

        popupWebView?.let { popup ->
            AuthPopupWebViewOverlay(
                popupWebView = popup,
                onClose = { closePopup() }
            )
        }

        if (showKeyboardDialog) {
            WebKeyboardModalHost(
                keyboardMode = keyboardMode,
                focusedInput = focusedInput,
                searchInitialText = currentUrl,
                searchPlaceholder = "Адрес сайта или поиск...",
                searchConfirmLabel = "Перейти",
                searchInitialLayout = KeyboardLayoutMode.EN,
                activeWebViewProvider = { popupWebView ?: webView },
                onDismiss = { showKeyboardDialog = false },
                onSubmitSearchOrUrl = { query ->
                    val targetSearchUrl = if (query.startsWith("http://") || query.startsWith("https://")) {
                        query
                    } else if (query.contains(".") && !query.contains(" ")) {
                        "https://$query"
                    } else {
                        val encoded = runCatching { URLEncoder.encode(query, "UTF-8") }.getOrDefault(query)
                        "https://www.google.com/search?q=$encoded"
                    }
                    webView?.loadUrl(targetSearchUrl)
                }
            )
        }
    }
}
