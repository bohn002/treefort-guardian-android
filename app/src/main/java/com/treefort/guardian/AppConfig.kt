package com.treefort.guardian

import android.content.Context
import com.treefort.guardian.theme.ThemeConfig
import com.treefort.guardian.theme.ThemeMode

/** Build-time endpoints (see app/build.gradle.kts) plus the debug-only host override. */
object AppConfig {
    val supabaseUrl: String get() = BuildConfig.SUPABASE_URL
    val supabaseAnonKey: String get() = BuildConfig.SUPABASE_ANON_KEY

    /** Release builds have no fallback: a blank value fails loudly instead of using a baked-in default. */
    val missingConfig: Boolean get() = supabaseUrl.isBlank() || supabaseAnonKey.isBlank()

    /** Developer tools (host override). Compiled out of release builds. */
    val devTools: Boolean get() = BuildConfig.DEV_TOOLS

    /** `host[:port]` of the TreeFort backend. Only a debug build honours an override. */
    @Volatile var apiHost: String = BuildConfig.API_HOST

    fun isPlainHttpHost(host: String): Boolean =
        devTools && (host.startsWith("10.") || host.startsWith("192.168.") || host.startsWith("127.") ||
            host.startsWith("localhost") || host.endsWith(".local"))

    val httpBase: String get() = (if (isPlainHttpHost(apiHost)) "http://" else "https://") + apiHost
    val wsBase: String get() = (if (isPlainHttpHost(apiHost)) "ws://" else "wss://") + apiHost

    const val SUPPORT_EMAIL = "support@treefortphone.com"
    const val WEB_APP_URL = "https://app.treefort.kids"
    const val PRIVACY_URL = "https://treefort.kids/privacy"
}

/** Small persisted conveniences (not secrets). */
object AppPrefs {
    private const val FILE = "treefort_prefs"
    private lateinit var ctx: Context

    fun init(context: Context) {
        ctx = context.applicationContext
        val p = ctx.getSharedPreferences(FILE, Context.MODE_PRIVATE)
        ThemeConfig.isBoringTextEnabled = p.getBoolean("boring_text_enabled", false)
        ThemeConfig.mode = runCatching { ThemeMode.valueOf(p.getString("theme_mode", ThemeMode.System.name)!!) }.getOrDefault(ThemeMode.System)
        if (AppConfig.devTools) p.getString("dev_api_host", null)?.takeIf { it.isNotBlank() }?.let { AppConfig.apiHost = it }
    }

    private val prefs get() = ctx.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    var plainText: Boolean
        get() = ThemeConfig.isBoringTextEnabled
        set(v) { ThemeConfig.isBoringTextEnabled = v; prefs.edit().putBoolean("boring_text_enabled", v).apply() }

    var themeMode: ThemeMode
        get() = ThemeConfig.mode
        set(v) { ThemeConfig.mode = v; prefs.edit().putString("theme_mode", v.name).apply() }

    var selectedHouseholdId: Int
        get() = prefs.getInt("selected_household", -1)
        set(v) { prefs.edit().putInt("selected_household", v).apply() }

    /** Debug builds only. */
    fun setDevApiHost(host: String?) {
        if (!AppConfig.devTools) return
        AppConfig.apiHost = host?.takeIf { it.isNotBlank() } ?: BuildConfig.API_HOST
        prefs.edit().putString("dev_api_host", host).apply()
    }
}
