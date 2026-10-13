package com.treefort.guardian.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// Wire models for the TreeFort backend (ADR 0002 and backend/src/api/*.py). Unknown keys are ignored
// so a newer backend never breaks an older app. Device.secret_key and mac_address are never sent.

const val ROLE_OWNER = "owner"
const val ROLE_GUARDIAN = "guardian"
const val ROLE_CALLER = "caller"

const val MAX_GUARDIANS = 6
const val MAX_CALLERS = 50

@Serializable
data class UserProfile(
    val id: Int,
    val email: String,
    @SerialName("birth_date") val birthDate: String? = null,
    @SerialName("is_guardian_certified") val isGuardianCertified: Boolean = false,
    @SerialName("auth_provider") val authProvider: String = "email",
    @SerialName("two_factor_method") val twoFactorMethod: String = "email_otp",
    @SerialName("subscription_active") val subscriptionActive: Boolean = false,
    @SerialName("subscription_plan") val subscriptionPlan: String = "",
)

@Serializable
data class AppDialCode(@SerialName("app_dial_code") val appDialCode: String)

@Serializable
data class PhoneSummary(
    @SerialName("device_id") val deviceId: Int,
    val name: String,
    @SerialName("peer_dial_code") val peerDialCode: String? = null,
    @SerialName("is_online") val isOnline: Boolean = false,
    @SerialName("can_call") val canCall: Boolean = true,
    @SerialName("can_receive") val canReceive: Boolean = true,
)

@Serializable
data class HouseholdSummary(
    val id: Int,
    val name: String,
    val timezone: String? = null,
    val role: String,
    @SerialName("display_name") val displayName: String? = null,
    @SerialName("avatar_icon") val avatarIcon: String? = null,
    @SerialName("avatar_color") val avatarColor: String? = null,
    @SerialName("is_default") val isDefault: Boolean = false,
    val phones: List<PhoneSummary> = emptyList(),
) {
    val isManager: Boolean get() = role == ROLE_OWNER || role == ROLE_GUARDIAN
    val isOwner: Boolean get() = role == ROLE_OWNER
}

@Serializable
data class HouseholdList(
    @SerialName("default_household_id") val defaultHouseholdId: Int? = null,
    val households: List<HouseholdSummary> = emptyList(),
)

@Serializable
data class Member(
    @SerialName("user_id") val userId: Int,
    val role: String,
    @SerialName("display_name") val displayName: String? = null,
    val email: String = "",
    @SerialName("avatar_icon") val avatarIcon: String? = null,
    @SerialName("avatar_color") val avatarColor: String? = null,
    @SerialName("app_dial_code") val appDialCode: String? = null,
    @SerialName("joined_at") val joinedAt: String? = null,
    @SerialName("is_me") val isMe: Boolean = false,
) {
    val label: String get() = displayName?.takeIf { it.isNotBlank() } ?: email.substringBefore('@').ifBlank { "Member" }
}

@Serializable
data class HouseholdDetail(
    val id: Int,
    val name: String,
    val timezone: String? = null,
    val role: String,
    @SerialName("display_name") val displayName: String? = null,
    val members: List<Member> = emptyList(),
)

@Serializable
data class HouseholdInvite(
    val id: Int,
    @SerialName("household_id") val householdId: Int,
    @SerialName("invite_code") val inviteCode: String,
    val role: String = ROLE_GUARDIAN,
    @SerialName("invited_email") val invitedEmail: String? = null,
    @SerialName("display_name") val displayName: String? = null,
    @SerialName("expires_at") val expiresAt: String = "",
    @SerialName("share_url") val shareUrl: String = "",
)

@Serializable
data class Device(
    val id: Int,
    val name: String,
    @SerialName("peer_dial_code") val peerDialCode: String? = null,
    @SerialName("last_known_ssid") val lastKnownSsid: String? = null,
    @SerialName("is_online") val isOnline: Boolean = false,
    @SerialName("last_seen") val lastSeen: String? = null,
    @SerialName("volume_level") val volumeLevel: Int = 50,
    @SerialName("e911_verified") val e911Verified: Boolean = false,
    @SerialName("e911_address_id") val e911AddressId: Int? = null,
    @SerialName("e911_pending_reconfirmation") val e911PendingReconfirmation: Boolean = false,
    @SerialName("household_id") val householdId: Int? = null,
    @SerialName("applied_trip_id") val appliedTripId: Int? = null,
)

@Serializable
data class Contact(
    val id: Int,
    val name: String,
    @SerialName("treefort_id") val treefortId: String? = null,
    @SerialName("allow_incoming") val allowIncoming: Boolean = true,
    @SerialName("allow_outgoing") val allowOutgoing: Boolean = true,
    @SerialName("speed_dial") val speedDial: String? = null,
    @SerialName("avatar_icon") val avatarIcon: String? = null,
    @SerialName("avatar_color") val avatarColor: String? = null,
    @SerialName("household_id") val householdId: Int? = null,
)

@Serializable
data class CallLogEntry(
    val id: Int,
    @SerialName("device_id") val deviceId: Int,
    @SerialName("contact_id") val contactId: Int? = null,
    val direction: String = "outbound",
    @SerialName("duration_seconds") val durationSeconds: Int = 0,
    val timestamp: String = "",
)

@Serializable
data class AuditEntry(
    val id: Int,
    @SerialName("action_type") val actionType: String,
    val summary: String,
    val details: String? = null,
    @SerialName("target_name") val targetName: String? = null,
    val timestamp: String = "",
    @SerialName("actor_name") val actorName: String? = null,
    @SerialName("actor_role") val actorRole: String? = null,
)

@Serializable
data class Address(
    val id: Int,
    @SerialName("street_line_1") val street1: String,
    @SerialName("street_line_2") val street2: String? = null,
    val city: String = "",
    val state: String = "",
    @SerialName("zip_code") val zip: String = "",
    @SerialName("is_temporary") val isTemporary: Boolean = false,
    @SerialName("household_id") val householdId: Int? = null,
) {
    val oneLine: String
        get() = listOfNotNull(street1, street2?.takeIf { it.isNotBlank() }, "$city, $state $zip".trim()).joinToString(", ")
}

@Serializable
data class AddressResult(val address: Address)

@Serializable
data class QuietRule(
    val id: String? = null,
    val name: String? = null,
    val enabled: Boolean = true,
    val days: List<String> = emptyList(),
    val startTime: String,
    val endTime: String,
)

@Serializable
data class QuietRules(val rules: List<QuietRule> = emptyList())

@Serializable
data class Trip(
    val id: Int,
    @SerialName("household_id") val householdId: Int,
    @SerialName("device_id") val deviceId: Int? = null,
    val name: String? = null,
    @SerialName("starts_at") val startsAt: String,
    @SerialName("ends_at") val endsAt: String,
    val timezone: String,
    @SerialName("address_id") val addressId: Int? = null,
    @SerialName("quiet_hours_override") val quietHoursOverride: Boolean = false,
    val rules: List<QuietRule> = emptyList(),
    val status: String = "upcoming",
)

@Serializable
data class FriendInvite(
    @SerialName("invite_code") val inviteCode: String,
    @SerialName("dial_code") val dialCode: String = "",
    @SerialName("expires_in_hours") val expiresInHours: Int = 48,
    @SerialName("share_url") val shareUrl: String = "",
    @SerialName("allow_caller") val allowCaller: Boolean = false,
)

@Serializable
data class FriendInviteAccepted(
    val mode: String,
    val name: String = "",
    @SerialName("treefort_id") val treefortId: String = "",
    @SerialName("household_id") val householdId: Int = 0,
    @SerialName("household_name") val householdName: String? = null,
    @SerialName("app_dial_code") val appDialCode: String? = null,
)

@Serializable
data class IceServerList(@SerialName("ice_servers") val iceServers: List<IceServer> = emptyList())

@Serializable
data class IceServer(val urls: List<String> = emptyList(), val username: String? = null, val credential: String? = null)
