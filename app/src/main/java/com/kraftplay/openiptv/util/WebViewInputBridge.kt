package com.kraftplay.openiptv.util

import android.annotation.SuppressLint
import android.content.Context
import android.os.SystemClock
import android.view.KeyCharacterMap
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.webkit.CookieManager
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import java.net.URLDecoder

enum class WebKeyboardMode { WEB_FIELD, SEARCH, URL }

data class FocusedWebInput(
    val value: String,
    val inputType: String,
    val placeholder: String,
    val isCrossOriginIframe: Boolean
)

sealed class WebNavAction {
    object AllowInWebView : WebNavAction()
    data class LoadFallbackUrl(val url: String) : WebNavAction()
    object BlockExternalScheme : WebNavAction()
}

private const val FALLBACK_MOBILE_CHROME_UA =
    "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Mobile Safari/537.36"

private val WV_TOKEN_REGEX = Regex(";\\s*wv(?=\\s*[;)])", RegexOption.IGNORE_CASE)
private val VERSION_TOKEN_REGEX = Regex("\\bVersion/\\d+(?:\\.\\d+)*\\s*", RegexOption.IGNORE_CASE)
private val MULTI_SPACE_REGEX = Regex("\\s{2,}")

fun sanitizeWebViewUserAgent(defaultUa: String): String {
    if (defaultUa.isBlank()) return FALLBACK_MOBILE_CHROME_UA
    return defaultUa
        .replace(WV_TOKEN_REGEX, "")
        .replace(VERSION_TOKEN_REGEX, "")
        .replace("; )", ")")
        .replace(MULTI_SPACE_REGEX, " ")
        .trim()
        .ifEmpty { FALLBACK_MOBILE_CHROME_UA }
}

fun shouldAutoShowCarKeyboard(isAuto: Boolean): Boolean = isAuto

fun classifyWebNavigation(url: String?): WebNavAction {
    if (url.isNullOrBlank()) return WebNavAction.BlockExternalScheme
    val trimmed = url.trim()
    val lower = trimmed.lowercase()
    if (lower.startsWith("http://") || lower.startsWith("https://")) {
        return WebNavAction.AllowInWebView
    }
    if (lower.startsWith("intent://")) {
        val marker = "S.browser_fallback_url="
        val idx = trimmed.indexOf(marker)
        if (idx >= 0) {
            val start = idx + marker.length
            val end = trimmed.indexOf(';', start).let { if (it < 0) trimmed.length else it }
            val raw = trimmed.substring(start, end)
            val decoded = runCatching { URLDecoder.decode(raw, "UTF-8") }.getOrDefault(raw)
            val decodedLower = decoded.lowercase()
            if (decodedLower.startsWith("http://") || decodedLower.startsWith("https://")) {
                return WebNavAction.LoadFallbackUrl(decoded)
            }
        }
        return WebNavAction.BlockExternalScheme
    }
    return WebNavAction.BlockExternalScheme
}

internal fun escapeJsString(value: String): String {
    val sb = StringBuilder(value.length + 16)
    for (ch in value) {
        when (ch) {
            '\\' -> sb.append("\\\\")
            '"' -> sb.append("\\\"")
            '\'' -> sb.append("\\'")
            '\n' -> sb.append("\\n")
            '\r' -> sb.append("\\r")
            '\t' -> sb.append("\\t")
            '\u2028' -> sb.append("\\u2028")
            '\u2029' -> sb.append("\\u2029")
            else -> sb.append(ch)
        }
    }
    return sb.toString()
}

fun buildDetectFocusedInputScript(notifyIfMissing: Boolean = false): String {
    val missingCall = if (notifyIfMissing) {
        "if (window.CarBridge && window.CarBridge.onNoInputFocused) { window.CarBridge.onNoInputFocused(); }"
    } else {
        ""
    }
    return """
        (function() {
          function deepActive(doc) {
            if (!doc) return null;
            var el = doc.activeElement;
            var guard = 0;
            while (el && guard++ < 10) {
              if (el.shadowRoot && el.shadowRoot.activeElement) {
                el = el.shadowRoot.activeElement;
                continue;
              }
              if (el.tagName === 'IFRAME') {
                try {
                  var innerDoc = el.contentDocument || (el.contentWindow && el.contentWindow.document);
                  if (innerDoc && innerDoc.activeElement) {
                    el = innerDoc.activeElement;
                    continue;
                  }
                } catch (crossOriginErr) {
                  return { crossOriginIframe: true, element: el };
                }
              }
              break;
            }
            return { crossOriginIframe: false, element: el };
          }
          var res = deepActive(document);
          if (res && res.crossOriginIframe) {
            window.__openIptvActiveInput = null;
            window.__openIptvLastNotify = Date.now();
            if (window.CarBridge) {
              window.CarBridge.onInputFocused('', 'text', '', true);
            }
            return;
          }
          var e = res ? res.element : null;
          if (!e) { $missingCall return; }
          var tag = (e.tagName || '').toUpperCase();
          var rawType = ((e.getAttribute && e.getAttribute('type')) || e.type || 'text').toLowerCase();
          var nonText = ['button','submit','reset','checkbox','radio','hidden','file','image','range','color'];
          var isContentEditable = !!(e.isContentEditable || (e.getAttribute && e.getAttribute('contenteditable') === 'true'));
          var isTextInput = (tag === 'INPUT' && nonText.indexOf(rawType) === -1) || tag === 'TEXTAREA' || isContentEditable;
          if (!isTextInput) { $missingCall return; }
          window.__openIptvActiveInput = e;
          var inputMode = ((e.getAttribute && e.getAttribute('inputmode')) || '').toLowerCase();
          var normType = rawType;
          if (normType !== 'password') {
            if (rawType === 'tel' || inputMode === 'tel') normType = 'tel';
            else if (rawType === 'number' || inputMode === 'numeric' || inputMode === 'decimal') normType = 'number';
            else if (rawType === 'email' || inputMode === 'email') normType = 'email';
            else if (rawType === 'url' || inputMode === 'url') normType = 'url';
            else normType = 'text';
          }
          var val = isContentEditable ? (e.innerText || '') : (e.value || '');
          var ph = (e.getAttribute && (e.getAttribute('placeholder') || e.getAttribute('aria-label'))) || '';
          window.__openIptvLastNotify = Date.now();
          if (window.CarBridge) {
            window.CarBridge.onInputFocused(val, normType, ph, false);
          }
        })();
    """.trimIndent()
}

fun buildSetInputValueScript(text: String, triggerChange: Boolean = false): String {
    val escaped = escapeJsString(text)
    val changeDispatch = if (triggerChange) {
        "e.dispatchEvent(new Event('change', { bubbles: true }));"
    } else {
        ""
    }
    return """
        (function() {
          var value = "$escaped";
          function deepActive(doc) {
            if (!doc) return null;
            var el = doc.activeElement;
            var guard = 0;
            while (el && guard++ < 10) {
              if (el.shadowRoot && el.shadowRoot.activeElement) {
                el = el.shadowRoot.activeElement;
                continue;
              }
              if (el.tagName === 'IFRAME') {
                try {
                  var innerDoc = el.contentDocument || (el.contentWindow && el.contentWindow.document);
                  if (innerDoc && innerDoc.activeElement) {
                    el = innerDoc.activeElement;
                    continue;
                  }
                } catch (err) { break; }
              }
              break;
            }
            return el;
          }
          var e = window.__openIptvActiveInput;
          if (!e || e.isConnected === false) {
            e = deepActive(document);
          }
          if (!e) return;
          window.__openIptvSettingValue = true;
          try {
            if (e.focus) { try { e.focus(); } catch (err) {} }
            var isContentEditable = !!(e.isContentEditable || (e.getAttribute && e.getAttribute('contenteditable') === 'true'));
            if (isContentEditable) {
              e.innerText = value;
            } else {
              var win = (e.ownerDocument && e.ownerDocument.defaultView) || window;
              var proto = e.tagName === 'TEXTAREA'
                ? (win.HTMLTextAreaElement && win.HTMLTextAreaElement.prototype)
                : (win.HTMLInputElement && win.HTMLInputElement.prototype);
              var desc = proto ? Object.getOwnPropertyDescriptor(proto, 'value') : null;
              if (desc && desc.set) {
                desc.set.call(e, value);
              } else {
                e.value = value;
              }
            }
            try {
              e.dispatchEvent(new InputEvent('input', { bubbles: true, cancelable: true, inputType: 'insertText', data: value }));
            } catch (err) {
              e.dispatchEvent(new Event('input', { bubbles: true }));
            }
            $changeDispatch
          } finally {
            window.__openIptvSettingValue = false;
          }
        })();
    """.trimIndent()
}

fun buildInstallAutoFocusListenerScript(): String {
    val detectScript = buildDetectFocusedInputScript(notifyIfMissing = false)
    return """
        (function() {
          if (window.__openIptvFocusListenerInstalled) return;
          window.__openIptvFocusListenerInstalled = true;
          document.addEventListener('focusin', function() {
            if (window.__openIptvSettingValue) return;
            setTimeout(function() {
              if (window.__openIptvSettingValue) return;
              var now = Date.now();
              if (window.__openIptvLastNotify && (now - window.__openIptvLastNotify) < 400) return;
              $detectScript
            }, 120);
          }, true);
        })();
    """.trimIndent()
}

fun buildResetInputNotifyScript(): String = "window.__openIptvLastNotify = 0;"

@SuppressLint("SetJavaScriptEnabled")
fun configureAuthWebView(webView: WebView) {
    CookieManager.getInstance().apply {
        setAcceptCookie(true)
        setAcceptThirdPartyCookies(webView, true)
    }
    webView.settings.apply {
        javaScriptEnabled = true
        domStorageEnabled = true
        databaseEnabled = true
        javaScriptCanOpenWindowsAutomatically = true
        setSupportMultipleWindows(true)
        mediaPlaybackRequiresUserGesture = false
        useWideViewPort = true
        loadWithOverviewMode = true
        val defaultUa = runCatching { WebSettings.getDefaultUserAgent(webView.context) }.getOrDefault("")
        userAgentString = sanitizeWebViewUserAgent(defaultUa)
    }
}

fun hideSystemSoftKeyboard(view: View?) {
    if (view == null) return
    runCatching {
        val imm = view.context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
        imm?.hideSoftInputFromWindow(view.windowToken, 0)
    }
}

fun flushWebViewCookies() {
    runCatching { CookieManager.getInstance().flush() }
}

fun dispatchDeltaToWebView(webView: WebView?, oldText: String, newText: String) {
    if (webView == null || oldText == newText) return
    val minLen = minOf(oldText.length, newText.length)
    var prefixLen = 0
    while (prefixLen < minLen && oldText[prefixLen] == newText[prefixLen]) {
        prefixLen++
    }
    val deleteCount = oldText.length - prefixLen
    val added = newText.substring(prefixLen)

    webView.requestFocus()
    repeat(deleteCount) {
        val now = SystemClock.uptimeMillis()
        webView.dispatchKeyEvent(KeyEvent(now, now, KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DEL, 0))
        webView.dispatchKeyEvent(KeyEvent(now, now, KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DEL, 0))
    }
    if (added.isNotEmpty()) {
        val keyCharMap = KeyCharacterMap.load(KeyCharacterMap.VIRTUAL_KEYBOARD)
        val events = keyCharMap.getEvents(added.toCharArray())
        if (events != null) {
            for (event in events) {
                webView.dispatchKeyEvent(event)
            }
        } else {
            val now = SystemClock.uptimeMillis()
            webView.dispatchKeyEvent(KeyEvent(now, added, KeyCharacterMap.VIRTUAL_KEYBOARD, 0))
        }
    }
}

fun handleWebNavAction(view: WebView?, url: String?): Boolean {
    return when (val action = classifyWebNavigation(url)) {
        WebNavAction.AllowInWebView -> false
        is WebNavAction.LoadFallbackUrl -> {
            view?.loadUrl(action.url)
            true
        }
        WebNavAction.BlockExternalScheme -> true
    }
}

fun attachCarInputBridge(
    webView: WebView,
    onInputFocused: (FocusedWebInput) -> Unit,
    onNoInputFocused: () -> Unit = {}
) {
    webView.addJavascriptInterface(
        object {
            @JavascriptInterface
            fun onInputFocused(
                initialValue: String?,
                inputType: String?,
                placeholder: String?,
                isCrossOriginIframe: Boolean
            ) {
                val info = FocusedWebInput(
                    value = initialValue.orEmpty(),
                    inputType = inputType.orEmpty().ifBlank { "text" },
                    placeholder = placeholder.orEmpty(),
                    isCrossOriginIframe = isCrossOriginIframe
                )
                webView.post { onInputFocused(info) }
            }

            @JavascriptInterface
            fun onNoInputFocused() {
                webView.post { onNoInputFocused() }
            }
        },
        "CarBridge"
    )
}

@SuppressLint("ClickableViewAccessibility")
fun attachCarTouchListener(webView: WebView, isAuto: Boolean) {
    webView.setOnTouchListener { v, event ->
        if (event.action == MotionEvent.ACTION_UP) {
            v.requestFocus()
            if (shouldAutoShowCarKeyboard(isAuto)) {
                hideSystemSoftKeyboard(v)
                v.postDelayed({
                    hideSystemSoftKeyboard(v)
                    webView.evaluateJavascript(buildDetectFocusedInputScript(notifyIfMissing = false), null)
                }, 200)
            }
        }
        false
    }
}

fun createAuthPopupWebView(
    context: Context,
    isAuto: Boolean,
    onInputFocused: (FocusedWebInput) -> Unit,
    onNoInputFocused: () -> Unit,
    onClosePopup: () -> Unit
): WebView {
    return WebView(context).apply {
        layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        isFocusable = true
        isFocusableInTouchMode = true
        configureAuthWebView(this)
        attachCarInputBridge(this, onInputFocused, onNoInputFocused)
        attachCarTouchListener(this, isAuto)
        webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                return handleWebNavAction(view, request?.url?.toString())
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                flushWebViewCookies()
                if (shouldAutoShowCarKeyboard(isAuto)) {
                    view?.evaluateJavascript(buildInstallAutoFocusListenerScript(), null)
                }
            }
        }
        webChromeClient = object : WebChromeClient() {
            override fun onCloseWindow(window: WebView?) {
                flushWebViewCookies()
                onClosePopup()
            }
        }
    }
}
