package com.kraftplay.openiptv.lsposed

import androidx.annotation.Keep

@Keep
object LSPosedStatus {
    @JvmStatic
    @Keep
    fun isModuleActive(): Boolean = false

    @JvmField
    @Keep
    var isActive: Boolean = false
}
