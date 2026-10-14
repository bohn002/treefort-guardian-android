package com.treefort.guardian

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.treefort.guardian.theme.BorderInk
import com.treefort.guardian.theme.CanvasBg
import com.treefort.guardian.theme.CardBg
import com.treefort.guardian.theme.Ink
import com.treefort.guardian.theme.MutedText
import com.treefort.guardian.theme.Tf
import com.treefort.guardian.theme.TreeFortTokens
import com.treefort.guardian.theme.upper
import com.treefort.guardian.ui.ActiveCallOverlay
import com.treefort.guardian.ui.CampfireScreen
import com.treefort.guardian.ui.AuthScreen
import com.treefort.guardian.ui.Banner
import com.treefort.guardian.ui.BannerKind
import com.treefort.guardian.ui.CertifyScreen
import com.treefort.guardian.ui.ConfigMissingScreen
import com.treefort.guardian.ui.FriendsTab
import com.treefort.guardian.ui.HelpScreen
import com.treefort.guardian.ui.JoinDialog
import com.treefort.guardian.ui.LoadFailedScreen
import com.treefort.guardian.ui.LoadingScreen
import com.treefort.guardian.ui.LogbookTab
import com.treefort.guardian.ui.MembersScreen
import com.treefort.guardian.ui.Ph
import com.treefort.guardian.ui.SettingsTab
import com.treefort.guardian.ui.Sub
import com.treefort.guardian.ui.TactileButton
import com.treefort.guardian.ui.TactilePill
import com.treefort.guardian.ui.TfFormDialog
import com.treefort.guardian.ui.TreeFortTab
import com.treefort.guardian.ui.TripsScreen
import com.treefort.guardian.ui.UnlockPrompt

private val S = TreeFortTokens.Space

/** Root: routes by session phase, and lays the call screen over everything while a call is live. */
@Composable
fun MainNavigation() {
    val callState by WebRTCManager.callState.collectAsState()
    Box(Modifier.fillMaxSize().background(CanvasBg)) {
        when (AppStore.phase) {
            Phase.Starting, Phase.Loading -> LoadingScreen()
            Phase.ConfigMissing -> ConfigMissingScreen()
            Phase.SignedOut -> AuthScreen()
            Phase.NeedsCertify -> CertifyScreen()
            Phase.LoadFailed -> LoadFailedScreen()
            Phase.Ready -> {
                MainScaffold()
                if (callState !is CallState.Idle) ActiveCallOverlay(callState)
            }
        }
    }
}

private class TabSpec(val label: String, val icon: ImageVector, val activeIcon: ImageVector, val fill: Color)

private val TABS = listOf(
    TabSpec("TreeFort", Ph.Phone, Ph.PhoneFill, TreeFortTokens.Palette.SafeGreen),
    TabSpec("Friends", Ph.UsersThree, Ph.UsersThreeFill, TreeFortTokens.Palette.PrimaryBlue),
    TabSpec("Logbook", Ph.Logbook, Ph.LogbookFill, TreeFortTokens.Palette.WarningYellow),
    TabSpec("Settings", Ph.Gear, Ph.GearFill, TreeFortTokens.Palette.RetroGrape),
)
private const val SETTINGS_TAB = 3

@Composable
private fun MainScaffold() {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var subName by rememberSaveable { mutableStateOf<String?>(null) }
    var joining by remember { mutableStateOf(false) }
    var switching by remember { mutableStateOf(false) }
    val sub = subName?.let { runCatching { Sub.valueOf(it) }.getOrNull() }

    BackHandler(enabled = sub != null) { subName = null }
    // Guardian Lock closes again when the app is backgrounded or Settings is left.
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) { GuardianLock.relock() }
    LaunchedEffect(tab, sub) { if (tab != SETTINGS_TAB && (sub == null || sub == Sub.Help)) GuardianLock.relock() }

    val locked = GuardianLock.isSet(ctx) && !GuardianLock.unlocked

    @Composable
    fun Gated(content: @Composable () -> Unit) {
        if (locked) UnlockPrompt(onForgot = { GuardianLock.clear(ctx); AppStore.signOut() }) else content()
    }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val wide = maxWidth >= 600.dp
        val content: @Composable () -> Unit = {
            Column(Modifier.fillMaxSize()) {
                TopBar(
                    title = if (sub != null) subTitle(sub) else null,
                    onBack = if (sub != null) ({ subName = null }) else null,
                    onHelp = { subName = Sub.Help.name },
                    onSwitch = { switching = true },
                )
                MessageBanner()
                Box(Modifier.weight(1f).fillMaxWidth()) {
                    when (sub) {
                        Sub.Help -> HelpScreen()
                        Sub.Trips -> Gated { TripsScreen() }
                        Sub.Members -> Gated { MembersScreen() }
                        Sub.Campfire -> CampfireScreen()
                        null -> when (tab) {
                            0 -> TreeFortTab(onOpenTrips = { subName = Sub.Trips.name }, onOpenCampfire = { subName = Sub.Campfire.name }, onOpenJoin = { joining = true })
                            1 -> FriendsTab()
                            2 -> LogbookTab()
                            else -> Gated { SettingsTab(onOpen = { subName = it.name }) }
                        }
                    }
                }
                if (!wide) BottomNav(tab) { tab = it; subName = null }
            }
        }
        if (wide) {
            Row(Modifier.fillMaxSize()) {
                SideRail(tab) { tab = it; subName = null }
                Box(Modifier.weight(1f)) { content() }
            }
        } else content()
    }

    if (joining) JoinDialog { joining = false }
    if (switching) HouseholdSwitcher { switching = false }
}

private fun subTitle(s: Sub) = when (s) {
    Sub.Help -> "Help"
    Sub.Trips -> "Vacation mode"
    Sub.Members -> "Guardians and callers"
    Sub.Campfire -> "Campfire"
}

@Composable
private fun TopBar(title: String?, onBack: (() -> Unit)?, onHelp: () -> Unit, onSwitch: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = S.Lg, vertical = S.Sm),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(S.Md),
    ) {
        if (onBack != null) TactileButton("Back", onBack, backgroundColor = CardBg, icon = Ph.Back)
        Text(title ?: "TreeFort", style = if (title == null) Tf.brand else Tf.title, color = Ink, modifier = Modifier.weight(1f), maxLines = 1)
        if (title == null && AppStore.households.size > 1) {
            TactilePill(AppStore.selected?.name ?: "Household", onClick = onSwitch, icon = Ph.Home)
        }
        if (title == null) TactilePill("Help", onClick = onHelp, icon = Ph.Help)
    }
}

@Composable
private fun MessageBanner() {
    val m = AppStore.message ?: return
    LaunchedEffect(m.id) { kotlinx.coroutines.delay(7000); if (AppStore.message?.id == m.id) AppStore.dismissMessage() }
    Box(Modifier.padding(horizontal = S.Lg).padding(bottom = S.Sm)) {
        Banner(
            m.text,
            kind = when (m.kind) { MessageKind.Danger -> BannerKind.Danger; MessageKind.Success -> BannerKind.Success; else -> BannerKind.Info },
            action = { TactilePill("Dismiss", onClick = { AppStore.dismissMessage() }) },
        )
    }
}

@Composable
private fun HouseholdSwitcher(onDismiss: () -> Unit) {
    TfFormDialog("Switch household", onDismiss) {
        AppStore.households.forEach { h ->
            TactileButton(
                h.name + if (h.id == AppStore.selectedId) " (current)" else "",
                { AppStore.selectHousehold(h.id); onDismiss() },
                Modifier.fillMaxWidth(),
                backgroundColor = if (h.id == AppStore.selectedId) TreeFortTokens.Palette.SafeGreen else CardBg,
            )
        }
        TactileButton("Close", onDismiss, Modifier.fillMaxWidth(), backgroundColor = com.treefort.guardian.theme.PillBg)
    }
}

@Composable
private fun TabButton(spec: TabSpec, selected: Boolean, onClick: () -> Unit, modifier: Modifier) {
    val shape = RoundedCornerShape(TreeFortTokens.Radius.Md)
    Column(
        modifier
            .background(if (selected) spec.fill else Color.Transparent, shape)
            .then(if (selected) Modifier.border(TreeFortTokens.Border.Std, BorderInk, shape) else Modifier)
            .clickable(role = Role.Tab, onClick = onClick)
            .semantics { contentDescription = spec.label + if (selected) ", selected" else "" }
            .padding(vertical = S.Sm, horizontal = S.Xs),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(if (selected) spec.activeIcon else spec.icon, null, tint = if (selected) com.treefort.guardian.ui.onColorFor(spec.fill) else Ink, modifier = Modifier.width(24.dp))
        Text(upper(spec.label), style = Tf.labelSm, color = if (selected) com.treefort.guardian.ui.onColorFor(spec.fill) else MutedText, maxLines = 1)
    }
}

@Composable
private fun BottomNav(selected: Int, onSelect: (Int) -> Unit) {
    Row(
        Modifier.fillMaxWidth().background(CardBg).border(TreeFortTokens.Border.Std, BorderInk).navigationBarsPadding().padding(S.Sm),
        horizontalArrangement = Arrangement.spacedBy(S.Xs),
    ) {
        TABS.forEachIndexed { i, t -> TabButton(t, i == selected, { onSelect(i) }, Modifier.weight(1f)) }
    }
}

@Composable
private fun SideRail(selected: Int, onSelect: (Int) -> Unit) {
    Column(
        Modifier.fillMaxHeight().width(96.dp).background(CardBg).border(TreeFortTokens.Border.Std, BorderInk).statusBarsPadding().navigationBarsPadding().padding(S.Sm),
        verticalArrangement = Arrangement.spacedBy(S.Sm),
    ) {
        TABS.forEachIndexed { i, t -> TabButton(t, i == selected, { onSelect(i) }, Modifier.fillMaxWidth()) }
    }
}
