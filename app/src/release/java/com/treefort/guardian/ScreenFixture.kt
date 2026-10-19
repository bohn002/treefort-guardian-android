package com.treefort.guardian

import android.content.Intent
import com.treefort.guardian.api.QuietRule

/** Release builds have no screenshot fixtures; every hook is a constant no-op. */
object ScreenFixture {
    const val active = false
    val quietRules: List<QuietRule> = emptyList()
    val tab = 0
    val sub: String? = null
    val scrollPx = 0
    fun apply(intent: Intent?) = false
    fun at(vararg ids: String) = false
}
