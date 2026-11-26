package com.kraftplay.openiptv.ui

import android.annotation.SuppressLint
import android.os.Message
import android.view.View
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.Refresh
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
fun YouTubeScreen(
    isAuto: Boolean = false,
    onFullscreenVideoChanged: (Boolean) -> Unit = {},
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var webView: WebView? by remember { mutableStateOf(null) }
    var popupWebView: WebView? by remember { mutableStateOf(null) }
    var canGoBack by remember { mutableStateOf(false) }
    var canGoForward by remember { mutableStateOf(false) }
    var showKeyboardDialog by remember { mutableStateOf(false) }
    var keyboardMode by remember { mutableStateOf(WebKeyboardMode.SEARCH) }
    var focusedInput by remember { mutableStateOf(FocusedWebInput("", "text", "", false)) }

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
            // Полноэкранный видеоплеер YouTube
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
                // Панель управления YouTube
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
                        Row(verticalAlignment = Alignment.CenterVertically) {
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
                                imageVector = Icons.Rounded.PlayCircle,
                                contentDescription = "YouTube",
                                tint = Color(0xFFFF0000),
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "YouTube",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
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
                                keyboardMode = WebKeyboardMode.SEARCH
                                showKeyboardDialog = true
                            }) {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = "Клавиатура поиска YouTube",
                                    tint = Color.White
                                )
                            }
                            IconButton(onClick = {
                                com.kraftplay.openiptv.util.VoiceInputHelper.startSpeechRecognition(
                                    context = context,
                                    onResult = { query ->
                                        val encoded = runCatching { URLEncoder.encode(query, "UTF-8") }.getOrDefault(query)
                                        webView?.loadUrl("https://m.youtube.com/results?search_query=$encoded")
                                    }
                                )
                            }) {
                                Icon(
                                    imageVector = Icons.Default.Mic,
                                    contentDescription = "Голосовой поиск YouTube",
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
                                webView?.loadUrl("https://m.youtube.com")
                            }) {
                                Icon(
                                    imageVector = Icons.Rounded.Home,
                                    contentDescription = "Главная YouTube",
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

                // Встроенный WebView плеера YouTube
                AndroidView(
                    factory = { ctx ->
                        WebView(ctx).apply {
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

                                override fun onPageFinished(view: WebView?, url: String?) {
                                    super.onPageFinished(view, url)
                                    flushWebViewCookies()
                                    if (shouldAutoShowCarKeyboard(isAuto)) {
                                        view?.evaluateJavascript(buildInstallAutoFocusListenerScript(), null)
                                    }
                                    canGoBack = view?.canGoBack() == true
                                    canGoForward = view?.canGoForward() == true
                                }
                            }
                            webChromeClient = object : WebChromeClient() {
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
                            loadUrl("https://m.youtube.com")
                            webView = this
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
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
                searchInitialText = "",
                searchPlaceholder = "Поиск на YouTube...",
                searchConfirmLabel = "Найти",
                searchInitialLayout = KeyboardLayoutMode.RU,
                activeWebViewProvider = { popupWebView ?: webView },
                onDismiss = { showKeyboardDialog = false },
                onSubmitSearchOrUrl = { query ->
                    val encoded = runCatching { URLEncoder.encode(query, "UTF-8") }.getOrDefault(query)
                    webView?.loadUrl("https://m.youtube.com/results?search_query=$encoded")
                }
            )
        }
    }
}
