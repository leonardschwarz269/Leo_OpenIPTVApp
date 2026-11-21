package com.kraftplay.openiptv.util

/** Human-readable WebView main-frame load error. SSL is never bypassed; caller must cancel. */
fun browserLoadErrorMessage(isSsl: Boolean, description: String?): String {
    if (isSsl) {
        return "Не удалось установить защищённое соединение: сертификат сайта недействителен."
    }
    val detail = description?.trim().orEmpty()
    return if (detail.isEmpty()) "Не удалось загрузить страницу."
    else "Не удалось загрузить страницу: $detail"
}

/** SSL errors must be cancelled for every resource, but only main-frame failure replaces the page. */
fun shouldShowMainFrameSslError(mainFrameUrl: String?, failingUrl: String?): Boolean =
    !failingUrl.isNullOrBlank() && failingUrl == mainFrameUrl