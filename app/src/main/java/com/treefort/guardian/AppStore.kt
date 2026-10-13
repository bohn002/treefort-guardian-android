package com.treefort.guardian

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.treefort.guardian.api.Address
import com.treefort.guardian.api.ApiException
import com.treefort.guardian.api.AuditEntry
import com.treefort.guardian.api.CallLogEntry
import com.treefort.guardian.api.Contact
import com.treefort.guardian.api.Device
import com.treefort.guardian.api.FriendInvite
import com.treefort.guardian.api.FriendInviteAccepted
import com.treefort.guardian.api.HouseholdDetail
import com.treefort.guardian.api.HouseholdInvite
import com.treefort.guardian.api.HouseholdSummary
import com.treefort.guardian.api.Member
import com.treefort.guardian.api.QuietRule
import com.treefort.guardian.api.Trip
import com.treefort.guardian.api.TreeFortApi
import com.treefort.guardian.api.UserProfile
import io.github.jan.supabase.gotrue.SessionStatus
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

enum class Phase { Starting, ConfigMissing, SignedOut, NeedsCertify, Loading, LoadFailed, Ready }

enum class MessageKind { Info, Success, Danger }

data class StoreMessage(val text: String, val kind: MessageKind = MessageKind.Info, val id: Long = System.nanoTime())

/** Message text from a failed [Result] (always user-safe: it came from [ApiException] or a fixed string). */
val Result<*>.errorText: String get() = exceptionOrNull()?.message ?: "Something went wrong. Try again."

/**
 * The app's single source of truth. Everything comes from the backend API (TreeFortApi) using the
 * Supabase session; nothing is seeded and nothing is read from or written to PostgREST. Mutations
 * return a [Result] so dialogs can show the backend's message inline; nothing is swallowed.
 */
object AppStore {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var started = false
    private var appContext: Context? = null

    // ---- session ----------------------------------------------------------------------------

    var phase by mutableStateOf(Phase.Starting)
        private set
    var loadError by mutableStateOf<String?>(null)
        private set
    var profile by mutableStateOf<UserProfile?>(null)
        private set
    var myDialCode by mutableStateOf("")
        private set

    // ---- household-scoped data --------------------------------------------------------------

    var households by mutableStateOf<List<HouseholdSummary>>(emptyList())
        private set
    var selectedId by mutableStateOf<Int?>(null)
        private set
    var devices by mutableStateOf<List<Device>>(emptyList())
        private set
    var contacts by mutableStateOf<List<Contact>>(emptyList())
        private set
    var calls by mutableStateOf<List<CallLogEntry>>(emptyList())
        private set
    var audit by mutableStateOf<List<AuditEntry>>(emptyList())
        private set
    var addresses by mutableStateOf<List<Address>>(emptyList())
        private set
    var trips by mutableStateOf<List<Trip>>(emptyList())
        private set
    var detail by mutableStateOf<HouseholdDetail?>(null)
        private set
    var invites by mutableStateOf<List<HouseholdInvite>>(emptyList())
        private set
    var refreshing by mutableIntStateOf(0)
        private set

    var message by mutableStateOf<StoreMessage?>(null)
        private set

    val selected: HouseholdSummary? get() = households.firstOrNull { it.id == selectedId }
    val isManager: Boolean get() = selected?.isManager == true
    val isOwner: Boolean get() = selected?.isOwner == true
    val isCaller: Boolean get() = selected != null && !isManager

    /** A signed-in account with no household yet: the first claim or invite creates one. */
    val hasHousehold: Boolean get() = households.isNotEmpty()

    fun say(text: String, kind: MessageKind = MessageKind.Info) { message = StoreMessage(text, kind) }
    fun dismissMessage() { message = null }

    // ---- lifecycle --------------------------------------------------------------------------

    fun start(context: Context) {
        if (started) return
        started = true
        appContext = context.applicationContext
        if (AppConfig.missingConfig) { phase = Phase.ConfigMissing; return }
        TreeFortApi.onSessionExpired = { scope.launch { signedOut() } }
        WebRTCManager.peerResolver = ::peerFor
        scope.launch {
            SupabaseManager.sessionStatus.collect { status ->
                when (status) {
                    is SessionStatus.Authenticated -> if (phase == Phase.Starting || phase == Phase.SignedOut) bootstrap()
                    is SessionStatus.NotAuthenticated -> signedOut()
                    else -> Unit
                }
            }
        }
    }

    /** Foreground: reconnect signaling if it dropped and refresh the data. */
    fun onResume() {
        if (phase != Phase.Ready) return
        if (!WebRTCManager.isOnline.value && myDialCode.isNotBlank()) WebRTCManager.connectSignaling(myDialCode)
        scope.launch { runCatching { refreshHousehold() } }
    }

    fun retryBootstrap() { scope.launch { bootstrap() } }

    private suspend fun bootstrap() {
        phase = Phase.Loading
        loadError = null
        try {
            val me = TreeFortApi.me()
            profile = me
            if (!me.isGuardianCertified) { phase = Phase.NeedsCertify; return }
            finishBootstrap()
        } catch (e: CancellationException) {
            throw e
        } catch (e: ApiException) {
            if (e.status == 401) return // onSessionExpired already moved us to SignedOut
            loadError = e.message
            phase = Phase.LoadFailed
        } catch (e: Exception) {
            loadError = "Something went wrong. Try again."
            phase = Phase.LoadFailed
        }
    }

    private suspend fun finishBootstrap() {
        myDialCode = TreeFortApi.appDialCode()
        loadHouseholds()
        refreshHousehold()
        WebRTCManager.callerName = callerDisplayName()
        appContext?.let { ctx ->
            WebRTCManager.initialize(ctx)
            TreeFortPush.init(ctx, myDialCode)
        }
        WebRTCManager.connectSignaling(myDialCode)
        phase = Phase.Ready
    }

    private fun callerDisplayName(): String =
        selected?.displayName?.takeIf { it.isNotBlank() } ?: profile?.email?.substringBefore('@') ?: "TreeFort Guardian"

    private suspend fun signedOut() {
        WebRTCManager.disconnectSignaling()
        appContext?.let { TreeFortPush.clear(it) }
        profile = null; myDialCode = ""
        households = emptyList(); selectedId = null
        clearHouseholdData()
        if (phase != Phase.ConfigMissing) phase = Phase.SignedOut
    }

    private fun clearHouseholdData() {
        devices = emptyList(); contacts = emptyList(); calls = emptyList(); audit = emptyList()
        addresses = emptyList(); trips = emptyList(); detail = null; invites = emptyList()
    }

    // ---- loading ----------------------------------------------------------------------------

    private suspend fun loadHouseholds() {
        val list = TreeFortApi.households()
        households = list.households
        val remembered = AppPrefs.selectedHouseholdId
        selectedId = list.households.firstOrNull { it.id == selectedId }?.id
            ?: list.households.firstOrNull { it.id == remembered }?.id
            ?: list.households.firstOrNull { it.id == list.defaultHouseholdId }?.id
            ?: list.households.firstOrNull()?.id
    }

    /** Reloads the household list and everything the selected household's role may see. */
    suspend fun refreshHousehold() {
        refreshing++
        try {
            loadHouseholds()
            val h = selected
            if (h == null) { clearHouseholdData(); return }
            WebRTCManager.callerName = callerDisplayName()
            if (!h.isManager) {
                // A caller sees only the household name and the phones they may call.
                clearHouseholdData()
                detail = runCatching { TreeFortApi.household(h.id) }.getOrNull()
                return
            }
            val id = h.id
            devices = TreeFortApi.devices(id)
            contacts = TreeFortApi.contacts(id)
            calls = TreeFortApi.calls(id)
            audit = TreeFortApi.auditLogs(id)
            addresses = TreeFortApi.addresses(id)
            trips = TreeFortApi.trips(id)
            detail = TreeFortApi.household(id)
            invites = if (h.isOwner) runCatching { TreeFortApi.householdInvites(id) }.getOrDefault(emptyList()) else emptyList()
        } catch (e: CancellationException) {
            throw e
        } catch (e: ApiException) {
            if (e.status != 401) say(e.message ?: "Couldn't refresh.", MessageKind.Danger)
        } finally {
            refreshing--
        }
    }

    fun refresh() { scope.launch { refreshHousehold() } }

    fun selectHousehold(id: Int) {
        if (id == selectedId) return
        selectedId = id
        AppPrefs.selectedHouseholdId = id
        clearHouseholdData()
        scope.launch { refreshHousehold() }
    }

    private suspend fun <T> guarded(block: suspend () -> T): Result<T> = try {
        Result.success(block())
    } catch (e: CancellationException) {
        throw e
    } catch (e: ApiException) {
        Result.failure(e)
    } catch (e: Exception) {
        Result.failure(Exception("Something went wrong. Try again."))
    }

    /** Runs a mutation, then reloads. The backend's message is returned on failure. */
    private suspend fun <T> mutate(block: suspend () -> T): Result<T> {
        val r = guarded(block)
        if (r.isSuccess) refreshHousehold()
        return r
    }

    // ---- sign-in ----------------------------------------------------------------------------

    private fun authFailure(e: Exception, fallback: String): Exception {
        val m = e.message.orEmpty()
        return Exception(
            when {
                m.contains("Unable to resolve", true) || m.contains("timeout", true) || m.contains("network", true) ->
                    "Can't reach TreeFort. Check your connection and try again."
                m.contains("Invalid login", true) -> "That email and password don't match."
                m.contains("not confirmed", true) -> "Confirm your email first. We sent you a link."
                m.contains("rate", true) || m.contains("429") -> "Too many tries. Wait a few minutes and try again."
                m.contains("already", true) -> "That email already has an account. Sign in instead."
                m.contains("Password", true) && m.contains("weak", true) -> "Choose a longer, less common password."
                else -> fallback
            }
        )
    }

    suspend fun signIn(email: String, password: String): Result<Unit> = try {
        SupabaseManager.signIn(email.trim(), password); Result.success(Unit)
    } catch (e: CancellationException) { throw e } catch (e: Exception) {
        Result.failure(authFailure(e, "Couldn't sign in. Check your email and password."))
    }

    /** Success(true) = signed in; Success(false) = confirm the email first. */
    suspend fun signUp(email: String, password: String): Result<Boolean> = try {
        Result.success(SupabaseManager.signUp(email.trim(), password))
    } catch (e: CancellationException) { throw e } catch (e: Exception) {
        Result.failure(authFailure(e, "Couldn't create the account. Try again."))
    }

    suspend fun sendEmailCode(email: String): Result<Unit> = try {
        SupabaseManager.sendEmailCode(email.trim()); Result.success(Unit)
    } catch (e: CancellationException) { throw e } catch (e: Exception) {
        Result.failure(authFailure(e, "Couldn't send the code. Check the email address."))
    }

    suspend fun verifyEmailCode(email: String, code: String): Result<Unit> = try {
        SupabaseManager.verifyEmailCode(email.trim(), code.trim()); Result.success(Unit)
    } catch (e: CancellationException) { throw e } catch (e: Exception) {
        Result.failure(authFailure(e, "That code didn't work. Check it and try again."))
    }

    suspend fun certify(birthDate: String): Result<Unit> {
        val r = guarded { TreeFortApi.certify(birthDate) }
        r.getOrNull()?.let { profile = it; if (it.isGuardianCertified) { phase = Phase.Loading; bootstrap() } }
        return r.map { }
    }

    fun signOut() {
        scope.launch {
            try { SupabaseManager.signOut() } catch (_: Exception) { /* local session is cleared below regardless */ }
            signedOut()
        }
    }

    /** 409: this account is the last owner of a shared household. */
    suspend fun deleteAccount(): Result<Unit> {
        val r = guarded { TreeFortApi.scheduleAccountDeletion() }
        val e = r.exceptionOrNull()
        if (e is ApiException && e.isConflict) {
            return Result.failure(Exception(
                "You're the last owner of a household that has other members. Make another member an owner, or remove the others, then try again."
            ))
        }
        if (r.isSuccess) {
            try { SupabaseManager.signOut() } catch (_: Exception) { }
            signedOut()
        }
        return r
    }

    // ---- phones -----------------------------------------------------------------------------

    suspend fun claimDevice(code: String, kidName: String, room: String) =
        mutate { TreeFortApi.claimDevice(code, kidName.trim(), room.trim(), selectedId) }

    suspend fun releaseDevice(id: Int) = mutate { TreeFortApi.releaseDevice(id) }

    suspend fun setVolume(id: Int, level: Int): Result<Unit> {
        val r = guarded { TreeFortApi.setVolume(id, level) }
        if (r.isSuccess) devices = devices.map { if (it.id == id) it.copy(volumeLevel = level) else it }
        return r
    }

    suspend fun loadQuietRules(deviceId: Int) = guarded { TreeFortApi.quietHours(deviceId) }
    suspend fun saveQuietRules(deviceId: Int, rules: List<QuietRule>) = guarded { TreeFortApi.saveQuietHours(deviceId, rules) }

    suspend fun addAddress(street1: String, street2: String?, city: String, state: String, zip: String, temporary: Boolean) =
        mutate { TreeFortApi.addAddress(selectedId, street1.trim(), street2?.trim(), city.trim(), state.trim(), zip.trim(), temporary) }

    suspend fun confirmAddress(deviceId: Int, addressId: Int) = mutate { TreeFortApi.confirmAddress(deviceId, addressId) }

    // ---- Safelist ---------------------------------------------------------------------------

    suspend fun addContact(name: String, treefortId: String, allowIncoming: Boolean, allowOutgoing: Boolean, speedDial: String?, icon: String?, color: String?) =
        mutate { TreeFortApi.addContact(selectedId, name.trim(), treefortId.trim(), allowIncoming, allowOutgoing, speedDial, icon, color) }

    suspend fun updateContact(id: Int, name: String, allowIncoming: Boolean, allowOutgoing: Boolean, speedDial: String?, icon: String?, color: String?) =
        mutate { TreeFortApi.updateContact(id, name.trim(), allowIncoming, allowOutgoing, speedDial, icon, color) }

    /**
     * HH-SEC-05: deletes only through `DELETE /api/contacts/{id}` (which also revokes a caller's
     * membership). A failure is returned to the caller, and the list is not touched until the
     * backend confirms. The TreeFort number is never written from the app.
     */
    suspend fun deleteContact(id: Int): Result<Unit> = mutate { TreeFortApi.deleteContact(id) }

    suspend fun createFriendInvite(alias: String, allowCaller: Boolean, deviceId: Int?): Result<FriendInvite> =
        mutate { TreeFortApi.createFriendInvite(alias.trim(), allowCaller, deviceId, selectedId) }

    suspend fun acceptFriendInvite(code: String, alias: String, asCaller: Boolean): Result<FriendInviteAccepted> {
        val r = guarded { TreeFortApi.acceptFriendInvite(code, alias.trim(), asCaller, selectedId) }
        if (r.isSuccess) {
            r.getOrNull()?.takeIf { it.householdId != 0 }?.let { selectedId = it.householdId; AppPrefs.selectedHouseholdId = it.householdId }
            refreshHousehold()
        }
        return r
    }

    // ---- household, members, trips ----------------------------------------------------------

    suspend fun renameHousehold(name: String, timezone: String?) =
        mutate { selected?.let { TreeFortApi.updateHousehold(it.id, name.trim(), timezone) } ?: error("No household") }

    suspend fun updateMember(m: Member, role: String? = null, displayName: String? = null, icon: String? = null, color: String? = null): Result<Member> {
        val hid = selectedId ?: return Result.failure(Exception("No household selected."))
        return mutate { TreeFortApi.updateMember(hid, m.userId, role, displayName?.trim(), icon, color) }
    }

    /** Leave (own entry) or remove. 409: the last owner can't leave. */
    suspend fun removeMember(m: Member): Result<Unit> {
        val hid = selectedId ?: return Result.failure(Exception("No household selected."))
        val r = guarded { TreeFortApi.removeMember(hid, m.userId) }
        val e = r.exceptionOrNull()
        if (e is ApiException && e.isConflict) {
            return Result.failure(Exception("A household needs at least one owner. Make someone else an owner first."))
        }
        if (r.isSuccess) {
            if (m.isMe) { AppPrefs.selectedHouseholdId = -1; selectedId = null }
            refreshHousehold()
        }
        return r
    }

    suspend fun createHouseholdInvite(email: String?, displayName: String?): Result<HouseholdInvite> {
        val hid = selectedId ?: return Result.failure(Exception("No household selected."))
        return mutate { TreeFortApi.createHouseholdInvite(hid, email, displayName) }
    }

    suspend fun revokeHouseholdInvite(inviteId: Int): Result<Unit> {
        val hid = selectedId ?: return Result.failure(Exception("No household selected."))
        return mutate { TreeFortApi.revokeHouseholdInvite(hid, inviteId) }
    }

    suspend fun acceptHouseholdInvite(code: String, displayName: String?, icon: String?, color: String?): Result<HouseholdSummary> {
        val r = guarded { TreeFortApi.acceptHouseholdInvite(code, displayName, icon, color) }
        r.getOrNull()?.let { selectedId = it.id; AppPrefs.selectedHouseholdId = it.id; refreshHousehold() }
        return r
    }

    suspend fun createTrip(name: String?, deviceId: Int?, startsAt: String, endsAt: String, timezone: String, addressId: Int?, quietOverride: Boolean, rules: List<QuietRule>): Result<Trip> {
        val hid = selectedId ?: return Result.failure(Exception("No household selected."))
        // The backend rejects an address without a phone; fail fast with the same rule (ADR 0002).
        if (addressId != null && deviceId == null) return Result.failure(Exception("Pick one phone to give a trip its own emergency address."))
        return mutate { TreeFortApi.createTrip(hid, name, deviceId, startsAt, endsAt, timezone, addressId, quietOverride, rules) }
    }

    suspend fun cancelTrip(tripId: Int): Result<Trip> {
        val hid = selectedId ?: return Result.failure(Exception("No household selected."))
        return mutate { TreeFortApi.cancelTrip(hid, tripId) }
    }

    // ---- calling ----------------------------------------------------------------------------

    fun placeCall(dialCode: String, name: String, icon: String?, color: String?) {
        if (!WebRTCManager.isOnline.value) {
            say("Still connecting to TreeFort. Try again in a moment.", MessageKind.Danger)
            return
        }
        WebRTCManager.startCall(dialCode, name, icon, color)
    }

    /** Safelist name and avatar for a dial code, used for incoming calls and the call screen. */
    fun peerFor(dialCode: String): PeerInfo? {
        contacts.firstOrNull { it.treefortId == dialCode }?.let { return PeerInfo(it.name, it.avatarIcon, it.avatarColor) }
        devices.firstOrNull { it.peerDialCode == dialCode }?.let { return PeerInfo(it.name, "phone-call", "primary-blue") }
        detail?.members?.firstOrNull { it.appDialCode == dialCode }?.let { return PeerInfo(it.label, it.avatarIcon, it.avatarColor) }
        households.flatMap { it.phones }.firstOrNull { it.peerDialCode == dialCode }?.let { return PeerInfo(it.name, "phone-call", "primary-blue") }
        return null
    }
}
