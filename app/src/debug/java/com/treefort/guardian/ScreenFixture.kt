package com.treefort.guardian

import android.content.Intent
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
    val role: String = ROLE,
    val noPhones: Boolean = false,
    val noContacts: Boolean = false,
    val noCalls: Boolean = false,
    val noAudit: Boolean = false,
    val trip: Boolean = false,
    val invites: Boolean = false,
    val recovery: Boolean = false,
) {
    companion object {
        private const val ROLE = "owner"
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
            "station-no-phones" to ScreenRoute(noPhones = true),
            "station-trip-active" to ScreenRoute(trip = true),
            "station-caller-home" to ScreenRoute(role = "caller"),
            "modal-pair-phone" to ScreenRoute(),
            "modal-add-rule" to ScreenRoute(),
            "modal-remove-phone" to ScreenRoute(),
            "friends-list" to ScreenRoute(tab = 1),
            "friends-empty" to ScreenRoute(tab = 1, noContacts = true),
            "modal-add-friend-invite" to ScreenRoute(tab = 1),
            "modal-add-friend-phrase" to ScreenRoute(tab = 1),
            "modal-add-friend-number" to ScreenRoute(tab = 1),
            "modal-edit-friend" to ScreenRoute(tab = 1),
            "logbook-calls" to ScreenRoute(tab = 2),
            "logbook-calls-empty" to ScreenRoute(tab = 2, noCalls = true),
            "logbook-safety" to ScreenRoute(tab = 2),
            "logbook-safety-empty" to ScreenRoute(tab = 2, noAudit = true),
            "settings-top" to ScreenRoute(tab = SETTINGS),
            "settings-household" to ScreenRoute(tab = SETTINGS, sub = "Members"),
            "settings-household-invite" to ScreenRoute(tab = SETTINGS, sub = "Members", invites = true),
            "settings-household-invite-form" to ScreenRoute(tab = SETTINGS, sub = "Members"),
            "settings-household-accept-invite" to ScreenRoute(tab = SETTINGS),
            "settings-locations" to ScreenRoute(tab = SETTINGS),
            "settings-travel" to ScreenRoute(tab = SETTINGS, sub = "Trips", trip = true),
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
    val quietRules = listOf(QuietRule("r1", "School nights", true, listOf("Sun", "Mon", "Tue", "Wed", "Thu"), "20:00", "07:00"))
    val tab get() = route.tab
    val sub get() = route.sub
    var scrollPx = 0
        private set

    fun at(vararg ids: String) = active && id in ids

    /** Installs the fixture when [intent] names a screen; returns true if it did (the caller then skips AppStore.start). */
    fun apply(intent: Intent?): Boolean {
        val r = ScreenRoute.parse(intent?.getStringExtra("screen")) ?: return false
        active = true; id = intent?.getStringExtra("screen")?.trim(); route = r
        scrollPx = intent?.getIntExtra("scroll", 0) ?: 0
        val me = Member(1, "owner", "Alex Rivera", "alex@example.com", "bear", "forest", "*55001", isMe = true)
        val phones = if (r.noPhones) emptyList() else listOf(
            Device(1, "Maya's TreeFort", "*48213", "Rivera-Home", true, "2026-10-14T14:58:00Z", 60, true, 1, householdId = 1),
            Device(2, "Leo's TreeFort", "*73190", "Rivera-Home", false, "2026-10-13T20:10:00Z", 45, false, null, householdId = 1),
        )
        val summary = HouseholdSummary(
            1, "The Rivera Family", "America/Los_Angeles", r.role, "Alex Rivera", "bear", "forest", true,
            phones.map { PhoneSummary(it.id, it.name, it.peerDialCode, it.isOnline) },
        )
        AppStore.profile = UserProfile(1, "alex@example.com", isGuardianCertified = true, subscriptionActive = true, subscriptionPlan = "monthly")
        AppStore.myDialCode = ""
        AppStore.households = listOf(summary); AppStore.selectedId = 1
        AppStore.devices = phones
        AppStore.addresses = listOf(Address(1, "742 Evergreen Terrace", null, "Portland", "OR", "97201", false, 1))
        AppStore.contacts = if (r.noContacts) emptyList() else listOf(
            Contact(1, "Grandma Rose", "*55003", true, true, "#1", null, null, 1),
            Contact(2, "Uncle Ben", "*61842", true, true, null, null, null, 1),
            Contact(3, "Jordan", "*20977", false, false, null, null, null, 1),
        )
        AppStore.calls = if (r.noCalls) emptyList() else listOf(
            CallLogEntry(1, 1, 1, "inbound", 312, "2026-10-14T13:20:00Z"),
            CallLogEntry(2, 1, 2, "outbound", 95, "2026-10-14T11:05:00Z"),
            CallLogEntry(3, 2, 3, "inbound", 0, "2026-10-13T22:40:00Z"),
        )
        AppStore.audit = if (r.noAudit) emptyList() else listOf(
            AuditEntry(1, "CONTACT_APPROVED", "Approved Grandma Rose on the Safelist", "Dial route: *55003", "Grandma Rose", "2026-10-14T14:30:00Z", "Alex Rivera", "owner"),
            AuditEntry(2, "RULE_CREATED", "Created Quiet Hours \"School nights\"", null, "School nights", "2026-10-14T09:12:00Z", "Sam Rivera", "guardian"),
            AuditEntry(3, "DEVICE_CLAIMED", "Linked Maya's TreeFort", null, "Maya's TreeFort", "2026-10-12T18:00:00Z", "TreeFort", ""),
        )
        AppStore.trips = if (r.trip) listOf(Trip(1, 1, null, "Grandma trip", "2026-10-12T00:00:00Z", "2026-10-20T00:00:00Z", "America/Los_Angeles", status = "active")) else emptyList()
        AppStore.detail = HouseholdDetail(1, "The Rivera Family", "America/Los_Angeles", r.role, "Alex Rivera", listOf(
            me,
            Member(2, "guardian", "Sam Rivera", "sam@example.com", "fox", "amber", "*55002"),
            Member(3, "caller", "Grandma Rose", "rose@example.com", "owl", "coral", "*55003"),
        ))
        AppStore.invites = if (r.invites) listOf(HouseholdInvite(1, 1, "otter-maple-kite", "guardian", "pat@example.com", "Pat", "2026-10-21T08:00:00Z")) else emptyList()
        AppStore.oauthError = if (id == "auth-provider-error") "Couldn't sign you in with Google. Try again, or use your email." else null
        if (r.recovery) { AppStore.recoveryEmail = "alex@example.com"; AppStore.recoveryActive = true }
        AppStore.phase = r.phase
        return true
    }
}
