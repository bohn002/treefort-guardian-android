package com.treefort.guardian

import android.content.Context
import android.content.Intent
import com.treefort.guardian.api.TreeFortApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.time.Instant
import java.time.ZoneId
import com.treefort.guardian.api.Address
import com.treefort.guardian.api.AuditEntry
import com.treefort.guardian.api.CallLogEntry
import com.treefort.guardian.api.Contact
import com.treefort.guardian.api.Device
import com.treefort.guardian.api.HouseholdDetail
import com.treefort.guardian.api.HouseholdInvite
import com.treefort.guardian.api.HouseholdSummary
import com.treefort.guardian.api.Member
import com.treefort.guardian.api.PhoneSummary
import com.treefort.guardian.api.QuietRule
import com.treefort.guardian.api.Trip
import com.treefort.guardian.api.UserProfile

/** Where a screen id lands: session phase, tab, sub-screen and which fixture data it needs. */
data class ScreenRoute(
    val phase: Phase = Phase.Ready,
    val tab: Int = 0,
    val sub: String? = null,
    val recovery: Boolean = false,
) {
    companion object {
        private const val SETTINGS = 3
        private val SIGNED_OUT = ScreenRoute(phase = Phase.SignedOut)

        /** Every Android screen id that mirrors a docs/SCREENS.md id. Ids with no Android screen are not listed. */
        val ROUTES: Map<String, ScreenRoute> = mapOf(
            "auth-verifying" to ScreenRoute(phase = Phase.Loading),
            "auth-sign-in" to SIGNED_OUT,
            "auth-sign-in-error" to SIGNED_OUT,
            "auth-sign-in-provider-not-set-up" to SIGNED_OUT,
            "auth-create-account" to SIGNED_OUT,
            "auth-create-account-error" to SIGNED_OUT,
            "auth-check-email" to SIGNED_OUT,
            "auth-email-code" to SIGNED_OUT,
            "auth-email-code-verify" to SIGNED_OUT,
            "auth-forgot-password" to SIGNED_OUT,
            "auth-forgot-password-sent" to SIGNED_OUT,
            "auth-reset-password" to ScreenRoute(phase = Phase.SignedOut, recovery = true),
            "auth-reset-password-error" to ScreenRoute(phase = Phase.SignedOut, recovery = true),
            "auth-provider-error" to SIGNED_OUT,
            "auth-certify-18" to ScreenRoute(phase = Phase.NeedsCertify),
            "auth-guardian-lock" to ScreenRoute(tab = SETTINGS),
            "station-home" to ScreenRoute(),
            "station-quiet-hours-expanded" to ScreenRoute(),
            "station-phones" to ScreenRoute(),
            "station-device-menu" to ScreenRoute(),
            "station-no-phones" to ScreenRoute(),
            "station-trip-active" to ScreenRoute(),
            "station-caller-home" to ScreenRoute(),
            "modal-pair-phone" to ScreenRoute(),
            "modal-add-rule" to ScreenRoute(),
            "modal-remove-phone" to ScreenRoute(),
            "friends-list" to ScreenRoute(tab = 1),
            "friends-empty" to ScreenRoute(tab = 1),
            "modal-add-friend-invite" to ScreenRoute(tab = 1),
            "modal-add-friend-phrase" to ScreenRoute(tab = 1),
            "modal-add-friend-number" to ScreenRoute(tab = 1),
            "modal-edit-friend" to ScreenRoute(tab = 1),
            "logbook-calls" to ScreenRoute(tab = 2),
            "logbook-calls-empty" to ScreenRoute(tab = 2),
            "logbook-safety" to ScreenRoute(tab = 2),
            "logbook-safety-empty" to ScreenRoute(tab = 2),
            "settings-top" to ScreenRoute(tab = SETTINGS),
            "settings-household" to ScreenRoute(tab = SETTINGS, sub = "Members"),
            "settings-household-invite" to ScreenRoute(tab = SETTINGS, sub = "Members"),
            "settings-household-invite-form" to ScreenRoute(tab = SETTINGS, sub = "Members"),
            "settings-household-accept-invite" to ScreenRoute(tab = SETTINGS),
            "settings-locations" to ScreenRoute(tab = SETTINGS),
            "settings-travel" to ScreenRoute(tab = SETTINGS, sub = "Trips"),
            "settings-travel-plan-trip" to ScreenRoute(tab = SETTINGS, sub = "Trips"),
            "settings-guardian-lock" to ScreenRoute(tab = SETTINGS),
            "settings-subscription" to ScreenRoute(tab = SETTINGS),
            "settings-plain-text" to ScreenRoute(tab = SETTINGS),
            "settings-health" to ScreenRoute(tab = SETTINGS),
            "settings-about-delete" to ScreenRoute(tab = SETTINGS),
            "settings-modal-add-address" to ScreenRoute(tab = SETTINGS),
            "settings-modal-release-phone" to ScreenRoute(),
            "settings-modal-guardian-lock-pin" to ScreenRoute(tab = SETTINGS),
            "settings-modal-delete-account" to ScreenRoute(tab = SETTINGS),
            "support-home" to ScreenRoute(tab = SETTINGS, sub = "Help"),
            "support-faq-open" to ScreenRoute(tab = SETTINGS, sub = "Help"),
        )

        /** The intent's `screen` extra to a route, or null when absent or not a known id. */
        fun parse(id: String?): ScreenRoute? = id?.trim()?.let { ROUTES[it] }
    }
}

/**
 * Debug builds only (the release source set has a no-op twin): `adb shell am start -n
 * com.treefort.guardian/.MainActivity --es screen <id> [--ei scroll <px>]` skips Supabase and the
 * backend and shows one screen from built-in fixture data, for docs/screens/android (scripts/screens.sh).
 */
object ScreenFixture {
    var active = false
        private set
    private var id: String? = null
    private var route = ScreenRoute()
    var quietRules: List<QuietRule> = emptyList()
        private set
    val tab get() = route.tab
    val sub get() = route.sub
    var scrollPx = 0
        private set

    fun at(vararg ids: String) = active && id in ids

    /** Installs the fixture when [intent] names a screen; returns true if it did (the caller then skips AppStore.start). */
    fun apply(context: Context, intent: Intent?): Boolean {
        val r = ScreenRoute.parse(intent?.getStringExtra("screen")) ?: return false
        active = true; id = intent?.getStringExtra("screen")?.trim(); route = r
        scrollPx = intent?.getIntExtra("scroll", 0) ?: 0
        val fx = Json.parseToJsonElement(context.assets.open("guardian.json").bufferedReader().use { it.readText() }).jsonObject
        val scenario = fx["scenarios"]?.jsonObject?.get(id)?.jsonObject
        val j = TreeFortApi.json
        fun <T> list(key: String, ser: KSerializer<T>): List<T> =
            j.decodeFromJsonElement(ListSerializer(ser), scenario?.get(key) ?: fx[key] ?: JsonArray(emptyList()))
        DisplayClock.now = { Instant.parse(fx["now"]!!.jsonPrimitive.content) }
        DisplayClock.zone = { ZoneId.of(fx["display_time_zone"]!!.jsonPrimitive.content) }

        val role = scenario?.get("household.my_role")?.jsonPrimitive?.content ?: fx["household"]!!.jsonObject["my_role"]!!.jsonPrimitive.content
        val members = list("members", Member.serializer())
        val phones = list("phones", Device.serializer())
        val hh = fx["household"]!!.jsonObject
        val summary = j.decodeFromJsonElement(HouseholdSummary.serializer(), JsonObject(hh + ("role" to JsonPrimitive(role))))
            .copy(phones = phones.map { PhoneSummary(it.id, it.name, it.peerDialCode, it.isOnline) })
        AppStore.profile = j.decodeFromJsonElement(UserProfile.serializer(), fx["profile"]!!)
        AppStore.myDialCode = fx["profile"]!!.jsonObject["app_dial_code"]?.jsonPrimitive?.content.orEmpty()
        AppStore.households = listOf(summary); AppStore.selectedId = summary.id
        AppStore.devices = phones
        AppStore.addresses = list("emergency_addresses", Address.serializer())
        AppStore.contacts = list("friends", Contact.serializer())
        AppStore.calls = list("calls", CallLogEntry.serializer())
        AppStore.audit = list("safety_log", AuditEntry.serializer())
        AppStore.trips = if (scenario?.get("trip")?.jsonPrimitive?.booleanOrNull == true) listOf(j.decodeFromJsonElement(Trip.serializer(), fx["trip"]!!)) else emptyList()
        AppStore.detail = HouseholdDetail(summary.id, summary.name, summary.timezone, role, summary.displayName, members)
        AppStore.invites = if (scenario?.get("invite")?.jsonPrimitive?.booleanOrNull == true) listOf(j.decodeFromJsonElement(HouseholdInvite.serializer(), fx["invite"]!!)) else emptyList()
        quietRules = fx["quiet_hours"]!!.jsonObject["rules"]!!.jsonArray.map {
            val o = it.jsonObject
            QuietRule(o["id"]?.jsonPrimitive?.content, o["name"]?.jsonPrimitive?.content, o["enabled"]?.jsonPrimitive?.booleanOrNull ?: true,
                o["days"]!!.jsonArray.map { d -> d.jsonPrimitive.content }, o["start_time"]!!.jsonPrimitive.content, o["end_time"]!!.jsonPrimitive.content)
        }
        AppStore.oauthError = if (id == "auth-provider-error") "Couldn't sign you in with Google. Try again, or use your email." else null
        if (r.recovery) { AppStore.recoveryEmail = "alex@example.com"; AppStore.recoveryActive = true }
        AppStore.phase = r.phase
        return true
    }
}
