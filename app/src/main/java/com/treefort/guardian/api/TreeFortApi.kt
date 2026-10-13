package com.treefort.guardian.api

import com.treefort.guardian.AppConfig
import com.treefort.guardian.SupabaseManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.encodeToString
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit

/** An error from the backend (or the network, status 0) with a message safe to show a guardian. */
class ApiException(val status: Int, message: String) : Exception(message) {
    val isConflict get() = status == 409
    val isForbidden get() = status == 403
    val isNotFound get() = status == 404
}

/** Tries IPv4 first: broken home IPv6 routes otherwise cost ~connectTimeout per AAAA address. */
object Ipv4FirstDns : okhttp3.Dns {
    override fun lookup(hostname: String): List<java.net.InetAddress> =
        okhttp3.Dns.SYSTEM.lookup(hostname).sortedBy { it is java.net.Inet6Address }
}

/**
 * The only way the app reads or writes TreeFort data. Every call carries the Supabase access token
 * as `Authorization: Bearer`. Errors are surfaced as [ApiException]; nothing is swallowed.
 */
object TreeFortApi {
    val json = Json { ignoreUnknownKeys = true; coerceInputValues = true; encodeDefaults = true; explicitNulls = false }

    /** Called when the backend rejects the session even after a refresh. Set by the app store. */
    @Volatile var onSessionExpired: (() -> Unit)? = null

    private val http = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(25, TimeUnit.SECONDS)
        .dns(Ipv4FirstDns)
        .build()

    private val JSON_TYPE = "application/json".toMediaType()

    // ---- plumbing ---------------------------------------------------------------------------

    private fun buildRequest(method: String, path: String, query: Map<String, Any?>, body: JsonElement?, token: String?): Request {
        val url = (AppConfig.httpBase + path).toHttpUrl().newBuilder().apply {
            query.forEach { (k, v) -> if (v != null) addQueryParameter(k, v.toString()) }
        }.build()
        val rb = body?.let { json.encodeToString(JsonElement.serializer(), it).toRequestBody(JSON_TYPE) }
            ?: if (method == "POST" || method == "PUT" || method == "PATCH") "{}".toRequestBody(JSON_TYPE) else null
        return Request.Builder().url(url).method(method, rb).apply {
            if (token != null) header("Authorization", "Bearer $token")
            header("Accept", "application/json")
        }.build()
    }

    private suspend fun execute(method: String, path: String, query: Map<String, Any?>, body: JsonElement?): String =
        withContext(Dispatchers.IO) {
            var attempt = 0
            while (true) {
                val token = SupabaseManager.accessToken()
                    ?: throw ApiException(401, "You're signed out. Sign in again.")
                val req = buildRequest(method, path, query, body, token)
                val (code, text) = try {
                    http.newCall(req).execute().use { it.code to (it.body?.string() ?: "") }
                } catch (e: IOException) {
                    throw ApiException(0, "Can't reach TreeFort. Check your connection and try again.")
                }
                if (code in 200..299) return@withContext text
                if (code == 401 && attempt == 0) {
                    attempt++
                    try { SupabaseManager.refresh(); continue } catch (_: Exception) { /* fall through */ }
                }
                if (code == 401) onSessionExpired?.invoke()
                throw ApiException(code, errorMessage(code, text))
            }
            @Suppress("UNREACHABLE_CODE") ""
        }

    internal fun errorMessage(code: Int, body: String): String {
        val detail = runCatching { json.parseToJsonElement(body).jsonObject["detail"] }.getOrNull()
        val text = when (detail) {
            is JsonPrimitive -> detail.contentOrNull
            is JsonArray -> detail.mapNotNull { (it as? JsonObject)?.get("msg")?.let { m -> (m as? JsonPrimitive)?.contentOrNull } }
                .joinToString("; ").ifBlank { null }
            else -> null
        }
        if (!text.isNullOrBlank()) return text
        return when (code) {
            401 -> "You're signed out. Sign in again."
            403 -> "You don't have permission to do that."
            404 -> "That wasn't found."
            409 -> "That conflicts with the current state. Refresh and try again."
            429 -> "Too many tries. Wait a few minutes and try again."
            in 500..599 -> "TreeFort had a problem. Try again in a moment."
            else -> "Something went wrong (error $code)."
        }
    }

    private suspend inline fun <reified T> request(method: String, path: String, query: Map<String, Any?> = emptyMap(), body: JsonElement? = null): T {
        val text = execute(method, path, query, body)
        return try {
            json.decodeFromString<T>(text)
        } catch (e: Exception) {
            throw ApiException(500, "TreeFort sent something this app didn't understand. Update the app and try again.")
        }
    }

    private suspend fun requestUnit(method: String, path: String, query: Map<String, Any?> = emptyMap(), body: JsonElement? = null) {
        execute(method, path, query, body)
    }

    private fun obj(block: kotlinx.serialization.json.JsonObjectBuilder.() -> Unit): JsonObject = buildJsonObject(block)

    // ---- account ----------------------------------------------------------------------------

    suspend fun me(): UserProfile = request("GET", "/api/auth/me")

    suspend fun certify(birthDate: String): UserProfile =
        request("POST", "/api/auth/certify", body = obj { put("birth_date", birthDate); put("is_guardian_certified", true) })

    /** Allocates (once) and returns this account's own TreeFort number for the app. */
    suspend fun appDialCode(): String = request<AppDialCode>("POST", "/api/user/app-dial-code").appDialCode

    /** 409 means this account is the last owner of a shared household. */
    suspend fun scheduleAccountDeletion() = requestUnit("POST", "/api/user/schedule-deletion")

    // ---- households and members -------------------------------------------------------------

    suspend fun households(): HouseholdList = request("GET", "/api/households")
    suspend fun household(id: Int): HouseholdDetail = request("GET", "/api/households/$id")

    suspend fun updateHousehold(id: Int, name: String?, timezone: String?, clearTimezone: Boolean = false): HouseholdSummary =
        request("PATCH", "/api/households/$id", body = obj {
            if (name != null) put("name", name)
            if (timezone != null) put("timezone", timezone) else if (clearTimezone) put("timezone", JsonNull)
        })

    suspend fun updateMember(householdId: Int, userId: Int, role: String? = null, displayName: String? = null, avatarIcon: String? = null, avatarColor: String? = null): Member =
        request("PATCH", "/api/households/$householdId/members/$userId", body = obj {
            if (role != null) put("role", role)
            if (displayName != null) put("display_name", displayName)
            if (avatarIcon != null) put("avatar_icon", avatarIcon)
            if (avatarColor != null) put("avatar_color", avatarColor)
        })

    /** Leave (own user id) or remove a member. 409: last owner cannot leave. */
    suspend fun removeMember(householdId: Int, userId: Int) =
        requestUnit("DELETE", "/api/households/$householdId/members/$userId")

    suspend fun createHouseholdInvite(householdId: Int, email: String?, displayName: String?): HouseholdInvite =
        request("POST", "/api/households/$householdId/invites", body = obj {
            put("role", ROLE_GUARDIAN)
            if (!email.isNullOrBlank()) put("invited_email", email.trim())
            if (!displayName.isNullOrBlank()) put("display_name", displayName.trim())
        })

    suspend fun householdInvites(householdId: Int): List<HouseholdInvite> = request("GET", "/api/households/$householdId/invites")
    suspend fun revokeHouseholdInvite(householdId: Int, inviteId: Int) = requestUnit("DELETE", "/api/households/$householdId/invites/$inviteId")

    suspend fun acceptHouseholdInvite(code: String, displayName: String?, avatarIcon: String?, avatarColor: String?): HouseholdSummary =
        request("POST", "/api/households/invites/accept", body = obj {
            put("invite_code", code.trim())
            if (!displayName.isNullOrBlank()) put("display_name", displayName.trim())
            if (avatarIcon != null) put("avatar_icon", avatarIcon)
            if (avatarColor != null) put("avatar_color", avatarColor)
        })

    // ---- phones -----------------------------------------------------------------------------

    suspend fun devices(householdId: Int?): List<Device> = request("GET", "/api/devices", mapOf("household_id" to householdId))

    suspend fun claimDevice(code: String, kidName: String, room: String, householdId: Int?): Device =
        request("POST", "/api/devices/claim", mapOf("household_id" to householdId),
            obj { put("claim_code", code); put("kid_name", kidName); put("room", room) })

    suspend fun releaseDevice(id: Int) = requestUnit("DELETE", "/api/devices/$id")
    suspend fun setVolume(id: Int, level: Int) = requestUnit("PATCH", "/api/devices/$id/volume", body = obj { put("volume_level", level) })

    suspend fun quietHours(deviceId: Int): List<QuietRule> = request<QuietRules>("GET", "/api/devices/$deviceId/quiet-hours").rules

    suspend fun saveQuietHours(deviceId: Int, rules: List<QuietRule>): List<QuietRule> =
        request<QuietRules>("PUT", "/api/devices/$deviceId/quiet-hours",
            body = json.encodeToJsonElement(QuietRules.serializer(), QuietRules(rules.map { it.copy(id = null) }))).rules

    suspend fun confirmAddress(deviceId: Int, addressId: Int) =
        requestUnit("POST", "/api/devices/$deviceId/confirm-address", body = obj { put("address_id", addressId) })

    // ---- Safelist (Friends) -----------------------------------------------------------------

    suspend fun contacts(householdId: Int?): List<Contact> = request("GET", "/api/contacts", mapOf("household_id" to householdId))

    suspend fun addContact(householdId: Int?, name: String, treefortId: String, allowIncoming: Boolean, allowOutgoing: Boolean, speedDial: String?, avatarIcon: String?, avatarColor: String?): Contact =
        request("POST", "/api/contacts", mapOf("household_id" to householdId), obj {
            put("name", name); put("treefort_id", treefortId)
            put("allow_incoming", allowIncoming); put("allow_outgoing", allowOutgoing)
            if (speedDial != null) put("speed_dial", speedDial)
            if (avatarIcon != null) put("avatar_icon", avatarIcon)
            if (avatarColor != null) put("avatar_color", avatarColor)
        })

    /** PATCH sends every field the editor owns, so null clears the speed dial. The TreeFort number is never edited. */
    suspend fun updateContact(id: Int, name: String, allowIncoming: Boolean, allowOutgoing: Boolean, speedDial: String?, avatarIcon: String?, avatarColor: String?): Contact =
        request("PATCH", "/api/contacts/$id", body = obj {
            put("name", name)
            put("allow_incoming", allowIncoming); put("allow_outgoing", allowOutgoing)
            put("speed_dial", speedDial?.let { JsonPrimitive(it) } ?: JsonNull)
            put("avatar_icon", avatarIcon?.let { JsonPrimitive(it) } ?: JsonNull)
            put("avatar_color", avatarColor?.let { JsonPrimitive(it) } ?: JsonNull)
        })

    /** HH-SEC-05: deletes go through the backend (which also revokes a caller's membership), never PostgREST. */
    suspend fun deleteContact(id: Int) = requestUnit("DELETE", "/api/contacts/$id")

    suspend fun createFriendInvite(alias: String, allowCaller: Boolean, deviceId: Int?, householdId: Int?): FriendInvite =
        request("POST", "/api/contacts/invite", mapOf("household_id" to householdId), obj {
            put("friend_alias", alias); put("allow_caller", allowCaller)
            if (deviceId != null) put("device_id", deviceId)
        })

    suspend fun acceptFriendInvite(code: String, alias: String, asCaller: Boolean, householdId: Int?): FriendInviteAccepted =
        request("POST", "/api/contacts/accept-invite", mapOf("household_id" to householdId), obj {
            put("invite_code", code.trim()); put("friend_alias", alias); put("as_caller", asCaller)
        })

    // ---- Logbook ----------------------------------------------------------------------------

    suspend fun calls(householdId: Int?): List<CallLogEntry> = request("GET", "/api/calls", mapOf("household_id" to householdId))
    suspend fun auditLogs(householdId: Int?): List<AuditEntry> = request("GET", "/api/audit-logs", mapOf("household_id" to householdId))

    suspend fun postAudit(householdId: Int?, actionType: String, summary: String, details: String? = null, target: String? = null) =
        requestUnit("POST", "/api/audit-logs", mapOf("household_id" to householdId), obj {
            put("action_type", actionType); put("summary", summary)
            if (details != null) put("details", details)
            if (target != null) put("target_name", target)
        })

    // ---- emergency addresses (saved for later; no dispatch claims) --------------------------

    suspend fun addresses(householdId: Int?): List<Address> = request("GET", "/api/e911/addresses", mapOf("household_id" to householdId))

    suspend fun addAddress(householdId: Int?, street1: String, street2: String?, city: String, state: String, zip: String, temporary: Boolean): Address =
        request<AddressResult>("POST", "/api/e911/addresses", mapOf("household_id" to householdId), obj {
            put("street_line_1", street1)
            if (!street2.isNullOrBlank()) put("street_line_2", street2)
            put("city", city); put("state", state); put("zip_code", zip); put("is_temporary", temporary)
        }).address

    // ---- vacation / trips -------------------------------------------------------------------

    suspend fun trips(householdId: Int, includePast: Boolean = false): List<Trip> =
        request("GET", "/api/households/$householdId/trips", mapOf("include_past" to includePast))

    /** `startsAt`/`endsAt` are local wall times (yyyy-MM-ddTHH:mm:ss) in [timezone]. `addressId` needs `deviceId`. */
    suspend fun createTrip(householdId: Int, name: String?, deviceId: Int?, startsAt: String, endsAt: String, timezone: String, addressId: Int?, quietOverride: Boolean, rules: List<QuietRule>): Trip =
        request("POST", "/api/households/$householdId/trips", body = obj {
            if (!name.isNullOrBlank()) put("name", name.trim())
            if (deviceId != null) put("device_id", deviceId)
            put("starts_at", startsAt); put("ends_at", endsAt); put("timezone", timezone)
            if (addressId != null) put("address_id", addressId)
            put("quiet_hours_override", quietOverride)
            put("rules", json.encodeToJsonElement(kotlinx.serialization.builtins.ListSerializer(QuietRule.serializer()), rules.map { it.copy(id = null) }))
        })

    /** Cancels an upcoming trip or ends an active one now. */
    suspend fun cancelTrip(householdId: Int, tripId: Int): Trip = request("DELETE", "/api/households/$householdId/trips/$tripId")

    // ---- calling ----------------------------------------------------------------------------

    suspend fun iceServers(): List<IceServer> = request<IceServerList>("GET", "/api/webrtc/ice-servers").iceServers

    suspend fun registerPushToken(dialCode: String, token: String) =
        requestUnit("POST", "/api/push/voip-token", body = obj { put("dial_code", dialCode); put("voip_token", token); put("platform", "android") })
}
