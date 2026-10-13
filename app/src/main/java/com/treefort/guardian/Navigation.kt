package com.treefort.guardian

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.core.content.edit
import androidx.core.graphics.toColorInt
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.treefort.guardian.data.Contact
import com.treefort.guardian.theme.CanvasBg
import com.treefort.guardian.theme.CardBg
import com.treefort.guardian.theme.ForestGreen
import com.treefort.guardian.theme.GuardianBlue
import com.treefort.guardian.theme.Ink
import com.treefort.guardian.theme.MutedText
import com.treefort.guardian.theme.PillBg
import com.treefort.guardian.theme.SunburstAmber
import com.treefort.guardian.theme.TerracottaCoral
import com.treefort.guardian.theme.TreeFortGreen
import com.treefort.guardian.ui.IconActivity
import com.treefort.guardian.ui.IconCampfire
import com.treefort.guardian.ui.IconPhone
import com.treefort.guardian.ui.IconSafelist
import com.treefort.guardian.ui.IconSettings
import com.treefort.guardian.ui.IconStation
import com.treefort.guardian.ui.TactileButton
import com.treefort.guardian.ui.TactileCard
import com.treefort.guardian.ui.TactilePill
import kotlinx.coroutines.launch

data class TabInfo(
    val title: String,
    val unpressedColor: Color,
    val pressedColor: Color,
    val unpressedTextColor: Color,
    val pressedTextColor: Color
)

@Composable
fun MainNavigation() {
    var selectedItem by remember { mutableIntStateOf(0) }

    val tabs = listOf(
        TabInfo(
            title = "TreeFort",
            unpressedColor = com.treefort.guardian.theme.ColorSafeGreen,
            pressedColor = com.treefort.guardian.theme.ColorGreenForest,
            unpressedTextColor = Ink,
            pressedTextColor = Color.White
        ),
        TabInfo(
            title = "Friends",
            unpressedColor = com.treefort.guardian.theme.ColorPrimaryBlue,
            pressedColor = com.treefort.guardian.theme.ColorBlueDeep,
            unpressedTextColor = Color.White,
            pressedTextColor = Color.White
        ),
        TabInfo(
            title = "Logbook",
            unpressedColor = com.treefort.guardian.theme.ColorWarningYellow,
            pressedColor = com.treefort.guardian.theme.ColorYellowAmber,
            unpressedTextColor = Ink,
            pressedTextColor = Ink
        ),
        TabInfo(
            title = "Settings",
            unpressedColor = com.treefort.guardian.theme.ColorDangerRed,
            pressedColor = com.treefort.guardian.theme.ColorRedCrimson,
            unpressedTextColor = Color.White,
            pressedTextColor = Color.White
        )
    )

    var showHelpPage by remember { mutableStateOf(false) }

    val callState by WebRTCManager.callState.collectAsState()

    BackHandler(enabled = showHelpPage) {
        showHelpPage = false
    }

    if (!AndroidProfileManager.isLoggedIn) {
        GuardianSignInScreen(onSignIn = { AndroidProfileManager.isLoggedIn = true })
    } else if (showHelpPage) {
        HelpScreen(onBack = { showHelpPage = false })
    } else {
        Box(modifier = Modifier.fillMaxSize()) {
            BoxWithConstraints(modifier = Modifier.fillMaxSize().background(CanvasBg)) {
            val isTabletOrLandscape = maxWidth >= 600.dp

            if (isTabletOrLandscape) {
                // Dual-pane / Tablet & Landscape Navigation Rail Layout
                Row(modifier = Modifier.fillMaxSize()) {
                    // Left Side Rail
                    Column(
                        modifier = Modifier
                            .width(220.dp)
                            .fillMaxHeight()
                            .background(CardBg)
                            .border(width = 2.dp, color = Ink)
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(TreeFortGreen, RoundedCornerShape(8.dp))
                                    .border(2.dp, Ink, RoundedCornerShape(8.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("🌲", fontSize = 18.sp)
                            }
                            Text("TreeFort", fontSize = 18.sp, fontWeight = FontWeight.Black, color = Ink)
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        tabs.forEachIndexed { index, tab ->
                            val isSelected = selectedItem == index
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        if (isSelected) tab.unpressedColor else CanvasBg,
                                        RoundedCornerShape(10.dp)
                                    )
                                    .border(2.dp, Ink, RoundedCornerShape(10.dp))
                                    .clickable { selectedItem = index }
                                    .padding(vertical = 12.dp, horizontal = 14.dp)
                            ) {
                                Text(
                                    text = tab.title,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Black,
                                    color = if (isSelected) tab.unpressedTextColor else Ink
                                )
                            }
                        }

                        Spacer(modifier = Modifier.weight(1f))

                        TactileButton(
                            text = "❓ Family Help",
                            backgroundColor = PillBg,
                            textColor = Ink,
                            onClick = { showHelpPage = true },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    // Right Main Content Pane
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .background(CanvasBg)
                    ) {
                        when (selectedItem) {
                            0 -> StationScreen()
                            1 -> SafelistScreen()
                            2 -> ActivityScreen()
                            3 -> SettingsScreen()
                        }
                    }
                }
            } else {
                // Mobile Portrait Layout with Bottom Navigation Bar
                Scaffold(
                    containerColor = CanvasBg,
                    topBar = {
                        TopAppBar(
                            title = tabs[selectedItem].title,
                            onHelpClick = {
                                showHelpPage = true
                            }
                        )
                    },
                    bottomBar = {
                        CustomBottomNav(
                            tabs = tabs,
                            selectedIndex = selectedItem,
                            onSelectTab = { selectedItem = it }
                        )
                    }
                ) { innerPadding ->
                    var totalDragX by remember { mutableFloatStateOf(0f) }
                    Box(
                        modifier = Modifier
                            .padding(innerPadding)
                            .fillMaxSize()
                            .background(CanvasBg)
                            .pointerInput(selectedItem) {
                                detectHorizontalDragGestures(
                                    onDragEnd = {
                                        if (totalDragX < -60f) {
                                            selectedItem = (selectedItem + 1) % tabs.size
                                        } else if (totalDragX > 60f) {
                                            selectedItem = (selectedItem - 1 + tabs.size) % tabs.size
                                        }
                                        totalDragX = 0f
                                    },
                                    onDragCancel = { totalDragX = 0f },
                                    onHorizontalDrag = { _, dragAmount ->
                                        totalDragX += dragAmount
                                    }
                                )
                            }
                    ) {
                        when (selectedItem) {
                            0 -> StationScreen()
                            1 -> SafelistScreen()
                            2 -> ActivityScreen()
                            3 -> SettingsScreen()
                        }
                    }
                }
            }

            if (callState !is CallState.Idle) {
                ActiveCallOverlay(callState = callState)
            }
        }
    }
}
}

@Composable
fun ActiveCallOverlay(callState: CallState) {
    val context = LocalContext.current
    val isMuted by WebRTCManager.isMuted.collectAsState()
    val isSpeakerOn by WebRTCManager.isSpeakerOn.collectAsState()
    val durationSeconds by WebRTCManager.callDurationSeconds.collectAsState()

    val displayName = when (callState) {
        is CallState.Incoming -> callState.callerName
        else -> WebRTCManager.activePeerName
    }
    val dialCode = when (callState) {
        is CallState.Incoming -> callState.callerDialCode
        is CallState.Connected -> callState.peerDialCode.ifBlank { WebRTCManager.activePeerDialCode }
        else -> WebRTCManager.activePeerDialCode
    }
    val displayEmoji = WebRTCManager.activePeerEmoji
    val displayBg = parseHexColor(WebRTCManager.activePeerBg)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CardBg)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(24.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Spacer instead of top badges
            Spacer(modifier = Modifier.height(8.dp))

            // Center: Contact details, big avatar and duration
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(150.dp)
                        .background(displayBg, CircleShape)
                        .border(3.5.dp, Ink, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(displayEmoji, fontSize = 72.sp)
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = displayName,
                        fontSize = 30.sp,
                        fontWeight = FontWeight.Black,
                        color = Ink
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Dial Code: $dialCode",
                        fontSize = 15.sp,
                        color = MutedText,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                when (callState) {
                    is CallState.Connected -> {
                        Box(
                            modifier = Modifier
                                .background(Color(0xFFEFF6FF), RoundedCornerShape(12.dp))
                                .border(2.dp, Ink, RoundedCornerShape(12.dp))
                                .padding(horizontal = 24.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = WebRTCManager.formatDuration(durationSeconds),
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Black,
                                color = GuardianBlue
                            )
                        }
                    }
                    is CallState.Incoming -> {
                        Text(
                            text = "RINGING...",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Black,
                            color = ForestGreen
                        )
                    }
                    else -> Unit
                }
            }

            // Bottom Call Controls: Mute, Speaker, Answer, End
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(22.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(52.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Mute Button
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(66.dp)
                                .background(if (isMuted) Color(0xFFFEE2E2) else CanvasBg, CircleShape)
                                .border(2.5.dp, Ink, CircleShape)
                                .clickable { WebRTCManager.toggleMute() },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(if (isMuted) "🔇" else "🎙️", fontSize = 28.sp)
                        }
                        Text(
                            text = if (isMuted) "MUTED" else "MUTE",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            color = Ink
                        )
                    }

                    // Speaker Button
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(66.dp)
                                .background(if (isSpeakerOn) Color(0xFFDBEAFE) else CanvasBg, CircleShape)
                                .border(2.5.dp, Ink, CircleShape)
                                .clickable { WebRTCManager.toggleSpeaker(context) },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(if (isSpeakerOn) "📢" else "🔈", fontSize = 28.sp)
                        }
                        Text(
                            text = if (isSpeakerOn) "SPEAKER ON" else "SPEAKER",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            color = Ink
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    if (callState is CallState.Incoming) {
                        TactileButton(
                            text = "ANSWER CALL",
                            backgroundColor = ForestGreen,
                            textColor = Color.White,
                            onClick = { WebRTCManager.acceptCall() },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    TactileButton(
                        text = "END CALL",
                        backgroundColor = Color(0xFFEF4444),
                        textColor = Color.White,
                        onClick = { WebRTCManager.hangup() },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
fun GuardianSignInScreen(onSignIn: () -> Unit) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isPasswordMode by remember { mutableStateOf(true) }
    var showPassword by remember { mutableStateOf(false) }
    var otpCode by remember { mutableStateOf("") }
    var isOtpSent by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CanvasBg)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(24.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .background(SunburstAmber, CircleShape)
                    .border(3.dp, Ink, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text("🛡️", fontSize = 38.sp)
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "TreeFort Guardian",
                fontSize = 26.sp,
                fontWeight = FontWeight.Black,
                color = Ink
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Sign in with your verified guardian credentials to manage household phone stations.",
                fontSize = 13.sp,
                color = MutedText,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.padding(horizontal = 20.dp)
            )

            Spacer(modifier = Modifier.height(28.dp))

            TactileCard(
                modifier = Modifier.fillMaxWidth(),
                backgroundColor = CardBg,
                shadowOffset = 4.dp,
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "GUARDIAN AUTHENTICATION",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        color = MutedText
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Guardian Email Address", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Ink)
                        OutlinedTextField(
                            value = email,
                            onValueChange = { email = it },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Ink,
                                unfocusedBorderColor = Ink,
                                focusedContainerColor = CanvasBg,
                                unfocusedContainerColor = CanvasBg
                            ),
                            shape = RoundedCornerShape(8.dp)
                        )
                    }

                    if (isPasswordMode) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("Guardian Password", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Ink)
                            OutlinedTextField(
                                value = password,
                                onValueChange = { password = it },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                                trailingIcon = {
                                    Text(
                                        text = if (showPassword) "👁️" else "🙈",
                                        modifier = Modifier
                                            .clickable { showPassword = !showPassword }
                                            .padding(8.dp),
                                        fontSize = 16.sp
                                    )
                                },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Ink,
                                    unfocusedBorderColor = Ink,
                                    focusedContainerColor = CanvasBg,
                                    unfocusedContainerColor = CanvasBg
                                ),
                                shape = RoundedCornerShape(8.dp)
                            )
                        }

                        TactileButton(
                            text = "SIGN IN AS GUARDIAN",
                            backgroundColor = ForestGreen,
                            textColor = Color.White,
                            onClick = {
                                onSignIn()
                                coroutineScope.launch {
                                    try {
                                        SupabaseManager.signIn(email.trim(), password.trim())
                                    } catch (_: Exception) {}
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        )

                        Text(
                            text = "Or sign in with 6-digit email code",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = GuardianBlue,
                            modifier = Modifier
                                .align(Alignment.CenterHorizontally)
                                .clickable { isPasswordMode = false }
                                .padding(vertical = 4.dp)
                        )
                    } else {
                        if (isOtpSent) {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("6-Digit Verification Code", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Ink)
                                OutlinedTextField(
                                    value = otpCode,
                                    onValueChange = { otpCode = it },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true,
                                    placeholder = { Text("123456") },
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = Ink,
                                        unfocusedBorderColor = Ink,
                                        focusedContainerColor = CanvasBg,
                                        unfocusedContainerColor = CanvasBg
                                    ),
                                    shape = RoundedCornerShape(8.dp)
                                )
                            }
                        }

                        TactileButton(
                            text = if (isOtpSent) "VERIFY & ENTER PORTAL" else "SEND 6-DIGIT CODE",
                            backgroundColor = ForestGreen,
                            textColor = Color.White,
                            onClick = {
                                if (!isOtpSent) {
                                    isOtpSent = true
                                } else {
                                    onSignIn()
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        )

                        Text(
                            text = "Or sign in with password",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = GuardianBlue,
                            modifier = Modifier
                                .align(Alignment.CenterHorizontally)
                                .clickable { isPasswordMode = true }
                                .padding(vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun HelpScreen(onBack: () -> Unit) {
    val scrollState = rememberScrollState()
    var selectedCategory by remember { mutableStateOf("All") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CanvasBg)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // Top App Bar for Full Help Page
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TactileButton(
                text = "← BACK",
                onClick = onBack,
                backgroundColor = CardBg,
                textColor = Ink,
                shadowOffset = 2.dp,
                shape = RoundedCornerShape(8.dp),
                fontSize = 11.5.sp
            )

            Text(
                text = "Help & Family Guides",
                fontSize = 19.sp,
                fontWeight = FontWeight.Black,
                color = Ink,
                letterSpacing = (-0.5).sp
            )

            Spacer(modifier = Modifier.width(40.dp))
        }
        
        var searchQuery by remember { mutableStateOf("") }
        
        // Search Bar for Help Guides
        Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search guides, shortcuts, questions...", color = MutedText, fontSize = 13.5.sp) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(10.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Ink,
                    unfocusedBorderColor = Ink,
                    focusedContainerColor = CardBg,
                    unfocusedContainerColor = CardBg
                )
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Category Filter Pills
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("All", "Quick Dialing", "Wi-Fi & Setup", "Privacy & Safety", "Curfews & Audio").forEach { category ->
                    val isSelected = selectedCategory == category
                    TactilePill(
                        text = category,
                        backgroundColor = if (isSelected) TreeFortGreen else CardBg,
                        textColor = Ink,
                        shadowOffset = if (isSelected) 2.dp else 1.dp,
                        onClick = { selectedCategory = category }
                    )
                }
            }

            // Section 1: Household Keypad Dialing Shortcuts
            if (selectedCategory in listOf("All", "Quick Dialing")) {
                TactileCard(
                    modifier = Modifier.fillMaxWidth(),
                    backgroundColor = CardBg,
                    shadowOffset = 4.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "☎️ KEYPAD DIALING SHORTCUTS",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Black,
                                color = Ink
                            )
                            TactilePill(
                                text = "PUSH BUTTON",
                                backgroundColor = PillBg,
                                textColor = Ink,
                                shadowOffset = 1.dp
                            )
                        }

                        DialCodeRow(
                            code = "*NNNNN",
                            badgeColor = GuardianBlue,
                            title = "Free TreeFort VoIP Calling",
                            description = "Every base station has a 5-digit number (like *14185). Calls connect over encrypted peer-to-peer audio, completely free and unmetered."
                        )

                        DialCodeRow(
                            code = "#0",
                            badgeColor = SunburstAmber,
                            title = "The Campfire Family Room",
                            description = "Rings all household TreeFort stations simultaneously. Perfect for calling kids down to dinner, family announcements, or all-hands campfire chat."
                        )

                        DialCodeRow(
                            code = "#1 – #9",
                            badgeColor = TreeFortGreen,
                            title = "One-Touch Speed Dials",
                            description = "Press any slot #1 through #9 on the tactile keypad to instantly ring Mom, Dad, Grandma, or best friends without memorizing numbers."
                        )

                        DialCodeRow(
                            code = "911",
                            badgeColor = TerracottaCoral,
                            title = "FCC/CRTC Compliant E911",
                            description = "Direct emergency dispatch routing to your verified household address, with automatic offline voice guidance fallback if internet is disconnected."
                        )
                    }
                }
            }

            // Section 2: Privacy Promise & Safety
            if (selectedCategory in listOf("All", "Privacy & Safety")) {
                TactileCard(
                    modifier = Modifier.fillMaxWidth(),
                    backgroundColor = PillBg,
                    shadowOffset = 4.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "🛡️ OUR STRICT PRIVACY PROMISE",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Black,
                                color = ForestGreen
                            )
                            TactilePill(
                                text = "ZERO AI",
                                backgroundColor = TreeFortGreen,
                                textColor = Ink,
                                shadowOffset = 1.dp
                            )
                        }

                        Text(
                            text = "TreeFort does NOT use Artificial Intelligence, speech recognition, machine learning, or cloud recording. Conversations are never analyzed, transcribed, or stored.",
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Medium,
                            color = Ink
                        )

                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            HelpCheckItem("Zero Telemarketers or Spam: Only guardian-approved numbers can ever ring the phone.")
                            HelpCheckItem("End-to-End Encryption: Audio travels securely encrypted directly from phone to phone.")
                            HelpCheckItem("Ephemeral Live Audio: Voice disappears the instant it is heard—just like a classic landline.")
                            HelpCheckItem("No Advertising or Profiles: We never track kid behaviors or sell family data to third parties.")
                        }
                    }
                }
            }

            // Section 3: Wi-Fi Setup & Offline Reliability
            if (selectedCategory in listOf("All", "Wi-Fi & Setup")) {
                TactileCard(
                    modifier = Modifier.fillMaxWidth(),
                    backgroundColor = CardBg,
                    shadowOffset = 4.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "📶 WI-FI & OFFLINE RELIABILITY",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Black,
                            color = Ink
                        )

                        HelpStepItem(
                            stepNumber = "1",
                            title = "Connect to Hotspot",
                            desc = "Hold '#' on the keypad for 10 seconds (or power on). Connect your smartphone to the 'TreeFort-Setup' Wi-Fi network."
                        )
                        HelpStepItem(
                            stepNumber = "2",
                            title = "Open Setup Portal",
                            desc = "Visit http://treefort.setup or 192.168.4.1 in your browser to choose your 2.4GHz home Wi-Fi and enter your password."
                        )
                        HelpStepItem(
                            stepNumber = "3",
                            title = "Offline Memory Safeguard",
                            desc = "If home internet goes down, contacts, curfews, and speed dials remain safely preserved in local flash memory. The station auto-reconnects when Wi-Fi returns."
                        )
                    }
                }
            }

            // Section 4: Curfew & Quiet Hours
            if (selectedCategory in listOf("All", "Curfews & Audio")) {
                TactileCard(
                    modifier = Modifier.fillMaxWidth(),
                    backgroundColor = CardBg,
                    shadowOffset = 4.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "🌙 BEDTIME CURFEWS & QUIET HOURS",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Black,
                            color = Ink
                        )

                        Text(
                            text = "Curfews automatically silence the physical ringer speaker during bedtime hours (e.g. 8:30 PM to 7:00 AM) so children sleep soundly without nighttime disruptions.",
                            fontSize = 12.5.sp,
                            color = Ink
                        )

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFFEF3C7), RoundedCornerShape(8.dp))
                                .border(1.dp, Ink, RoundedCornerShape(8.dp))
                                .padding(10.dp)
                        ) {
                            Text(
                                text = "⚠️ Safety Exception: Outgoing 911 emergency dispatch is NEVER disabled or silenced by curfews.",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF92400E)
                            )
                        }
                    }
                }
            }

            // Section 5: Anti-Kid Code Lock Guide
            if (selectedCategory in listOf("All", "Privacy & Safety")) {
                TactileCard(
                    modifier = Modifier.fillMaxWidth(),
                    backgroundColor = CardBg,
                    shadowOffset = 4.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("🔒 ANTI-KID GUARDIAN LOCK", fontSize = 13.sp, fontWeight = FontWeight.Black, color = Ink)
                            TactilePill(text = "CHILDPROOF", backgroundColor = com.treefort.guardian.theme.ColorWarningYellow, textColor = Ink, shadowOffset = 1.dp)
                        }
                        Text(
                            text = "Enable the Anti-Kid PIN Code in Settings to protect contacts, curfews, and speed dials. Kids can view friends, but need your 4-digit code to make changes.",
                            fontSize = 12.5.sp,
                            color = Ink
                        )
                    }
                }
            }

            // Section 6: Interactive Station Diagnostics (Stoplights)
            TactileCard(
                modifier = Modifier.fillMaxWidth(),
                backgroundColor = CardBg,
                shadowOffset = 4.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "📻 PHONE SYSTEM HEALTH & STATUS",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Black,
                            color = Ink
                        )
                        TactilePill(
                            text = "ALL GOOD",
                            backgroundColor = com.treefort.guardian.theme.ColorGreenMint,
                            textColor = ForestGreen,
                            shadowOffset = 1.dp
                        )
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(PillBg, RoundedCornerShape(8.dp))
                                .border(1.dp, Ink, RoundedCornerShape(8.dp))
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Wi-Fi Access", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Ink)
                            TactilePill(text = "CONNECTED", backgroundColor = com.treefort.guardian.theme.ColorBluePowder, textColor = com.treefort.guardian.theme.ColorBlueDeep, shadowOffset = 1.dp)
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(PillBg, RoundedCornerShape(8.dp))
                                .border(1.dp, Ink, RoundedCornerShape(8.dp))
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Network Access", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Ink)
                            TactilePill(text = "ONLINE (WEBRTC)", backgroundColor = com.treefort.guardian.theme.ColorGreenMint, textColor = ForestGreen, shadowOffset = 1.dp)
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(PillBg, RoundedCornerShape(8.dp))
                                .border(1.dp, Ink, RoundedCornerShape(8.dp))
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("System Status", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Ink)
                            TactilePill(text = "ACTIVE & READY", backgroundColor = com.treefort.guardian.theme.ColorGreenMint, textColor = ForestGreen, shadowOffset = 1.dp)
                        }
                    }
                }
            }

            // Section 7: E911 Emergency Dispatch & Vacation Mode (From Web FAQ)
            if (selectedCategory in listOf("All", "Privacy & Safety")) {
                TactileCard(
                    modifier = Modifier.fillMaxWidth(),
                    backgroundColor = CardBg,
                    shadowOffset = 4.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("🚨 E911 DISPATCH & VACATION REVERSION", fontSize = 13.sp, fontWeight = FontWeight.Black, color = Ink)
                            TactilePill(text = "FCC/CRTC COMPLIANT", backgroundColor = com.treefort.guardian.theme.ColorGreenMint, textColor = ForestGreen, shadowOffset = 1.dp)
                        }

                        Text(
                            text = "TreeFort is certified for residential emergency dispatch. Dialing 911 immediately routes to your local emergency PSAP with verified street coordinates.",
                            fontSize = 12.5.sp,
                            color = Ink
                        )

                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            HelpCheckmarkRow("Always Bypasses Curfews: 911 is NEVER silenced or blocked by bedtime schedules.")
                            HelpCheckmarkRow("Vacation Mode: Visiting Grandma or a cabin? Update your temporary address in seconds.")
                            HelpCheckmarkRow("Auto-Revert Safety: Set an auto-return date so your home address automatically restores.")
                        }
                    }
                }
            }

            // Section 8: Co-Parenting & Multi-Guardian Protection (From Web FAQ)
            if (selectedCategory in listOf("All", "Privacy & Safety")) {
                TactileCard(
                    modifier = Modifier.fillMaxWidth(),
                    backgroundColor = CardBg,
                    shadowOffset = 4.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("🤝 CO-PARENTING & MULTI-GUARDIAN", fontSize = 13.sp, fontWeight = FontWeight.Black, color = Ink)
                            TactilePill(text = "ROLE SECURITY", backgroundColor = com.treefort.guardian.theme.ColorBluePowder, textColor = GuardianBlue, shadowOffset = 1.dp)
                        }

                        Text(
                            text = "TreeFort supports 2 homes and up to 4 verified guardians (Mom, Dad, Stepmom, Stepdad) with shared safelists and independent station curfews.",
                            fontSize = 12.5.sp,
                            color = Ink
                        )

                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            HelpCheckmarkRow("Shared Whitelist: Both parents receive instant alerts and can approve friends.")
                            HelpCheckmarkRow("Station Access: Set bedtime quiet hours and volume per home station independently.")
                            HelpCheckmarkRow("Independent 2FA: No shared passwords. Each guardian logs in with their own account.")
                        }
                    }
                }
            }

            // Section 9: Free Global Calling (From Web FAQ)
            if (selectedCategory in listOf("All", "Keypad Dialing")) {
                TactileCard(
                    modifier = Modifier.fillMaxWidth(),
                    backgroundColor = CardBg,
                    shadowOffset = 4.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("🌍 FREE GLOBAL CALLING (NO LONG DISTANCE)", fontSize = 13.sp, fontWeight = FontWeight.Black, color = Ink)
                            TactilePill(text = "100% FREE VOIP", backgroundColor = com.treefort.guardian.theme.ColorSafeGreen, textColor = Ink, shadowOffset = 1.dp)
                        }

                        Text(
                            text = "TreeFort calls run over encrypted WebRTC VoIP anywhere there is Wi-Fi, completely bypassing cellular carriers and toll surcharges.",
                            fontSize = 12.5.sp,
                            color = Ink
                        )

                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            HelpCheckmarkRow("Free Cross-Border Calling: Ring cousins or friends in Canada, UK, Mexico, or anywhere on Earth.")
                            HelpCheckmarkRow("Free Guardian Mobile App: Relatives abroad can call your home phone directly via this free app.")
                        }
                    }
                }
            }

            // Section 10: 3D Printing & Maker Enclosures (From Web FAQ)
            if (selectedCategory in listOf("All", "Wi-Fi & Pairing")) {
                TactileCard(
                    modifier = Modifier.fillMaxWidth(),
                    backgroundColor = CardBg,
                    shadowOffset = 4.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("🖨️ 3D PRINTING & MAKER ENCLOSURES", fontSize = 13.sp, fontWeight = FontWeight.Black, color = Ink)
                            TactilePill(text = "FREE STL FILES", backgroundColor = SunburstAmber, textColor = Ink, shadowOffset = 1.dp)
                        }

                        Text(
                            text = "TreeFort is maker-friendly! Download 100% free STL and STEP files on Printables and GitHub to customize your station in any filament color.",
                            fontSize = 12.5.sp,
                            color = Ink
                        )

                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            HelpCheckmarkRow("Fits Standard Beds: Engineered for 180×180mm printer beds with zero supports.")
                            HelpCheckmarkRow("Infinite Colors: Atomic purple, rainbow PLA, glow-in-the-dark, or wood filament.")
                        }
                    }
                }
            }

            // Footer Support Contact Card
            TactileCard(
                modifier = Modifier.fillMaxWidth(),
                backgroundColor = PillBg,
                shadowOffset = 3.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "Need personal guardian assistance?",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Ink
                    )
                    Text(
                        text = "Email our friendly family support team at support@treefortphone.com",
                        fontSize = 11.5.sp,
                        color = MutedText,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
private fun DialCodeRow(code: String, badgeColor: Color, title: String, description: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .background(badgeColor, RoundedCornerShape(8.dp))
                .border(1.5.dp, Ink, RoundedCornerShape(8.dp))
                .padding(horizontal = 8.dp, vertical = 5.dp)
        ) {
            Text(
                text = code,
                fontWeight = FontWeight.Black,
                fontSize = 11.5.sp,
                color = if (badgeColor == GuardianBlue || badgeColor == TerracottaCoral) Color.White else Ink
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Ink)
            Spacer(modifier = Modifier.height(2.dp))
            Text(description, fontSize = 11.5.sp, color = MutedText, lineHeight = 16.sp)
        }
    }
}

@Composable
private fun HelpCheckItem(text: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text("✓", fontWeight = FontWeight.Black, fontSize = 12.sp, color = ForestGreen)
        Text(text, fontSize = 12.sp, color = Ink, lineHeight = 16.sp)
    }
}

@Composable
private fun HelpCheckmarkRow(text: String) {
    HelpCheckItem(text)
}

@Composable
private fun HelpStepItem(stepNumber: String, title: String, desc: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .background(SunburstAmber, CircleShape)
                .border(1.5.dp, Ink, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(stepNumber, fontWeight = FontWeight.Black, fontSize = 12.sp, color = Ink)
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Ink)
            Spacer(modifier = Modifier.height(2.dp))
            Text(desc, fontSize = 11.5.sp, color = MutedText, lineHeight = 16.sp)
        }
    }
}

@Composable
fun TopAppBar(
    title: String,
    onHelpClick: () -> Unit
) {
    val isOnline by WebRTCManager.isOnline.collectAsState()
    var showStatusDialog by remember { mutableStateOf(false) }

    if (showStatusDialog) {
        Dialog(onDismissRequest = { showStatusDialog = false }) {
            TactileCard(
                modifier = Modifier.fillMaxWidth(0.92f),
                backgroundColor = CardBg,
                shadowOffset = 4.dp,
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = if (isOnline) "🟢 Signaling Online" else "🟡 Signaling Connecting",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Black,
                        color = Ink
                    )
                    Text(
                        text = if (isOnline)
                            "Your phone is connected to the TreeFort local signaling server at ${WebRTCManager.signalingHost}.\n\nEncrypted WebRTC VoIP calling is ready between your phone and base stations."
                        else
                            "Connecting to the TreeFort signaling server at ${WebRTCManager.signalingHost}...\n\nEnsure this phone and the base station share the same Wi-Fi network.",
                        fontSize = 13.sp,
                        color = Ink,
                        lineHeight = 18.sp
                    )
                    TactileButton(
                        text = "DISMISS",
                        backgroundColor = PillBg,
                        textColor = Ink,
                        onClick = { showStatusDialog = false },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(CanvasBg)
            .statusBarsPadding()
            .border(width = 2.dp, color = Ink)
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Circle Patch Logo Placeholder + App Name
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .background(com.treefort.guardian.theme.ColorWarningYellow, CircleShape)
                        .border(2.dp, Ink, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    IconPhone(modifier = Modifier.size(18.dp), color = Ink)
                }

                Text(
                    text = title.ifBlank { "TreeFort" },
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    color = Ink,
                    letterSpacing = (-0.5).sp
                )
            }

            // Right side: Status Badge + Tactile Help Button
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TactilePill(
                    text = if (isOnline) "🟢 ONLINE" else "🟡 CONNECTING",
                    onClick = { showStatusDialog = true },
                    backgroundColor = if (isOnline) Color(0xFFDCFCE7) else Color(0xFFFEF9C3),
                    textColor = if (isOnline) Color(0xFF15803D) else Color(0xFF854D0E),
                    shadowOffset = 1.dp
                )

                TactilePill(
                    text = "HELP",
                    icon = "❓",
                    onClick = onHelpClick,
                    backgroundColor = CardBg,
                    textColor = Ink,
                    shadowOffset = 2.dp,
                    shape = RoundedCornerShape(8.dp)
                )
            }
        }
    }
}

@Composable
fun CustomBottomNav(
    tabs: List<TabInfo>,
    selectedIndex: Int,
    onSelectTab: (Int) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(CardBg)
            .border(
                width = 2.5.dp,
                color = Ink
            )
            .navigationBarsPadding()
            .padding(horizontal = 10.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            tabs.forEachIndexed { index, tab ->
                val isSelected = selectedIndex == index
                val interactionSource = remember { MutableInteractionSource() }
                val isPressed by interactionSource.collectIsPressedAsState()
                val effectivelyPressed = isSelected || isPressed
                val shape = RoundedCornerShape(12.dp)
                val shadowOffset = 3.dp

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(62.dp)
                        .padding(end = 2.dp, bottom = 2.dp)
                ) {
                    // Pop-up Shadow Socket behind button when unpressed
                    if (!effectivelyPressed) {
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .offset(x = shadowOffset, y = shadowOffset)
                                .background(Ink, shape)
                        )
                    }

                    // Tactile Square Button
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .offset(
                                x = if (effectivelyPressed) shadowOffset else 0.dp,
                                y = if (effectivelyPressed) shadowOffset else 0.dp
                            )
                            .background(
                                color = if (isSelected) tab.pressedColor else tab.unpressedColor,
                                shape = shape
                            )
                            .border(2.dp, Ink, shape)
                            .clickable(
                                interactionSource = interactionSource,
                                indication = null,
                                onClick = { onSelectTab(index) }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            val contentColor = if (isSelected) tab.pressedTextColor else tab.unpressedTextColor
                            RenderTabIcon(index, color = contentColor)
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = tab.title.uppercase(),
                                fontWeight = FontWeight.Black,
                                fontSize = 9.5.sp,
                                color = contentColor,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RenderTabIcon(index: Int, color: Color) {
    val sizeMod = Modifier.size(19.dp)
    when (index) {
        0 -> IconStation(modifier = sizeMod, color = color)
        1 -> IconSafelist(modifier = sizeMod, color = color)
        2 -> IconActivity(modifier = sizeMod, color = color)
        3 -> IconSettings(modifier = sizeMod, color = color)
    }
}

@Composable
fun StationScreen() {
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    val callState by WebRTCManager.callState.collectAsState()
    val isOnline by WebRTCManager.isOnline.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Active Call / Incoming Call Banner
        when (val state = callState) {
            is CallState.Incoming -> {
                TactileCard(
                    modifier = Modifier.fillMaxWidth(),
                    backgroundColor = Color(0xFFFEF3C7),
                    shadowOffset = 4.dp
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("📞 INCOMING CALL", fontWeight = FontWeight.Black, fontSize = 13.sp, color = TerracottaCoral)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(state.callerName, fontWeight = FontWeight.Black, fontSize = 18.sp, color = Ink)
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            TactileButton(
                                text = "ANSWER",
                                backgroundColor = TreeFortGreen,
                                textColor = Ink,
                                onClick = { WebRTCManager.acceptCall() },
                                modifier = Modifier.weight(1f)
                            )
                            TactileButton(
                                text = "DECLINE",
                                backgroundColor = TerracottaCoral,
                                textColor = Color.White,
                                onClick = { WebRTCManager.hangup() },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
            is CallState.Calling -> {
                TactileCard(
                    modifier = Modifier.fillMaxWidth(),
                    backgroundColor = Color(0xFFD1F2D9),
                    shadowOffset = 4.dp
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("CALLING...", fontWeight = FontWeight.Black, fontSize = 12.sp, color = ForestGreen)
                            Text("Connecting WebRTC audio...", fontWeight = FontWeight.Medium, fontSize = 13.sp, color = Ink)
                        }
                        TactilePill(
                            text = "HANG UP",
                            backgroundColor = TerracottaCoral,
                            textColor = Color.White,
                            onClick = { WebRTCManager.hangup() },
                            shadowOffset = 2.dp
                        )
                    }
                }
            }
            is CallState.Connected -> {
                TactileCard(
                    modifier = Modifier.fillMaxWidth(),
                    backgroundColor = Color(0xFFD1F2D9),
                    shadowOffset = 4.dp
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("LIVE CALL ACTIVE", fontWeight = FontWeight.Black, fontSize = 12.sp, color = ForestGreen)
                            Text("Peer: ${state.peerDialCode}", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Ink)
                        }
                        TactilePill(
                            text = "END CALL",
                            backgroundColor = TerracottaCoral,
                            textColor = Color.White,
                            onClick = { WebRTCManager.hangup() },
                            shadowOffset = 2.dp
                        )
                    }
                }
            }
            CallState.Idle -> {}
        }
        if (!AndroidProfileManager.activeRole.hasStation) {
            // Extended Family Caller Mode (Uncle Bob)
            TactileCard(
                modifier = Modifier.fillMaxWidth(),
                backgroundColor = com.treefort.guardian.theme.ColorYellowPale,
                shadowOffset = 4.dp
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Extended Family Caller", fontSize = 16.sp, fontWeight = FontWeight.Black, color = Ink)
                            Text("Uncle Bob's Personal TreeFort Line", fontSize = 12.sp, color = MutedText)
                        }
                        TactilePill(
                            text = "CALLER ONLY",
                            backgroundColor = com.treefort.guardian.theme.ColorPrimaryBlue,
                            textColor = Color.White,
                            shadowOffset = 1.dp
                        )
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(CanvasBg, RoundedCornerShape(8.dp))
                            .border(1.dp, Ink, RoundedCornerShape(8.dp))
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("YOUR CALLER CODE", fontSize = 10.sp, fontWeight = FontWeight.Black, color = MutedText)
                            Text("*38912", fontSize = 24.sp, fontWeight = FontWeight.Black, color = com.treefort.guardian.theme.ColorPrimaryBlue)
                        }
                        Text("Kids dial this code from any approved home phone", fontSize = 11.sp, color = MutedText, modifier = Modifier.weight(1f).padding(start = 12.dp))
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(CanvasBg, RoundedCornerShape(8.dp))
                            .border(1.dp, Ink, RoundedCornerShape(8.dp))
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("SECRET PAIRING CODE", fontSize = 10.sp, fontWeight = FontWeight.Black, color = MutedText)
                            Text("519-204", fontSize = 18.sp, fontWeight = FontWeight.Black, color = com.treefort.guardian.theme.ColorRustyOrange)
                        }
                        Text("Share this 6-digit code with parents to whitelist your line", fontSize = 11.sp, color = MutedText, modifier = Modifier.weight(1f).padding(start = 12.dp))
                    }
                }
            }
        } else {
            // Soft Requests Card (Replaces Household Status)
            RequestsCard()

            // Active Device Base Station Card
            TactileCard(
                modifier = Modifier.fillMaxWidth(),
                backgroundColor = CardBg,
                shadowOffset = 4.dp
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Kid's Bedroom",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Black,
                                color = Ink
                            )
                            // Connection Stoplight Dot
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .background(if (isOnline) com.treefort.guardian.theme.ColorSafeGreen else com.treefort.guardian.theme.ColorWarningYellow, CircleShape)
                                    .border(1.5.dp, Ink, CircleShape)
                            )
                        }

                        if (AndroidProfileManager.activeRole.isCoParentDualHome) {
                            Text(
                                text = "🏡 Mom's House · Co-Parent Station",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                color = GuardianBlue
                            )
                        }

                        if (AndroidProfileManager.isTemporaryMoveActive) {
                            Text(
                                text = "🏕️ OVERNIGHT AT ${AndroidProfileManager.tempMoveHostName.uppercase()}",
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Black,
                                color = com.treefort.guardian.theme.ColorDangerRed,
                                modifier = Modifier
                                    .background(com.treefort.guardian.theme.ColorYellowAmber, RoundedCornerShape(4.dp))
                                    .padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                
                // Dual Numbers Display: Station Handset & Mobile Phone App
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Station Handset Number
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .background(CanvasBg, RoundedCornerShape(8.dp))
                                .border(1.dp, Ink, RoundedCornerShape(8.dp))
                                .padding(10.dp)
                        ) {
                            Text("📻 STATION NUMBER", fontSize = 9.5.sp, color = MutedText, fontWeight = FontWeight.Black)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text("*14185", fontSize = 22.sp, fontWeight = FontWeight.Black, color = com.treefort.guardian.theme.ColorPrimaryBlue)
                            Text("Base handset in bedroom", fontSize = 10.sp, color = MutedText, maxLines = 1)
                        }

                        // Phone App TreeFort Number
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .background(CanvasBg, RoundedCornerShape(8.dp))
                                .border(1.dp, Ink, RoundedCornerShape(8.dp))
                                .padding(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("📱 THIS PHONE", fontSize = 9.5.sp, color = MutedText, fontWeight = FontWeight.Black)
                                TactilePill(text = "GUARDIAN", backgroundColor = com.treefort.guardian.theme.ColorSafeGreen, textColor = Ink, shadowOffset = 0.dp)
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(AndroidProfileManager.phoneTreeFortNumber, fontSize = 22.sp, fontWeight = FontWeight.Black, color = ForestGreen)
                            Text("Rings this Android directly", fontSize = 10.sp, color = MutedText, maxLines = 1)
                        }
                    }

                    // Station Setup Code
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(CanvasBg, RoundedCornerShape(6.dp))
                            .border(1.dp, Ink, RoundedCornerShape(6.dp))
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("🔑 Base Station Setup Code:", fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = MutedText)
                            Text("382-991", fontSize = 13.sp, fontWeight = FontWeight.Black, color = com.treefort.guardian.theme.ColorRustyOrange)
                        }
                        Text("Online & Paired", fontSize = 10.sp, fontWeight = FontWeight.Black, color = ForestGreen)
                    }
                }
            }
        }

        var showEditCurfewDialog by remember { mutableStateOf(false) }
        var quietRules by remember {
            mutableStateOf(
                listOf(
                    QuietRule(title = "Bedtime (Nightly)", startHour = 20, startMinute = 30, endHour = 7, endMinute = 0, isEnabled = true),
                    QuietRule(title = "Homework Hours", startHour = 16, startMinute = 0, endHour = 17, endMinute = 30, isEnabled = true)
                )
            )
        }

        if (showEditCurfewDialog) {
            QuietTimeDialog(
                rules = quietRules,
                onDismiss = { showEditCurfewDialog = false },
                onSave = { updated ->
                    quietRules = updated
                    showEditCurfewDialog = false
                }
            )
        }

        // Quiet Time (Customizable Rules)
        TactileCard(
            modifier = Modifier.fillMaxWidth(),
            backgroundColor = CardBg,
            shadowOffset = 3.dp
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Quiet Time",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Black,
                            color = Ink
                        )
                        Text(
                            text = "${quietRules.count { it.isEnabled }} active quiet schedules",
                            fontSize = 12.sp,
                            color = MutedText
                        )
                    }
                    TactilePill(
                        text = "EDIT",
                        backgroundColor = com.treefort.guardian.theme.ColorBluePowder,
                        textColor = Ink,
                        shadowOffset = 1.dp,
                        onClick = { showEditCurfewDialog = true }
                    )
                }
                Spacer(modifier = Modifier.height(14.dp))
                
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    quietRules.forEach { rule ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(rule.title, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Ink)
                                Text(rule.timeFormatted, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (rule.isEnabled) Ink else MutedText)
                            }
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .background(if (rule.isEnabled) com.treefort.guardian.theme.ColorSafeGreen else Color.Gray.copy(alpha = 0.3f), CircleShape)
                                    .border(1.dp, Ink, CircleShape)
                            )
                        }
                    }
                }
            }
        }
    }

        // Prominent Campfire Hero Card
        TactileCard(
            modifier = Modifier.fillMaxWidth(),
            backgroundColor = CardBg,
            shadowOffset = 4.dp
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .background(com.treefort.guardian.theme.ColorWarningYellow, RoundedCornerShape(12.dp))
                            .border(2.dp, Ink, RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        IconCampfire(modifier = Modifier.size(26.dp), color = Ink)
                    }
                    Column {
                        Text(
                            text = "All-Hands Campfire",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Black,
                            color = Ink
                        )
                        Text(
                            text = "Broadcast ring all household phones",
                            fontSize = 12.sp,
                            color = MutedText,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                TactileButton(
                    text = "Call The Campfire (#0)",
                    backgroundColor = com.treefort.guardian.theme.ColorSafeGreen,
                    textColor = Ink,
                    onClick = {
                        WebRTCManager.startCall("*90001")
                        Toast.makeText(context, "Calling The Campfire (#0)...", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

data class QuietRule(
    val id: String = java.util.UUID.randomUUID().toString(),
    var title: String,
    var startHour: Int,
    var startMinute: Int,
    var endHour: Int,
    var endMinute: Int,
    var isEnabled: Boolean = true
) {
    val timeFormatted: String
        get() {
            fun format(h: Int, m: Int): String {
                val period = if (h >= 12) "PM" else "AM"
                val h12 = if (h == 0) 12 else if (h > 12) h - 12 else h
                return String.format(java.util.Locale.US, "%d:%02d %s", h12, m, period)
            }
            return "${format(startHour, startMinute)} – ${format(endHour, endMinute)}"
        }
}

@Composable
fun RequestsCard(
    modifier: Modifier = Modifier,
    onApproveContact: ((com.treefort.guardian.data.Contact) -> Unit)? = null
) {
    var isExpanded by remember { mutableStateOf(false) }
    val pendingRequests = AndroidProfileManager.pendingContactRequests
    val count = pendingRequests.size

    TactileCard(
        modifier = modifier.fillMaxWidth(),
        backgroundColor = com.treefort.guardian.theme.ColorYellowPale,
        shadowOffset = 3.dp,
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(
                                if (count > 0) com.treefort.guardian.theme.ColorWarningYellow else com.treefort.guardian.theme.ColorSafeGreen,
                                CircleShape
                            )
                            .border(1.5.dp, Ink, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (count > 0) "🔔" else "✓",
                            fontSize = 14.sp
                        )
                    }

                    Text(
                        text = "Requests",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Black,
                        color = Ink
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TactilePill(
                        text = "$count",
                        backgroundColor = if (count > 0) com.treefort.guardian.theme.ColorWarningYellow else CardBg,
                        textColor = Ink,
                        shadowOffset = 1.dp
                    )

                    Text(
                        text = if (isExpanded || count > 0) "▲" else "▼",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Ink
                    )
                }
            }

            if (isExpanded || count > 0) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(Ink.copy(alpha = 0.2f))
                )
                if (count == 0) {
                    Text(
                        text = "All caught up · 0 waiting",
                        fontSize = 12.sp,
                        color = MutedText
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        pendingRequests.forEach { pending ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(CardBg, RoundedCornerShape(8.dp))
                                    .border(1.dp, Ink, RoundedCornerShape(8.dp))
                                    .padding(8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .background(parseHexColor(pending.avatarBg), CircleShape)
                                            .border(1.5.dp, Ink, CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(pending.avatarEmoji ?: "👤", fontSize = 18.sp)
                                    }

                                    Column {
                                        Text(pending.name, fontSize = 14.sp, fontWeight = FontWeight.Black, color = Ink)
                                        Text("Wants to connect · ${pending.phone ?: ""}", fontSize = 11.sp, color = MutedText)
                                    }
                                }

                                TactilePill(
                                    text = "APPROVE",
                                    backgroundColor = com.treefort.guardian.theme.ColorSafeGreen,
                                    textColor = Ink,
                                    shadowOffset = 1.dp,
                                    onClick = {
                                        AndroidProfileManager.pendingContactRequests = AndroidProfileManager.pendingContactRequests.filter { it.id != pending.id }
                                        onApproveContact?.invoke(pending)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun QuietTimeDialog(
    rules: List<QuietRule>,
    onDismiss: () -> Unit,
    onSave: (List<QuietRule>) -> Unit
) {
    var editableRules by remember { mutableStateOf(rules.map { it.copy() }) }
    var newRuleTitle by remember { mutableStateOf("") }
    var newStartHour by remember { mutableIntStateOf(14) }
    var newEndHour by remember { mutableIntStateOf(16) }
    var showAddRule by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .heightIn(max = 680.dp)
        ) {
            TactileCard(
                modifier = Modifier.fillMaxWidth(),
                backgroundColor = CardBg,
                shadowOffset = 6.dp,
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Quiet Time Schedules", fontSize = 18.sp, fontWeight = FontWeight.Black, color = Ink)
                            Text("Silence station ringer during bedtimes and homework", fontSize = 11.5.sp, color = MutedText)
                        }
                        TactilePill(text = "QUIET", backgroundColor = com.treefort.guardian.theme.ColorRetroGrape, textColor = Color.White, shadowOffset = 1.dp)
                    }

                    var unlockedRuleIndices by remember { mutableStateOf(setOf<Int>()) }

                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        editableRules.forEachIndexed { index, rule ->
                            TactileCard(
                                modifier = Modifier.fillMaxWidth(),
                                backgroundColor = CanvasBg,
                                shadowOffset = 2.dp,
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // Inset Well for Schedule Name
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .background(CardBg, RoundedCornerShape(8.dp))
                                                .border(1.5.dp, Ink, RoundedCornerShape(8.dp))
                                                .padding(horizontal = 10.dp, vertical = 6.dp)
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                Text("✏️", fontSize = 11.sp)
                                                Text(rule.title, fontSize = 13.5.sp, fontWeight = FontWeight.Bold, color = Ink)
                                            }
                                        }

                                        Spacer(modifier = Modifier.width(12.dp))

                                        Switch(
                                            checked = rule.isEnabled,
                                            onCheckedChange = { checked ->
                                                editableRules = editableRules.toMutableList().also {
                                                    it[index] = rule.copy(isEnabled = checked)
                                                }
                                            },
                                            colors = SwitchDefaults.colors(
                                                checkedThumbColor = Color.White,
                                                checkedTrackColor = com.treefort.guardian.theme.ColorSafeGreen
                                            )
                                        )
                                    }

                                    // Time Settings: Locked by default with pencil unlock button
                                    val isUnlocked = unlockedRuleIndices.contains(index)

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(CardBg, RoundedCornerShape(8.dp))
                                            .border(1.dp, Ink, RoundedCornerShape(8.dp))
                                            .padding(horizontal = 10.dp, vertical = 8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text("HOURS", fontSize = 9.5.sp, fontWeight = FontWeight.Black, color = MutedText)
                                            Text(rule.timeFormatted, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Ink)
                                        }

                                        TactilePill(
                                            text = if (isUnlocked) "LOCK" else "✏️ EDIT TIME",
                                            backgroundColor = if (isUnlocked) com.treefort.guardian.theme.ColorPrimaryBlue else com.treefort.guardian.theme.ColorYellowPale,
                                            textColor = if (isUnlocked) Color.White else Ink,
                                            shadowOffset = 1.dp,
                                            onClick = {
                                                unlockedRuleIndices = if (isUnlocked) {
                                                    unlockedRuleIndices - index
                                                } else {
                                                    unlockedRuleIndices + index
                                                }
                                            }
                                        )
                                    }

                                    if (isUnlocked) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            TactilePill(
                                                text = "8:00 PM – 7:00 AM",
                                                backgroundColor = PillBg,
                                                textColor = Ink,
                                                onClick = {
                                                    editableRules = editableRules.toMutableList().also {
                                                        it[index] = rule.copy(startHour = 20, startMinute = 0, endHour = 7, endMinute = 0)
                                                    }
                                                }
                                            )
                                            TactilePill(
                                                text = "1:00 PM – 3:00 PM",
                                                backgroundColor = PillBg,
                                                textColor = Ink,
                                                onClick = {
                                                    editableRules = editableRules.toMutableList().also {
                                                        it[index] = rule.copy(startHour = 13, startMinute = 0, endHour = 15, endMinute = 0)
                                                    }
                                                }
                                            )
                                        }
                                    }

                                    // Delete Schedule Button
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.End
                                    ) {
                                        Text(
                                            text = "🗑️ DELETE SCHEDULE",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Black,
                                            color = com.treefort.guardian.theme.ColorDangerRed,
                                            modifier = Modifier
                                                .clickable {
                                                    editableRules = editableRules.filterIndexed { i, _ -> i != index }
                                                }
                                                .padding(vertical = 4.dp, horizontal = 6.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    if (showAddRule) {
                        TactileCard(
                            modifier = Modifier.fillMaxWidth(),
                            backgroundColor = CanvasBg,
                            shadowOffset = 2.dp,
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("New Rule", fontSize = 13.sp, fontWeight = FontWeight.Black, color = Ink)
                                OutlinedTextField(
                                    value = newRuleTitle,
                                    onValueChange = { newRuleTitle = it },
                                    placeholder = { Text("e.g. Afternoon Nap", fontSize = 12.sp) },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    TactileButton(
                                        text = "Add Schedule",
                                        backgroundColor = com.treefort.guardian.theme.ColorSafeGreen,
                                        textColor = Ink,
                                        onClick = {
                                            if (newRuleTitle.isNotBlank()) {
                                                editableRules = editableRules + QuietRule(
                                                    title = newRuleTitle.trim(),
                                                    startHour = newStartHour,
                                                    startMinute = 0,
                                                    endHour = newEndHour,
                                                    endMinute = 0,
                                                    isEnabled = true
                                                )
                                                newRuleTitle = ""
                                                showAddRule = false
                                            }
                                        },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    } else {
                        TactileButton(
                            text = "+ Add Another Quiet Rule",
                            backgroundColor = PillBg,
                            textColor = Ink,
                            onClick = { showAddRule = true },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        TactileButton(
                            text = "Cancel",
                            backgroundColor = CardBg,
                            textColor = Ink,
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f)
                        )
                        TactileButton(
                            text = "Save Schedules",
                            backgroundColor = com.treefort.guardian.theme.ColorPrimaryBlue,
                            textColor = Color.White,
                            onClick = { onSave(editableRules) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

fun parseHexColor(hex: String?, default: Color = TreeFortGreen): Color {
    if (hex.isNullOrBlank()) return default
    return try {
        Color(hex.toColorInt())
    } catch (_: Exception) {
        default
    }
}

val AVATAR_ICONS = listOf(
    "⚽", "🏀", "🎸", "🌲", "🔥", "🐱",
    "🐾", "⭐", "❤️", "📞", "🤖", "🚀",
    "🎨", "🚲", "🐶", "🍕", "🍦", "📚",
    "🎧", "🌈", "🏕️", "🛹", "🎮", "💡"
)

val AVATAR_PALETTE_HEX = listOf(
    "#1C1E1D", "#F6F4EE", "#E6E2D6", "#3A8BFF", "#FFC43A", "#3DCC79",
    "#FF5E3A", "#194E9A", "#89C2FF", "#CA8A04", "#FFE79A", "#1B6E3D",
    "#82EBB0", "#B82A1B", "#7A3EE4", "#E86C2D", "#8C5E3B", "#556E7B"
)

@Composable
fun ContactDialog(
    contactToEdit: Contact? = null,
    onDismiss: () -> Unit,
    onDelete: ((Contact) -> Unit)? = null,
    onSave: (name: String, number: String, speedDial: String, targetStation: String, allowIn: Boolean, allowOut: Boolean, avatarEmoji: String, avatarBg: String) -> Unit
) {
    val isEdit = contactToEdit != null
    val isCampfire = contactToEdit?.phone == "*90001" ||
                     contactToEdit?.name?.contains("Campfire", ignoreCase = true) == true ||
                     contactToEdit?.speedDial == "#0"

    var name by remember(contactToEdit) { mutableStateOf(if (isCampfire) "Campfire Room" else (contactToEdit?.name ?: "")) }
    var number by remember(contactToEdit) { mutableStateOf(if (isCampfire) "*90001" else (contactToEdit?.treefortId ?: contactToEdit?.phone?.substringBefore(" (") ?: "")) }
    var pairingCode by remember(contactToEdit) { mutableStateOf(if (isCampfire) "900-001" else "382-991") }
    var speedDial by remember(contactToEdit) {
        mutableStateOf(
            contactToEdit?.speedDial
                ?: if (contactToEdit?.phone?.contains("#") == true) "#" + contactToEdit.phone.substringAfter("#").substringBefore(")")
                else if (isCampfire) "#0"
                else "None"
        )
    }
    var targetStation by remember(contactToEdit) { mutableStateOf(contactToEdit?.targetStation ?: "All") }
    var allowIn by remember(contactToEdit) { mutableStateOf(contactToEdit?.allowIncoming ?: true) }
    var allowOut by remember(contactToEdit) { mutableStateOf(contactToEdit?.allowOutgoing ?: true) }
    var selectedEmoji by remember(contactToEdit) { mutableStateOf(if (isCampfire) "🔥" else (contactToEdit?.avatarEmoji ?: "⚽")) }
    var selectedBg by remember(contactToEdit) { mutableStateOf(if (isCampfire) "#FFC43A" else (contactToEdit?.avatarBg ?: "#3DCC79")) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .heightIn(max = 680.dp)
        ) {
            TactileCard(
                modifier = Modifier.fillMaxWidth(),
                backgroundColor = CardBg,
                shadowOffset = 6.dp,
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Header Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isCampfire) "Edit Campfire Room" else if (isEdit) "Edit Approved Contact" else "Add Approved Contact",
                                fontSize = 17.5.sp,
                                fontWeight = FontWeight.Black,
                                color = Ink
                            )
                            Text(
                                text = if (isCampfire) "Permanent Family Party Line · Always Active" else if (isEdit) "Update avatar, speed dial & call permissions" else "Safelist 2FA Guardian Handshake",
                                fontSize = 11.sp,
                                color = MutedText,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    // Live Contact Card Preview
                    TactileCard(
                        modifier = Modifier.fillMaxWidth(),
                        backgroundColor = CardBg,
                        shadowOffset = 2.dp,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(46.dp)
                                        .background(parseHexColor(selectedBg), CircleShape)
                                        .border(2.dp, Ink, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(selectedEmoji, fontSize = 22.sp)
                                }

                                Column {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = name.ifBlank { "Friend Name" },
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (name.isBlank()) MutedText else Ink
                                        )
                                        if (speedDial != "None" && speedDial.isNotBlank()) {
                                            Box(
                                                modifier = Modifier
                                                    .background(SunburstAmber, RoundedCornerShape(4.dp))
                                                    .border(1.dp, Ink, RoundedCornerShape(4.dp))
                                                    .padding(horizontal = 4.dp, vertical = 1.dp)
                                            ) {
                                                Text(
                                                    text = speedDial,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Black,
                                                    color = Ink
                                                )
                                            }
                                        }
                                    }
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(
                                            text = number.ifBlank { "*XXXXX" },
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MutedText
                                        )
                                        Text(
                                            text = "· ${if (targetStation == "All") "All Stations" else targetStation}",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = GuardianBlue
                                        )
                                    }
                                }
                            }

                            Column(
                                horizontalAlignment = Alignment.End,
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Text(
                                    text = if (allowIn) "Friend can call" else "Inbound Blocked",
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (allowIn) ForestGreen else TerracottaCoral
                                )
                                Text(
                                    text = if (allowOut) "Can call Friend" else "Outbound Blocked",
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (allowOut) GuardianBlue else TerracottaCoral
                                )
                            }
                        }
                    }

                    // --- AVATAR MANAGEMENT MODULE ---
                    if (isCampfire) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(PillBg, RoundedCornerShape(12.dp))
                                .border(1.5.dp, Ink, RoundedCornerShape(12.dp))
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(52.dp)
                                    .background(SunburstAmber, CircleShape)
                                    .border(2.5.dp, Ink, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("🔥", fontSize = 26.sp)
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("CAMPFIRE FAMILY ROOM", fontSize = 12.sp, fontWeight = FontWeight.Black, color = Ink)
                                Text("Fixed permanent party line avatar", fontSize = 11.sp, color = MutedText)
                            }
                        }
                    } else {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(PillBg, RoundedCornerShape(12.dp))
                                .border(1.5.dp, Ink, RoundedCornerShape(12.dp))
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    // Live Avatar Preview Box
                                    Box(
                                        modifier = Modifier
                                            .size(54.dp)
                                            .background(parseHexColor(selectedBg), CircleShape)
                                            .border(2.5.dp, Ink, CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(selectedEmoji, fontSize = 26.sp)
                                    }

                                    Spacer(modifier = Modifier.width(12.dp))

                                    Column {
                                        Text("AVATAR CUSTOMIZER", fontSize = 11.5.sp, fontWeight = FontWeight.Black, color = Ink)
                                        Text("Pick icon & background", fontSize = 11.sp, color = MutedText)
                                    }
                                }

                                TactilePill(
                                    text = "🎲 RANDOM",
                                    backgroundColor = CardBg,
                                    textColor = Ink,
                                    shadowOffset = 1.dp,
                                    onClick = {
                                        selectedEmoji = AVATAR_ICONS.random()
                                        selectedBg = AVATAR_PALETTE_HEX.random()
                                    }
                                )
                            }

                            // Icon Selector (4 rows of 6 icons = 24 icons)
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                AVATAR_ICONS.chunked(6).forEach { rowEmojis ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        rowEmojis.forEach { emoji ->
                                            val isSelected = selectedEmoji == emoji
                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .size(36.dp)
                                                    .background(if (isSelected) SunburstAmber else CardBg, RoundedCornerShape(8.dp))
                                                    .border(if (isSelected) 2.dp else 1.dp, Ink, RoundedCornerShape(8.dp))
                                                    .clickable { selectedEmoji = emoji },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(emoji, fontSize = 18.sp)
                                            }
                                        }
                                    }
                                }
                            }

                            // 18-Color Neobrutalist Palette Selector (2 rows of 9)
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("18-COLOR NEOBRUTALIST PALETTE:", fontSize = 10.sp, fontWeight = FontWeight.Black, color = Ink)

                                AVATAR_PALETTE_HEX.chunked(9).forEach { rowColors ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        rowColors.forEach { hex ->
                                            val isSelected = selectedBg.equals(hex, ignoreCase = true)
                                            val swatchColor = parseHexColor(hex)
                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .height(26.dp)
                                                    .background(swatchColor, CircleShape)
                                                    .border(if (isSelected) 2.5.dp else 1.dp, Ink, CircleShape)
                                                    .clickable { selectedBg = hex },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                if (isSelected) {
                                                    Text(
                                                        text = "✓",
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Black,
                                                        color = if (hex == "#1C1E1D" || hex == "#194E9A" || hex == "#1B6E3D" || hex == "#B82A1B") Color.White else Ink
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Contact Name Input
                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text("CONTACT NAME", fontSize = 11.sp, fontWeight = FontWeight.Black, color = Ink)
                        OutlinedTextField(
                            value = name,
                            onValueChange = { if (!isCampfire) name = it },
                            placeholder = { Text("e.g. Grandpa Joe", color = MutedText, fontSize = 13.5.sp) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            enabled = !isCampfire,
                            shape = RoundedCornerShape(8.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Ink,
                                unfocusedTextColor = Ink,
                                focusedBorderColor = Ink,
                                unfocusedBorderColor = Ink,
                                disabledBorderColor = Color(0xFFD1D5DB),
                                focusedContainerColor = PillBg,
                                unfocusedContainerColor = PillBg,
                                disabledContainerColor = Color(0xFFF3F4F6),
                                disabledTextColor = Ink
                            )
                        )
                        if (isCampfire) {
                            Text("Permanent household family room name", fontSize = 10.5.sp, color = MutedText)
                        }
                    }

                    // TreeFort 5-Digit *Code Input
                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("TREEFORT 5-DIGIT CODE", fontSize = 11.sp, fontWeight = FontWeight.Black, color = Ink)
                            Text("*NNNNN", fontSize = 9.5.sp, fontWeight = FontWeight.Bold, color = GuardianBlue)
                        }
                        OutlinedTextField(
                            value = number,
                            onValueChange = { if (!isCampfire) number = it },
                            placeholder = { Text("e.g. *24819", color = MutedText, fontSize = 13.5.sp) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            enabled = !isCampfire,
                            shape = RoundedCornerShape(8.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Ink,
                                unfocusedTextColor = Ink,
                                focusedBorderColor = Ink,
                                unfocusedBorderColor = Ink,
                                disabledBorderColor = Color(0xFFD1D5DB),
                                focusedContainerColor = PillBg,
                                unfocusedContainerColor = PillBg,
                                disabledContainerColor = Color(0xFFF3F4F6),
                                disabledTextColor = Ink
                            )
                        )
                        Text(
                            text = if (isCampfire) "*90001 is the shared party line for all home stations" else "5-digit phone number assigned to their TreeFort station or mobile app.",
                            fontSize = 10.5.sp,
                            color = MutedText
                        )
                    }

                    // Secret Pairing Code for Mutual Countersigning / Remote Approval
                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("SECRET PAIRING CODE", fontSize = 11.sp, fontWeight = FontWeight.Black, color = Ink)
                            Text("(NNN-NNN)", fontSize = 9.5.sp, fontWeight = FontWeight.Bold, color = com.treefort.guardian.theme.ColorRustyOrange)
                        }
                        OutlinedTextField(
                            value = pairingCode,
                            onValueChange = { if (!isCampfire) pairingCode = it },
                            placeholder = { Text("e.g. 519-204", color = MutedText, fontSize = 13.5.sp) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            enabled = !isCampfire,
                            shape = RoundedCornerShape(8.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Ink,
                                unfocusedTextColor = Ink,
                                focusedBorderColor = Ink,
                                unfocusedBorderColor = Ink,
                                disabledBorderColor = Color(0xFFD1D5DB),
                                focusedContainerColor = PillBg,
                                unfocusedContainerColor = PillBg,
                                disabledContainerColor = Color(0xFFF3F4F6),
                                disabledTextColor = Ink
                            )
                        )
                        Text("6-digit security code displayed on their station to countersign and approve friend link.", fontSize = 10.5.sp, color = MutedText)
                    }

                    // Speed Dial Keypad Slots (None, #0, #1 – #9)
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("ASSIGN SPEED DIAL KEYPAD SLOT", fontSize = 11.sp, fontWeight = FontWeight.Black, color = Ink)

                        // Row 1: None, #0, #1, #2, #3
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf("None", "#0", "#1", "#2", "#3").forEach { slot ->
                                val isSelected = speedDial == slot
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .background(if (isSelected) SunburstAmber else PillBg, RoundedCornerShape(8.dp))
                                        .border(if (isSelected) 2.dp else 1.dp, Ink, RoundedCornerShape(8.dp))
                                        .clickable { speedDial = slot }
                                        .padding(vertical = 7.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = slot,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Black,
                                        color = Ink
                                    )
                                }
                            }
                        }

                        // Row 2: #4, #5, #6, #7, #8, #9
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf("#4", "#5", "#6", "#7", "#8", "#9").forEach { slot ->
                                val isSelected = speedDial == slot
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .background(if (isSelected) SunburstAmber else PillBg, RoundedCornerShape(8.dp))
                                        .border(if (isSelected) 2.dp else 1.dp, Ink, RoundedCornerShape(8.dp))
                                        .clickable { speedDial = slot }
                                        .padding(vertical = 7.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = slot,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Black,
                                        color = Ink
                                    )
                                }
                            }
                        }
                    }

                    // Target Station Selector: All vs Specific TreeFort Stations
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("TARGET TREEFORT STATION", fontSize = 11.sp, fontWeight = FontWeight.Black, color = Ink)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf("All", "Kid's Bedroom", "Playroom").forEach { st ->
                                val isSelected = targetStation == st
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .background(if (isSelected) SunburstAmber else PillBg, RoundedCornerShape(8.dp))
                                        .border(if (isSelected) 2.dp else 1.dp, Ink, RoundedCornerShape(8.dp))
                                        .clickable { targetStation = st }
                                        .padding(vertical = 7.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = if (st == "All") "All Stations" else st,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Black,
                                        color = Ink
                                    )
                                }
                            }
                        }
                    }

                    // Inbound & Outbound Calling Permissions with Warm Descriptions
                    TactileCard(
                        modifier = Modifier.fillMaxWidth(),
                        backgroundColor = PillBg,
                        shadowOffset = 2.dp,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text("CALL PERMISSIONS", fontSize = 11.sp, fontWeight = FontWeight.Black, color = Ink)

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Friend can call", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Ink)
                                    Text("Allow this friend to ring the physical TreeFort phone", fontSize = 11.sp, color = MutedText)
                                }
                                Switch(
                                    checked = allowIn,
                                    onCheckedChange = { allowIn = it },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color.White,
                                        checkedTrackColor = TreeFortGreen
                                    )
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(1.dp)
                                    .background(Ink.copy(alpha = 0.15f))
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Can call Friend", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Ink)
                                    Text("Allow your child to dial this friend from the keypad", fontSize = 11.sp, color = MutedText)
                                }
                                Switch(
                                    checked = allowOut,
                                    onCheckedChange = { allowOut = it },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color.White,
                                        checkedTrackColor = GuardianBlue
                                    )
                                )
                            }
                        }
                    }

                    if (isEdit && onDelete != null && !isCampfire) {
                        TactileButton(
                            text = "🗑️ Delete Friend",
                            backgroundColor = Color(0xFFEF4444),
                            textColor = Color.White,
                            onClick = {
                                onDelete(contactToEdit)
                                onDismiss()
                            },
                            modifier = Modifier.fillMaxWidth(),
                            fontSize = 13.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Bottom Action Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TactileButton(
                            text = "Cancel",
                            backgroundColor = CardBg,
                            textColor = Ink,
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f),
                            fontSize = 12.sp
                        )

                        TactileButton(
                            text = if (isEdit) "Save Changes" else "Approve & Save",
                            backgroundColor = com.treefort.guardian.theme.ColorPrimaryBlue,
                            textColor = Color.White,
                            onClick = {
                                if (name.isNotBlank()) {
                                    onSave(name.trim(), number.trim(), speedDial, targetStation, allowIn, allowOut, selectedEmoji, selectedBg)
                                }
                            },
                            modifier = Modifier.weight(1f),
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CallConfirmationDialog(
    contact: Contact,
    onDismiss: () -> Unit,
    onConfirmCall: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Ink.copy(alpha = 0.65f))
                .clickable { onDismiss() }
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            TactileCard(
                modifier = Modifier
                    .fillMaxWidth(0.96f)
                    .clickable(enabled = false) {},
                backgroundColor = CardBg,
                shadowOffset = 6.dp,
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Ready to Call?", fontSize = 22.sp, fontWeight = FontWeight.Black, color = Ink)
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // Extra Large Avatar
                    Box(
                        modifier = Modifier
                            .size(108.dp)
                            .background(parseHexColor(contact.avatarBg), CircleShape)
                            .border(3.5.dp, Ink, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = contact.avatarEmoji ?: contact.name.take(1),
                            fontSize = 52.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = contact.name,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Black,
                        color = Ink
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (!contact.phone.isNullOrBlank()) {
                            Box(
                                modifier = Modifier
                                    .background(CanvasBg, RoundedCornerShape(8.dp))
                                    .border(1.5.dp, Ink, RoundedCornerShape(8.dp))
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = "📞 ${contact.phone}",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Ink
                                )
                            }
                        }
                        if (!contact.speedDial.isNullOrBlank() && contact.speedDial != "None") {
                            Box(
                                modifier = Modifier
                                    .background(SunburstAmber, RoundedCornerShape(8.dp))
                                    .border(1.5.dp, Ink, RoundedCornerShape(8.dp))
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = contact.speedDial,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Ink
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    val callJokes = remember {
                        listOf(
                            "Ring up ${contact.name} to tell them a joke!" to "Why do birds fly south for winter? Because walking takes too long! 🪽",
                            "Call ${contact.name} and ask what's for dinner!" to "What do you call a fake noodle? An impasta! 🍝",
                            "Give ${contact.name} a ring from the fort!" to "Why did the scarecrow win an award? He was outstanding in his field! 🌾",
                            "Call ${contact.name} to trade secret fort secrets!" to "What kind of room has no doors or windows? A mushroom! 🍄",
                            "Quick check-in with ${contact.name}!" to "Why did the banana go to the doctor? It wasn't peeling well! 🍌",
                            "Ring-ring! ${contact.name} is waiting by the phone!" to "What do you call cheese that isn't yours? Nacho cheese! 🧀",
                            "Ring ${contact.name} to tell a telephone riddle!" to "Why did the telephone wear glasses? Because it lost its contacts! 👓",
                            "Call ${contact.name} from the treehouse!" to "How does a treehouse stay online? With branch offices and root routers! 🌲",
                            "Sing a tune to ${contact.name}!" to "Why couldn't the pony sing a lullaby? Because she was a little horse! 🐴",
                            "Share a refreshing joke with ${contact.name}!" to "What is a tree's favorite drink? Root beer! 🥤",
                            "Ring up ${contact.name} with sports news!" to "Why did the golfer bring two pairs of pants? In case he got a hole in one! ⛳",
                            "Tell ${contact.name} about a sleepy giant!" to "What do you call a sleeping dinosaur? A dino-snore! 💤",
                            "Salad time check-in with ${contact.name}!" to "Why did the tomato blush? Because it saw the salad dressing! 🍅",
                            "Special delivery riddle for ${contact.name}!" to "What has four wheels and flies? A garbage truck! 🚛",
                            "Ring ${contact.name} on the secret reptile line!" to "What kind of phone does a turtle use? A shell phone! 🐢",
                            "Sweet treats check-in with ${contact.name}!" to "Why did the cookie go to the hospital? Because it felt crumbly! 🍪"
                        )
                    }
                    val selectedJoke = remember { callJokes.random() }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(com.treefort.guardian.theme.ColorYellowPale, RoundedCornerShape(14.dp))
                            .border(1.5.dp, Ink, RoundedCornerShape(14.dp))
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "✨ CONVERSATION STARTER & JOKE",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            color = Ink
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = selectedJoke.first,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = Ink
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(CardBg, RoundedCornerShape(8.dp))
                                .border(1.dp, Ink, RoundedCornerShape(8.dp))
                                .padding(12.dp)
                        ) {
                            Text(
                                text = "💡 \"${selectedJoke.second}\"",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium,
                                color = Ink.copy(alpha = 0.9f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        TactileButton(
                            text = "📞 CALL NOW",
                            backgroundColor = TreeFortGreen,
                            textColor = Ink,
                            onClick = onConfirmCall,
                            modifier = Modifier.fillMaxWidth(),
                            fontSize = 15.sp
                        )

                        TactileButton(
                            text = "CANCEL",
                            backgroundColor = PillBg,
                            textColor = Ink,
                            onClick = onDismiss,
                            modifier = Modifier.fillMaxWidth(),
                            fontSize = 13.5.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SafelistScreen() {
    val defaultContacts = remember {
        listOf(
            Contact(
                id = 1,
                name = "Mom & Dad",
                phone = "*10001",
                speedDial = "#1",
                allowIncoming = true,
                allowOutgoing = true,
                avatarEmoji = "❤️",
                avatarBg = "#3DCC79"
            ),
            Contact(
                id = 2,
                name = "Grandma Ruth",
                phone = "*24819",
                speedDial = "#2",
                allowIncoming = true,
                allowOutgoing = true,
                avatarEmoji = "⭐",
                avatarBg = "#FFC72C"
            ),
            Contact(
                id = 3,
                name = "Uncle Bob (App)",
                phone = "*38912",
                speedDial = "#3",
                allowIncoming = true,
                allowOutgoing = true,
                avatarEmoji = "🎸",
                avatarBg = "#3A8BFF"
            ),
            Contact(
                id = 4,
                name = "Aunt Maya",
                phone = "*49102",
                speedDial = "#4",
                allowIncoming = true,
                allowOutgoing = true,
                avatarEmoji = "🎨",
                avatarBg = "#FF6542"
            ),
            Contact(
                id = 5,
                name = "Campfire Room",
                phone = "*90001",
                speedDial = "#0",
                allowIncoming = true,
                allowOutgoing = true,
                avatarEmoji = "🔥",
                avatarBg = "#FFC72C"
            )
        )
    }
    var contacts by remember { mutableStateOf(defaultContacts) }
    var searchQuery by remember { mutableStateOf("") }
    var contactUnderEdit by remember { mutableStateOf<Contact?>(null) }
    var contactToCall by remember { mutableStateOf<Contact?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        coroutineScope.launch {
            try {
                val fetched = SupabaseManager.fetchContacts()
                if (fetched.isNotEmpty()) {
                    contacts = fetched
                }
            } catch (_: Exception) {
                // Keep defaultContacts
            }
        }
    }

    // Call Confirmation Dialog
    if (contactToCall != null) {
        CallConfirmationDialog(
            contact = contactToCall!!,
            onDismiss = { contactToCall = null },
            onConfirmCall = {
                val target = contactToCall!!
                contactToCall = null
                WebRTCManager.startCall(
                    targetDialCode = target.phone ?: "",
                    targetName = target.name,
                    targetEmoji = target.avatarEmoji,
                    targetBg = target.avatarBg
                )
                Toast.makeText(context, "Calling ${target.name} (${target.phone ?: ""})...", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // Add or Edit Dialog
    if (showAddDialog || contactUnderEdit != null) {
        ContactDialog(
            contactToEdit = contactUnderEdit,
            onDismiss = {
                showAddDialog = false
                contactUnderEdit = null
            },
            onDelete = { deleted ->
                contacts = contacts.filter { it.id != deleted.id }
                coroutineScope.launch {
                    try {
                        SupabaseManager.deleteContact(deleted.id)
                    } catch (_: Exception) {}
                }
                showAddDialog = false
                contactUnderEdit = null
                Toast.makeText(context, "Deleted ${deleted.name}", Toast.LENGTH_SHORT).show()
            },
            onSave = { name, number, speedDial, targetStation, allowIn, allowOut, avatarEmoji, avatarBg ->
                val editing = contactUnderEdit
                if (editing != null) {
                    val updated = editing.copy(
                        name = name,
                        phone = number,
                        speedDial = if (speedDial != "None") speedDial else null,
                        targetStation = targetStation,
                        allowIncoming = allowIn,
                        allowOutgoing = allowOut,
                        avatarEmoji = avatarEmoji,
                        avatarBg = avatarBg
                    )
                    contacts = contacts.map { if (it.id == updated.id) updated else it }
                    Toast.makeText(context, "Updated $name!", Toast.LENGTH_SHORT).show()
                } else {
                    val newContact = Contact(
                        id = (contacts.maxOfOrNull { it.id } ?: 0) + 1,
                        name = name,
                        phone = number,
                        treefortId = if (number.startsWith("*")) number else null,
                        speedDial = if (speedDial != "None") speedDial else null,
                        targetStation = targetStation,
                        allowIncoming = allowIn,
                        allowOutgoing = allowOut,
                        avatarEmoji = avatarEmoji,
                        avatarBg = avatarBg
                    )
                    contacts = contacts + newContact
                    coroutineScope.launch {
                        try {
                            SupabaseManager.insertContact(newContact)
                        } catch (_: Exception) {
                            // Keep local
                        }
                    }
                    Toast.makeText(context, "Approved $name and synced to station!", Toast.LENGTH_SHORT).show()
                }
                showAddDialog = false
                contactUnderEdit = null
            }
        )
    }

    // Filter contacts based on real-time search query
    val filteredContacts = contacts.filter { contact ->
        searchQuery.isBlank() ||
            contact.name.contains(searchQuery, ignoreCase = true) ||
            (contact.phone?.contains(searchQuery, ignoreCase = true) == true) ||
            (contact.speedDial?.contains(searchQuery, ignoreCase = true) == true)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Friends",
                fontSize = 18.sp,
                fontWeight = FontWeight.Black,
                color = Ink
            )

            TactilePill(
                text = "ADD FRIEND",
                icon = "➕",
                backgroundColor = GuardianBlue,
                textColor = Color.White,
                shadowOffset = 2.dp,
                shape = RoundedCornerShape(8.dp),
                onClick = {
                    contactUnderEdit = null
                    showAddDialog = true
                }
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // --- INTERACTIVE REAL-TIME SEARCH BAR ---
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Find...", color = MutedText, fontSize = 13.5.sp) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(10.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Ink,
                unfocusedBorderColor = Ink,
                focusedContainerColor = CardBg,
                unfocusedContainerColor = CardBg
            ),
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    Text(
                        text = "✕",
                        modifier = Modifier
                            .clickable { searchQuery = "" }
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        fontWeight = FontWeight.Black,
                        fontSize = 14.sp,
                        color = MutedText
                    )
                }
            }
        )

        Spacer(modifier = Modifier.height(14.dp))

        // --- HOUSEHOLD TREEFORT BASE STATIONS CARD ---
        TactileCard(
            modifier = Modifier.fillMaxWidth(),
            backgroundColor = ForestGreen,
            shadowOffset = 3.dp,
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(TreeFortGreen, CircleShape)
                        .border(2.dp, Ink, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text("📻", fontSize = 20.sp)
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "TreeFort Station #01",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .background(SunburstAmber, RoundedCornerShape(4.dp))
                                .border(1.dp, Ink, RoundedCornerShape(4.dp))
                                .padding(horizontal = 4.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = "BASE STATION",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Black,
                                color = Ink
                            )
                        }
                    }

                    Text(
                        text = "Dial *14185 · TreeFort Station",
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.85f),
                        fontWeight = FontWeight.Medium
                    )
                }

                TactilePill(
                    text = "CALL",
                    icon = "📞",
                    backgroundColor = TreeFortGreen,
                    textColor = Ink,
                    shadowOffset = 1.dp,
                    onClick = {
                        contactToCall = Contact(
                            id = 9999,
                            name = "Kids' Room TreeFort Phone",
                            phone = "*14185",
                            speedDial = "Station #01",
                            allowIncoming = true,
                            allowOutgoing = true,
                            avatarEmoji = "📻",
                            avatarBg = "#3DCC79"
                        )
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (filteredContacts.isEmpty()) {
            TactileCard(
                modifier = Modifier.fillMaxWidth(),
                backgroundColor = CardBg,
                shadowOffset = 2.dp,
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("No matching contacts found", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Ink)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Try searching another name or dial code", fontSize = 12.sp, color = MutedText)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxWidth().weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredContacts, key = { it.id }) { contact ->
                    TactileCard(
                        modifier = Modifier.fillMaxWidth(),
                        backgroundColor = CardBg,
                        shadowOffset = 3.dp,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Contact Details area (Non-clickable label; edit via EDIT button only)
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Avatar circle using Avatar Management
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .background(parseHexColor(contact.avatarBg), CircleShape)
                                        .border(2.dp, Ink, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = contact.avatarEmoji ?: contact.name.take(1),
                                        fontSize = if (contact.avatarEmoji != null) 20.sp else 16.sp,
                                        fontWeight = FontWeight.Black,
                                        color = Ink
                                    )
                                }

                                Spacer(modifier = Modifier.width(10.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = contact.name,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Ink
                                        )
                                        if (!contact.speedDial.isNullOrBlank() && contact.speedDial != "None") {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Box(
                                                modifier = Modifier
                                                    .background(SunburstAmber, RoundedCornerShape(4.dp))
                                                    .border(1.dp, Ink, RoundedCornerShape(4.dp))
                                                    .padding(horizontal = 4.dp, vertical = 1.dp)
                                            ) {
                                                Text(
                                                    text = contact.speedDial,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Black,
                                                    color = Ink
                                                )
                                            }
                                        }
                                    }

                                    if (contact.phone != null) {
                                        Text(
                                            text = contact.phone,
                                            fontSize = 12.sp,
                                            color = MutedText,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            // Trailing Call & Edit actions
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                TactilePill(
                                    text = "CALL",
                                    icon = "📞",
                                    backgroundColor = com.treefort.guardian.theme.ColorSafeGreen,
                                    textColor = Ink,
                                    shadowOffset = 1.dp,
                                    onClick = {
                                        contactToCall = contact
                                    }
                                )

                                TactilePill(
                                    text = "EDIT",
                                    icon = "✏️",
                                    backgroundColor = PillBg,
                                    textColor = Ink,
                                    shadowOffset = 1.dp,
                                    onClick = {
                                        contactUnderEdit = contact
                                    }
                                )

                                if (contact.phone != "*90001") {
                                    TactilePill(
                                        text = "DEL",
                                        icon = "🗑️",
                                        backgroundColor = Color(0xFFFEE2E2),
                                        textColor = Color(0xFFDC2626),
                                        shadowOffset = 1.dp,
                                        onClick = {
                                            contacts = contacts.filter { it.id != contact.id }
                                            coroutineScope.launch {
                                                try {
                                                    SupabaseManager.deleteContact(contact.id)
                                                } catch (_: Exception) {}
                                            }
                                            Toast.makeText(context, "Deleted ${contact.name}", Toast.LENGTH_SHORT).show()
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

data class AuditEntry(
    val actionType: String,
    val summary: String,
    val details: String,
    val actor: String,
    val timestamp: String,
    val badgeBg: Color,
    val badgeColor: Color
)

data class CallLogEntry(
    val contactName: String,
    val detail: String,
    val timestamp: String,
    val avatarEmoji: String,
    val avatarBg: String,
    val dialCode: String = "*10001",
    val isMissed: Boolean = false
)

@Composable
fun ActivityScreen() {
    var selectedSection by remember { mutableIntStateOf(0) } // 0: Calls, 1: Audit Log
    val context = LocalContext.current
    var showCsvExportDialog by remember { mutableStateOf(false) }

    var callLogs by remember {
        mutableStateOf(
            listOf(
                CallLogEntry(
                    contactName = "Grandma Ruth",
                    detail = "Incoming · 4m 12s",
                    timestamp = "Today 10:14 AM",
                    avatarEmoji = "⭐",
                    avatarBg = "#FFC72C",
                    dialCode = "*24819"
                ),
                CallLogEntry(
                    contactName = "Mom & Dad",
                    detail = "Outgoing · 1m 45s",
                    timestamp = "Yesterday 6:30 PM",
                    avatarEmoji = "❤️",
                    avatarBg = "#3DCC79",
                    dialCode = "*10001"
                ),
                CallLogEntry(
                    contactName = "The Campfire",
                    detail = "Group Call · 12m",
                    timestamp = "Sep 25, 4:00 PM",
                    avatarEmoji = "🔥",
                    avatarBg = "#FFC72C",
                    dialCode = "*00000"
                ),
                CallLogEntry(
                    contactName = "Cousin Oliver",
                    detail = "Missed Call",
                    timestamp = "Sep 24, 2:15 PM",
                    avatarEmoji = "🚀",
                    avatarBg = "#3A8BFF",
                    dialCode = "*38932",
                    isMissed = true
                ),
                CallLogEntry(
                    contactName = "Aunt Maya",
                    detail = "Outgoing · 6m 20s",
                    timestamp = "Sep 23, 5:40 PM",
                    avatarEmoji = "🎨",
                    avatarBg = "#FF6542",
                    dialCode = "*77123"
                )
            )
        )
    }

    val auditLogs = listOf(
        AuditEntry(
            actionType = "CONTACT APPROVED",
            summary = "Approved Grandma Ruth (*24819) for two-way audio",
            details = "Added to household whitelist. All stations synchronized.",
            actor = "Sarah (Mom · Primary)",
            timestamp = "Today 10:14 AM",
            badgeBg = Color(0xFFD1F2D9),
            badgeColor = ForestGreen
        ),
        AuditEntry(
            actionType = "E911 VERIFIED",
            summary = "FCC E911 dispatch location confirmed",
            details = "742 Evergreen Terrace, Springfield, OR 97477 assigned to Station #01.",
            actor = "Sarah (Mom · Primary)",
            timestamp = "Yesterday 4:30 PM",
            badgeBg = Color(0xFFDBEAFE),
            badgeColor = GuardianBlue
        ),
        AuditEntry(
            actionType = "RULE UPDATED",
            summary = "Bedtime curfew quiet hours enforced",
            details = "Station ringer automatically silenced between 8:30 PM and 7:00 AM.",
            actor = "David (Dad · Co-Guardian)",
            timestamp = "Sep 25, 8:00 PM",
            badgeBg = Color(0xFFFEF3C7),
            badgeColor = Color(0xFFB45309)
        ),
        AuditEntry(
            actionType = "SPEED DIAL",
            summary = "Speed Dial #1 assigned to Mom & Dad (*10001)",
            details = "Keypad single-touch shortcut activated for physical handset.",
            actor = "Sarah (Mom · Primary)",
            timestamp = "Sep 24, 11:20 AM",
            badgeBg = Color(0xFFEDE9FE),
            badgeColor = Color(0xFF6D28D9)
        ),
        AuditEntry(
            actionType = "2FA VERIFIED",
            summary = "Co-Guardian 2FA verification verified for David",
            details = "SMS OTP handshake completed and root cryptographic key stored.",
            actor = "David (Dad · Co-Guardian)",
            timestamp = "Sep 23, 5:15 PM",
            badgeBg = Color(0xFFD1F2D9),
            badgeColor = ForestGreen
        ),
        AuditEntry(
            actionType = "STATION ONLINE",
            summary = "Living Room Base Station #01 connected via Wi-Fi",
            details = "Firmware v2.4.1 online and WebRTC signaling ticket confirmed.",
            actor = "Station Firmware",
            timestamp = "Sep 22, 10:00 AM",
            badgeBg = Color(0xFFDBEAFE),
            badgeColor = GuardianBlue
        )
    )

    if (showCsvExportDialog) {
        Dialog(onDismissRequest = { showCsvExportDialog = false }) {
            TactileCard(
                modifier = Modifier.fillMaxWidth(),
                backgroundColor = CardBg,
                shadowOffset = 5.dp,
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("Export Logbook (CSV)", fontWeight = FontWeight.Black, fontSize = 16.sp, color = Ink)
                    Text("Standard CSV record of contacts and verification events.", fontSize = 12.sp, color = MutedText)
                    val csvPreview = "Timestamp,Action,Actor,Summary\n" + auditLogs.take(3).joinToString("\n") { "\"${it.timestamp}\",\"${it.actionType}\",\"${it.actor}\",\"${it.summary}\"" }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(PillBg, RoundedCornerShape(8.dp))
                            .border(1.dp, Ink, RoundedCornerShape(8.dp))
                            .padding(8.dp)
                    ) {
                        Text(csvPreview, fontSize = 10.sp, color = Ink, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TactileButton(
                            text = "Cancel",
                            backgroundColor = CardBg,
                            textColor = Ink,
                            onClick = { showCsvExportDialog = false },
                            modifier = Modifier.weight(1f)
                        )
                        TactileButton(
                            text = "Share CSV",
                            backgroundColor = com.treefort.guardian.theme.ColorPrimaryBlue,
                            textColor = Color.White,
                            onClick = {
                                val csvText = "Timestamp,Action,Actor,Summary\n" + auditLogs.joinToString("\n") { "\"${it.timestamp}\",\"${it.actionType}\",\"${it.actor}\",\"${it.summary}\"" }
                                try {
                                    val sendIntent = android.content.Intent().apply {
                                        action = android.content.Intent.ACTION_SEND
                                        putExtra(android.content.Intent.EXTRA_TEXT, csvText)
                                        type = "text/plain"
                                    }
                                    val shareIntent = android.content.Intent.createChooser(sendIntent, "Export TreeFort Logbook")
                                    context.startActivity(shareIntent)
                                } catch (_: Exception) {
                                    val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                    val clip = android.content.ClipData.newPlainText("TreeFort Journal CSV", csvText)
                                    clipboard.setPrimaryClip(clip)
                                    Toast.makeText(context, "CSV copied to clipboard!", Toast.LENGTH_SHORT).show()
                                }
                                showCsvExportDialog = false
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Segmented Section Toggle: CALL HISTORY vs AUDIT LOG
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(CardBg, RoundedCornerShape(12.dp))
                .border(2.dp, Ink, RoundedCornerShape(12.dp))
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .background(
                        if (selectedSection == 0) TreeFortGreen else Color.Transparent,
                        RoundedCornerShape(8.dp)
                    )
                    .clickable { selectedSection = 0 }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "CALL HISTORY (${callLogs.size})",
                    fontWeight = FontWeight.Black,
                    fontSize = 11.5.sp,
                    color = Ink
                )
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .background(
                        if (selectedSection == 1) SunburstAmber else Color.Transparent,
                        RoundedCornerShape(8.dp)
                    )
                    .clickable { selectedSection = 1 }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "SAFETY JOURNAL (${auditLogs.size})",
                    fontWeight = FontWeight.Black,
                    fontSize = 11.5.sp,
                    color = Ink
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        if (selectedSection == 0) {
            LazyColumn(
                modifier = Modifier.fillMaxWidth().weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(callLogs) { log ->
                    TactileCard(
                        modifier = Modifier.fillMaxWidth(),
                        backgroundColor = CardBg,
                        shadowOffset = 3.dp,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Contact Avatar Circle
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .background(parseHexColor(log.avatarBg), CircleShape)
                                    .border(2.dp, Ink, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = log.avatarEmoji,
                                    fontSize = 18.sp
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = log.contactName,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Ink
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = log.detail,
                                    fontSize = 12.sp,
                                    color = if (log.isMissed) Color(0xFFDC2626) else MutedText,
                                    fontWeight = if (log.isMissed) FontWeight.Bold else FontWeight.Medium
                                )
                            }

                            Column(
                                horizontalAlignment = Alignment.End,
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = log.timestamp,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MutedText
                                )
                                TactilePill(
                                    text = "CALL",
                                    backgroundColor = com.treefort.guardian.theme.ColorSafeGreen,
                                    textColor = Ink,
                                    shadowOffset = 1.dp,
                                    onClick = {
                                        WebRTCManager.startCall(log.dialCode, log.contactName)
                                        val newEntry = CallLogEntry(
                                            contactName = log.contactName,
                                            detail = "Outgoing · Just now",
                                            timestamp = "Just now",
                                            avatarEmoji = log.avatarEmoji,
                                            avatarBg = log.avatarBg,
                                            dialCode = log.dialCode
                                        )
                                        callLogs = listOf(newEntry) + callLogs
                                        Toast.makeText(context, "Calling ${log.contactName} (${log.dialCode})...", Toast.LENGTH_SHORT).show()
                                    }
                                )
                            }
                        }
                    }
                }
            }
        } else {
            // Safety Journal
            LazyColumn(
                modifier = Modifier.fillMaxWidth().weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    TactileCard(
                        modifier = Modifier.fillMaxWidth(),
                        backgroundColor = PillBg,
                        shadowOffset = 3.dp,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Safety Journal",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Ink
                                )
                                TactilePill(
                                    text = "EXPORT",
                                    backgroundColor = CardBg,
                                    textColor = Ink,
                                    shadowOffset = 1.dp,
                                    onClick = { showCsvExportDialog = true }
                                )
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = "A permanent, tamper-proof record of every approved contact, bedtime curfew, and safety update. Only authorized guardians can modify your child's phone.",
                                fontSize = 12.sp,
                                color = Ink,
                                lineHeight = 16.sp
                            )
                        }
                    }
                }

                items(auditLogs) { entry ->
                    TactileCard(
                        modifier = Modifier.fillMaxWidth(),
                        backgroundColor = CardBg,
                        shadowOffset = 2.5.dp,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                TactilePill(
                                    text = entry.actionType,
                                    backgroundColor = entry.badgeBg,
                                    textColor = entry.badgeColor,
                                    shadowOffset = 1.dp
                                )

                                Text(
                                    text = entry.timestamp,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MutedText
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = entry.summary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Ink
                            )

                            Text(
                                text = entry.details,
                                fontSize = 12.sp,
                                color = MutedText
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = "👤 Actor: ${entry.actor}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = GuardianBlue
                            )
                        }
                    }
                }
            }
        }
    }
}

data class HouseholdTreeFort(
    val id: String = java.util.UUID.randomUUID().toString(),
    var name: String,
    var room: String,
    var number: String,
    var setupCode: String,
    var isOnline: Boolean = true,
    var homeLabel: String = "Mom's House",
    var physicalAddress: String = "742 Evergreen Terrace, Springfield, OR"
)

data class GuardianMember(
    val id: String = java.util.UUID.randomUUID().toString(),
    var name: String,
    var role: String,
    var emailOrPhone: String,
    var avatarEmoji: String,
    var avatarBg: String,
    var canManageRules: Boolean = true,
    var canManageContacts: Boolean = true,
    var isCoParentProtected: Boolean = false,
    var assignedHome: String = "Both Homes",
    var stationAccess: String = "All Stations"
)

enum class ProfileRole(
    val title: String,
    val subtitle: String,
    val hasStation: Boolean,
    val isCoParentDualHome: Boolean
) {
    PARENT_TWO_STATIONS(
        title = "Single Home (2 Stations)",
        subtitle = "Full household & station management",
        hasStation = true,
        isCoParentDualHome = false
    ),
    CO_PARENT_TWO_HOMES(
        title = "Co-Parenting (2 Homes · 4 Guardians)",
        subtitle = "1 station per house · Shared contacts · Station-level controls",
        hasStation = true,
        isCoParentDualHome = true
    ),
    FAMILY_CALLER_ONLY(
        title = "Uncle Bob (Caller Only)",
        subtitle = "Extended family caller · No physical station",
        hasStation = false,
        isCoParentDualHome = false
    )
}

object AndroidProfileManager {
    var isLoggedIn by mutableStateOf(true)
    var activeRole by mutableStateOf(ProfileRole.FAMILY_CALLER_ONLY)
    var phoneTreeFortNumber by mutableStateOf("*38912") // Uncle Bob (Android Guardian App)
    var isTemporaryMoveActive by mutableStateOf(false)
    var tempMoveHostName by mutableStateOf("Uncle Bob's Place")
    var tempMoveReturnDate by mutableStateOf("Sunday, 5:00 PM")
    var tempMoveAddress by mutableStateOf("404 Blueberry Hill Rd, Springfield")

    // Pending contact requests awaiting guardian approval (e.g. Uncle Bob)
    var pendingContactRequests by mutableStateOf(
        listOf(
            com.treefort.guardian.data.Contact(
                id = 888,
                name = "Uncle Bob (App)",
                phone = "*38912",
                speedDial = "#3",
                allowIncoming = true,
                allowOutgoing = true,
                avatarEmoji = "🎸",
                avatarBg = "#3A8BFF"
            )
        )
    )
}

data class SpeedDialAssignment(
    val id: Int,
    var slot: String,
    var contactName: String,
    var dialCode: String
)

@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val isOnline by WebRTCManager.isOnline.collectAsState()
    var handsetVol by remember { mutableFloatStateOf(0.85f) }
    var ringerVol by remember { mutableFloatStateOf(0.90f) }
    var curfewEnabled by remember { mutableStateOf(true) }

    // Dialog States
    var showAddTreeFortDialog by remember { mutableStateOf(false) }
    var showE911Dialog by remember { mutableStateOf(false) }
    var showGuardiansDialog by remember { mutableStateOf(false) }
    var showSpeedDialsDialog by remember { mutableStateOf(false) }
    var showPinDialog by remember { mutableStateOf(false) }
    var isPinLockEnabled by remember { mutableStateOf(false) }
    var antiKidPin by remember { mutableStateOf("1234") }
    var showHotelWifiDialog by remember { mutableStateOf(false) }
    var showCoParentDialog by remember { mutableStateOf(false) }
    var showEditOvernightMoveDialog by remember { mutableStateOf(false) }
    val prefs = remember { context.getSharedPreferences("treefort_prefs", android.content.Context.MODE_PRIVATE) }
    var boringTextEnabled by remember { mutableStateOf(prefs.getBoolean("boring_text_enabled", false)) }
    var showLogoutDialog by remember { mutableStateOf(false) }
    var showDeleteAccountDialog by remember { mutableStateOf(false) }
    var deleteAccountAckStep by remember { mutableIntStateOf(1) }
    var deleteCheck1 by remember { mutableStateOf(false) }
    var deleteCheck2 by remember { mutableStateOf(false) }
    var deleteConfirmText by remember { mutableStateOf("") }
    var selectedStationIndex by remember { mutableIntStateOf(0) }

    var stations by remember {
        mutableStateOf(
            listOf(
                HouseholdTreeFort(name = "Kid's Bedroom", room = "Bedroom #1", number = "*14185", setupCode = "382-991", isOnline = true, homeLabel = "Mom's House", physicalAddress = "742 Evergreen Terrace, Springfield, OR"),
                HouseholdTreeFort(name = "Playroom Phone", room = "Basement Den", number = "*14186", setupCode = "509-114", isOnline = true, homeLabel = "Dad's House", physicalAddress = "104 Maple Street, Springfield, OR")
            )
        )
    }

    var guardians by remember {
        mutableStateOf(
            listOf(
                GuardianMember(name = "Sarah (Mom)", role = "Primary Guardian (Home #1)", emailOrPhone = "sarah@family.treefort", avatarEmoji = "⭐", avatarBg = "#FFC43A", canManageRules = true, canManageContacts = true, isCoParentProtected = true, assignedHome = "Mom's House", stationAccess = "All Stations"),
                GuardianMember(name = "Mark (Stepdad)", role = "Co-Guardian (Home #1)", emailOrPhone = "mark@family.treefort", avatarEmoji = "🚴", avatarBg = "#3DCC79", canManageRules = true, canManageContacts = true, isCoParentProtected = false, assignedHome = "Mom's House", stationAccess = "Mom's Station Only"),
                GuardianMember(name = "David (Dad)", role = "Primary Guardian (Home #2)", emailOrPhone = "david@family.treefort", avatarEmoji = "🛡️", avatarBg = "#3A8BFF", canManageRules = true, canManageContacts = true, isCoParentProtected = true, assignedHome = "Dad's House", stationAccess = "All Stations"),
                GuardianMember(name = "Elena (Stepmom)", role = "Co-Guardian (Home #2)", emailOrPhone = "elena@family.treefort", avatarEmoji = "🎨", avatarBg = "#FF5E3A", canManageRules = true, canManageContacts = true, isCoParentProtected = false, assignedHome = "Dad's House", stationAccess = "Dad's Station Only")
            )
        )
    }

    var e911AddressStreet by remember { mutableStateOf("742 Evergreen Terrace") }
    var e911AddressCityStateZip by remember { mutableStateOf("Springfield, OR 97477") }

    var speedDials by remember {
        mutableStateOf(
            listOf(
                SpeedDialAssignment(id = 1, slot = "#1", contactName = "Mom & Dad", dialCode = "*10001"),
                SpeedDialAssignment(id = 2, slot = "#2", contactName = "Grandma Ruth", dialCode = "*24819"),
                SpeedDialAssignment(id = 3, slot = "#3", contactName = "Cousin Oliver", dialCode = "*38912"),
                SpeedDialAssignment(id = 4, slot = "#4", contactName = "Aunt Maya", dialCode = "*49102"),
                SpeedDialAssignment(id = 5, slot = "#5", contactName = "Unassigned", dialCode = ""),
                SpeedDialAssignment(id = 6, slot = "#6", contactName = "Unassigned", dialCode = ""),
                SpeedDialAssignment(id = 7, slot = "#7", contactName = "Unassigned", dialCode = ""),
                SpeedDialAssignment(id = 8, slot = "#8", contactName = "Unassigned", dialCode = ""),
                SpeedDialAssignment(id = 9, slot = "#9", contactName = "Unassigned", dialCode = "")
            )
        )
    }

    // Add TreeFort Dialog
    if (showAddTreeFortDialog) {
        var newStationName by remember { mutableStateOf("") }
        var newStationRoom by remember { mutableStateOf("") }
        var newSetupCode by remember { mutableStateOf("") }
        Dialog(onDismissRequest = { showAddTreeFortDialog = false }) {
            TactileCard(
                modifier = Modifier.fillMaxWidth(),
                backgroundColor = CardBg,
                shadowOffset = 5.dp,
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("Add a TreeFort", fontWeight = FontWeight.Black, fontSize = 17.sp, color = Ink)
                    Text("Connect a new physical phone station to your home.", fontSize = 12.sp, color = MutedText)
                    OutlinedTextField(
                        value = newStationName,
                        onValueChange = { newStationName = it },
                        placeholder = { Text("e.g. Playroom Phone", fontSize = 12.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = newStationRoom,
                        onValueChange = { newStationRoom = it },
                        placeholder = { Text("e.g. Upstairs Playroom", fontSize = 12.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = newSetupCode,
                        onValueChange = { newSetupCode = it },
                        placeholder = { Text("6-Digit Setup Code (e.g. 541-209)", fontSize = 12.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TactileButton(
                            text = "Cancel",
                            backgroundColor = CardBg,
                            textColor = Ink,
                            onClick = { showAddTreeFortDialog = false },
                            modifier = Modifier.weight(1f)
                        )
                        TactileButton(
                            text = "Connect Phone",
                            backgroundColor = com.treefort.guardian.theme.ColorSafeGreen,
                            textColor = Ink,
                            onClick = {
                                val name = if (newStationName.isNotBlank()) newStationName.trim() else "New TreeFort"
                                val room = if (newStationRoom.isNotBlank()) newStationRoom.trim() else "Playroom"
                                val code = if (newSetupCode.isNotBlank()) newSetupCode.trim() else "100-001"
                                val randomNum = "*${(10000..99999).random()}"
                                stations = stations + HouseholdTreeFort(name = name, room = room, number = randomNum, setupCode = code, isOnline = true)
                                Toast.makeText(context, "Added $name!", Toast.LENGTH_SHORT).show()
                                showAddTreeFortDialog = false
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }

    // Edit Overnight Relocation Dialog
    if (showEditOvernightMoveDialog) {
        var hostName by remember { mutableStateOf(AndroidProfileManager.tempMoveHostName) }
        var address by remember { mutableStateOf(AndroidProfileManager.tempMoveAddress) }
        var returnDate by remember { mutableStateOf(AndroidProfileManager.tempMoveReturnDate) }
        var selectedLocation by remember { mutableStateOf("Uncle Bob's Place") }
        var isCustomLocation by remember { mutableStateOf(false) }

        val savedLocations = remember {
            listOf(
                Triple("Uncle Bob's Place", "404 Blueberry Hill Rd, Springfield, OR", "Crazy Uncle Bob"),
                Triple("Grandma Ruth's", "12 Maple Wood Ln, Eugene, OR", "Grandma Ruth"),
                Triple("Lake Pine Cabin", "88 Whispering Pines Way, Sunriver, OR", "The Cabin")
            )
        }

        val schedulePresets = remember {
            listOf(
                "Weekend Sleepover" to "Sunday, 5:00 PM",
                "3-Day Visit" to "Tuesday, 12:00 PM",
                "1-Week Trip" to "Next Saturday, 10:00 AM"
            )
        }

        Dialog(onDismissRequest = { showEditOvernightMoveDialog = false }) {
            TactileCard(
                modifier = Modifier.fillMaxWidth(),
                backgroundColor = CardBg,
                shadowOffset = 5.dp,
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("Edit Overnight Relocation", fontWeight = FontWeight.Black, fontSize = 17.sp, color = Ink)
                    Text("Update destination host, local E911 dispatch, and auto-return schedule.", fontSize = 11.5.sp, color = MutedText)

                    Text("SELECT SAVED LOCATION", fontSize = 10.5.sp, fontWeight = FontWeight.Black, color = MutedText)
                    savedLocations.forEach { (locName, locAddr, locHost) ->
                        val isSelected = !isCustomLocation && (selectedLocation == locName)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(if (isSelected) com.treefort.guardian.theme.ColorYellowPale else CanvasBg, RoundedCornerShape(8.dp))
                                .border(if (isSelected) 1.5.dp else 1.dp, Ink, RoundedCornerShape(8.dp))
                                .clickable {
                                    isCustomLocation = false
                                    selectedLocation = locName
                                    hostName = locHost
                                    address = locAddr
                                }
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(locName, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Ink)
                                Text(locAddr, fontSize = 11.sp, color = MutedText)
                            }
                            if (isSelected) {
                                TactilePill(text = "SELECTED", backgroundColor = SunburstAmber, textColor = Ink, shadowOffset = 0.dp)
                            }
                        }
                    }

                    // Custom Location Option
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(if (isCustomLocation) com.treefort.guardian.theme.ColorBluePowder.copy(alpha = 0.3f) else CanvasBg, RoundedCornerShape(8.dp))
                            .border(if (isCustomLocation) 1.5.dp else 1.dp, Ink, RoundedCornerShape(8.dp))
                            .clickable { isCustomLocation = true }
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("📍 Enter New / Custom Address", fontSize = 12.sp, fontWeight = FontWeight.Black, color = Ink)
                        if (isCustomLocation) {
                            TactilePill(text = "CUSTOM", backgroundColor = GuardianBlue, textColor = Color.White, shadowOffset = 0.dp)
                        }
                    }

                    // Host & Address Fields
                    OutlinedTextField(
                        value = hostName,
                        onValueChange = { hostName = it },
                        label = { Text("Host / Destination Name", fontSize = 11.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = address,
                        onValueChange = { address = it },
                        label = { Text("Physical Address (E911 Dispatch)", fontSize = 11.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    // Schedule Presets
                    Text("SCHEDULE PRESETS", fontSize = 10.5.sp, fontWeight = FontWeight.Black, color = MutedText)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        schedulePresets.forEach { (presetTitle, presetReturn) ->
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(CanvasBg, RoundedCornerShape(6.dp))
                                    .border(1.dp, Ink, RoundedCornerShape(6.dp))
                                    .clickable { returnDate = presetReturn }
                                    .padding(vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(presetTitle, fontSize = 9.5.sp, fontWeight = FontWeight.Black, color = Ink)
                            }
                        }
                    }

                    OutlinedTextField(
                        value = returnDate,
                        onValueChange = { returnDate = it },
                        label = { Text("Auto-Return Date & Time", fontSize = 11.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TactileButton(
                            text = "Cancel",
                            backgroundColor = CardBg,
                            textColor = Ink,
                            onClick = { showEditOvernightMoveDialog = false },
                            modifier = Modifier.weight(1f)
                        )
                        TactileButton(
                            text = "Save Relocation",
                            backgroundColor = com.treefort.guardian.theme.ColorSafeGreen,
                            textColor = Ink,
                            onClick = {
                                if (hostName.isNotBlank()) AndroidProfileManager.tempMoveHostName = hostName.trim()
                                if (address.isNotBlank()) AndroidProfileManager.tempMoveAddress = address.trim()
                                if (returnDate.isNotBlank()) AndroidProfileManager.tempMoveReturnDate = returnDate.trim()
                                AndroidProfileManager.isTemporaryMoveActive = true
                                Toast.makeText(context, "Saved temporary relocation to ${AndroidProfileManager.tempMoveHostName}!", Toast.LENGTH_SHORT).show()
                                showEditOvernightMoveDialog = false
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }

    // E911 Dialog
    if (showE911Dialog) {
        var tempStreet by remember { mutableStateOf(e911AddressStreet) }
        var tempCityStateZip by remember { mutableStateOf(e911AddressCityStateZip) }
        Dialog(onDismissRequest = { showE911Dialog = false }) {
            TactileCard(
                modifier = Modifier.fillMaxWidth(),
                backgroundColor = CardBg,
                shadowOffset = 5.dp,
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("E911 Location", fontWeight = FontWeight.Black, fontSize = 17.sp, color = Ink)
                    Text("Verified physical address dispatched when 911 is dialed.", fontSize = 12.sp, color = MutedText)
                    OutlinedTextField(
                        value = tempStreet,
                        onValueChange = { tempStreet = it },
                        placeholder = { Text("Street address", fontSize = 12.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = tempCityStateZip,
                        onValueChange = { tempCityStateZip = it },
                        placeholder = { Text("City, State Zip", fontSize = 12.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TactileButton(
                            text = "Cancel",
                            backgroundColor = CardBg,
                            textColor = Ink,
                            onClick = { showE911Dialog = false },
                            modifier = Modifier.weight(1f)
                        )
                        TactileButton(
                            text = "Save Address",
                            backgroundColor = com.treefort.guardian.theme.ColorPrimaryBlue,
                            textColor = Color.White,
                            onClick = {
                                e911AddressStreet = tempStreet
                                e911AddressCityStateZip = tempCityStateZip
                                Toast.makeText(context, "E911 Address updated & verified!", Toast.LENGTH_SHORT).show()
                                showE911Dialog = false
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }

    // Guardians Dialog
    if (showGuardiansDialog) {
        var newGuardianName by remember { mutableStateOf("") }
        var newGuardianEmail by remember { mutableStateOf("") }
        var newGuardianRole by remember { mutableStateOf("Co-Guardian") }
        Dialog(onDismissRequest = { showGuardiansDialog = false }) {
            TactileCard(
                modifier = Modifier.fillMaxWidth(),
                backgroundColor = CardBg,
                shadowOffset = 6.dp,
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(18.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Manage Guardians", fontWeight = FontWeight.Black, fontSize = 17.sp, color = Ink)
                    Text("Guardians can approve contacts and adjust quiet hours.", fontSize = 12.sp, color = MutedText)

                    // Existing Guardians with permission toggles
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        guardians.forEachIndexed { index, guardian ->
                            TactileCard(
                                modifier = Modifier.fillMaxWidth(),
                                backgroundColor = CanvasBg,
                                shadowOffset = 2.dp,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("${guardian.avatarEmoji} ${guardian.name}", fontSize = 13.5.sp, fontWeight = FontWeight.Bold, color = Ink)
                                        Text(guardian.role, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = GuardianBlue)
                                    }
                                    Text(guardian.emailOrPhone, fontSize = 11.sp, color = MutedText)

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("Manage Rules", fontSize = 11.sp, color = Ink)
                                        Switch(
                                            checked = guardian.canManageRules,
                                            onCheckedChange = { chk ->
                                                guardians = guardians.toMutableList().also {
                                                    it[index] = guardian.copy(canManageRules = chk)
                                                }
                                            },
                                            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = TreeFortGreen)
                                        )
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("Approve Contacts", fontSize = 11.sp, color = Ink)
                                        Switch(
                                            checked = guardian.canManageContacts,
                                            onCheckedChange = { chk ->
                                                guardians = guardians.toMutableList().also {
                                                    it[index] = guardian.copy(canManageContacts = chk)
                                                }
                                            },
                                            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = GuardianBlue)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Add Guardian Form
                    Text("Add New Guardian", fontSize = 13.sp, fontWeight = FontWeight.Black, color = Ink)
                    OutlinedTextField(
                        value = newGuardianName,
                        onValueChange = { newGuardianName = it },
                        placeholder = { Text("Name (e.g. Grandma Joan)", fontSize = 12.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = newGuardianEmail,
                        onValueChange = { newGuardianEmail = it },
                        placeholder = { Text("Email or Phone", fontSize = 12.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TactileButton(
                            text = "Close",
                            backgroundColor = CardBg,
                            textColor = Ink,
                            onClick = { showGuardiansDialog = false },
                            modifier = Modifier.weight(1f)
                        )
                        TactileButton(
                            text = "Add Guardian",
                            backgroundColor = com.treefort.guardian.theme.ColorPrimaryBlue,
                            textColor = Color.White,
                            onClick = {
                                if (newGuardianName.isNotBlank()) {
                                    guardians = guardians + GuardianMember(
                                        name = newGuardianName.trim(),
                                        role = newGuardianRole,
                                        emailOrPhone = if (newGuardianEmail.isNotBlank()) newGuardianEmail.trim() else "guardian@family.treefort",
                                        avatarEmoji = "🛡️",
                                        avatarBg = "#89C2FF",
                                        canManageRules = true,
                                        canManageContacts = true
                                    )
                                    Toast.makeText(context, "Added $newGuardianName!", Toast.LENGTH_SHORT).show()
                                    newGuardianName = ""
                                    newGuardianEmail = ""
                                }
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }

    // Speed Dials Dialog with Station Selector, Friend Picker, and Trash/Clear Button
    if (showSpeedDialsDialog) {
        var selectedStationIndex by remember { mutableIntStateOf(0) }
        var selectedSlotId by remember { mutableIntStateOf(1) }
        var showFriendPickerSheet by remember { mutableStateOf(false) }

        val candidateFriends = remember {
            listOf(
                Contact(id = 1, name = "Mom & Dad", phone = "*10001", speedDial = "#1", allowIncoming = true, allowOutgoing = true, avatarEmoji = "❤️", avatarBg = "#3DCC79"),
                Contact(id = 2, name = "Grandma Ruth", phone = "*24819", speedDial = "#2", allowIncoming = true, allowOutgoing = true, avatarEmoji = "⭐", avatarBg = "#FFC72C"),
                Contact(id = 3, name = "Cousin Oliver", phone = "*38912", speedDial = "#3", allowIncoming = true, allowOutgoing = true, avatarEmoji = "🚀", avatarBg = "#3A8BFF"),
                Contact(id = 4, name = "Aunt Maya", phone = "*49102", speedDial = "#4", allowIncoming = true, allowOutgoing = true, avatarEmoji = "🎨", avatarBg = "#FF5E3A"),
                Contact(id = 5, name = "Leo & Toby (Puppy)", phone = "*58120", speedDial = "None", allowIncoming = true, allowOutgoing = true, avatarEmoji = "🐶", avatarBg = "#E86C2D"),
                Contact(id = 6, name = "Uncle Bob", phone = "*38912", speedDial = "None", allowIncoming = true, allowOutgoing = true, avatarEmoji = "⚽", avatarBg = "#3A8BFF")
            )
        }

        Dialog(onDismissRequest = { showSpeedDialsDialog = false }) {
            TactileCard(
                modifier = Modifier.fillMaxWidth(),
                backgroundColor = CardBg,
                shadowOffset = 5.dp,
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("Edit Speed Dials (#1–#9)", fontWeight = FontWeight.Black, fontSize = 16.sp, color = Ink)
                    Text("Assign 1-touch keypad shortcut buttons directly from your Friends list.", fontSize = 12.sp, color = MutedText)

                    // Station selector
                    if (stations.size > 1) {
                        Text("TARGET STATION", fontSize = 10.sp, fontWeight = FontWeight.Black, color = Ink)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            stations.forEachIndexed { idx, st ->
                                val isSel = selectedStationIndex == idx
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .background(if (isSel) SunburstAmber else PillBg, RoundedCornerShape(6.dp))
                                        .border(if (isSel) 2.dp else 1.dp, Ink, RoundedCornerShape(6.dp))
                                        .clickable { selectedStationIndex = idx }
                                        .padding(vertical = 6.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(st.name, fontSize = 11.sp, fontWeight = FontWeight.Black, color = Ink)
                                }
                            }
                        }
                    }

                    // Slot selector 1-5
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        (1..5).forEach { id ->
                            val slotStr = "#$id"
                            val isSel = selectedSlotId == id
                            val assigned = speedDials.find { it.id == id }?.contactName ?: "Unassigned"
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(if (isSel) SunburstAmber else if (assigned != "Unassigned") com.treefort.guardian.theme.ColorYellowPale else PillBg, RoundedCornerShape(6.dp))
                                    .border(if (isSel) 2.dp else 1.dp, Ink, RoundedCornerShape(6.dp))
                                    .clickable { selectedSlotId = id }
                                    .padding(vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(slotStr, fontSize = 11.sp, fontWeight = FontWeight.Black, color = Ink)
                            }
                        }
                    }
                    // Slot selector 6-9
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        (6..9).forEach { id ->
                            val slotStr = "#$id"
                            val isSel = selectedSlotId == id
                            val assigned = speedDials.find { it.id == id }?.contactName ?: "Unassigned"
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(if (isSel) SunburstAmber else if (assigned != "Unassigned") com.treefort.guardian.theme.ColorYellowPale else PillBg, RoundedCornerShape(6.dp))
                                    .border(if (isSel) 2.dp else 1.dp, Ink, RoundedCornerShape(6.dp))
                                    .clickable { selectedSlotId = id }
                                    .padding(vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(slotStr, fontSize = 11.sp, fontWeight = FontWeight.Black, color = Ink)
                            }
                        }
                    }

                    val currentSlot = speedDials.find { it.id == selectedSlotId }
                    val currentName = currentSlot?.contactName ?: "Unassigned"
                    val currentCode = currentSlot?.dialCode ?: ""

                    // Current Assignment Card with Clear/Trashcan Button
                    TactileCard(
                        modifier = Modifier.fillMaxWidth(),
                        backgroundColor = CanvasBg,
                        shadowOffset = 2.dp,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Slot #$selectedSlotId Assignment", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MutedText)
                                Text(currentName, fontSize = 14.sp, fontWeight = FontWeight.Black, color = Ink)
                                if (currentCode.isNotBlank()) {
                                    Text("Dial: $currentCode", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = GuardianBlue)
                                }
                            }

                            if (currentName != "Unassigned") {
                                TactilePill(
                                    text = "🗑️ CLEAR",
                                    backgroundColor = com.treefort.guardian.theme.ColorDangerRed,
                                    textColor = Color.White,
                                    shadowOffset = 1.dp,
                                    onClick = {
                                        speedDials = speedDials.map {
                                            if (it.id == selectedSlotId) it.copy(contactName = "Unassigned", dialCode = "") else it
                                        }
                                        Toast.makeText(context, "Cleared Slot #$selectedSlotId", Toast.LENGTH_SHORT).show()
                                    }
                                )
                            }
                        }
                    }

                    // Pick From Friends Button
                    TactileButton(
                        text = "👥 Pick From Friends List",
                        backgroundColor = com.treefort.guardian.theme.ColorPrimaryBlue,
                        textColor = Color.White,
                        onClick = { showFriendPickerSheet = true },
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Bottom Cancel & Done buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TactileButton(
                            text = "Cancel",
                            backgroundColor = CardBg,
                            textColor = Ink,
                            onClick = { showSpeedDialsDialog = false },
                            modifier = Modifier.weight(1f)
                        )
                        TactileButton(
                            text = "Done",
                            backgroundColor = com.treefort.guardian.theme.ColorSafeGreen,
                            textColor = Ink,
                            onClick = {
                                Toast.makeText(context, "Saved speed dials for ${stations[selectedStationIndex].name}!", Toast.LENGTH_SHORT).show()
                                showSpeedDialsDialog = false
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // Friend Picker Dialog
        if (showFriendPickerSheet) {
            Dialog(onDismissRequest = { showFriendPickerSheet = false }) {
                TactileCard(
                    modifier = Modifier.fillMaxWidth(),
                    backgroundColor = CardBg,
                    shadowOffset = 6.dp,
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text("Select Friend for Slot #$selectedSlotId", fontWeight = FontWeight.Black, fontSize = 16.sp, color = Ink)
                        Text("Pick a friend from your verified contacts list:", fontSize = 11.5.sp, color = MutedText)

                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            candidateFriends.forEach { friend ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(CanvasBg, RoundedCornerShape(8.dp))
                                        .border(1.dp, Ink, RoundedCornerShape(8.dp))
                                        .clickable {
                                            speedDials = speedDials.map {
                                                if (it.id == selectedSlotId) it.copy(contactName = friend.name, dialCode = friend.phone ?: "") else it
                                            }
                                            Toast.makeText(context, "Assigned ${friend.name} to #${selectedSlotId}!", Toast.LENGTH_SHORT).show()
                                            showFriendPickerSheet = false
                                        }
                                        .padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Box(
                                            modifier = Modifier
                                                .size(32.dp)
                                                .background(parseHexColor(friend.avatarBg), CircleShape)
                                                .border(1.dp, Ink, CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(friend.avatarEmoji ?: "⚽", fontSize = 15.sp)
                                        }
                                        Column {
                                            Text(friend.name, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Ink)
                                            Text(friend.phone ?: "", fontSize = 11.sp, color = MutedText)
                                        }
                                    }
                                    TactilePill(
                                        text = "SELECT",
                                        backgroundColor = com.treefort.guardian.theme.ColorSafeGreen,
                                        textColor = Ink,
                                        shadowOffset = 1.dp
                                    )
                                }
                            }
                        }

                        TactileButton(
                            text = "Back",
                            backgroundColor = CardBg,
                            textColor = Ink,
                            onClick = { showFriendPickerSheet = false },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Settings",
                fontSize = 20.sp,
                fontWeight = FontWeight.Black,
                color = Ink
            )

            TactilePill(
                text = "🏨 HOTEL WI-FI",
                backgroundColor = SunburstAmber,
                textColor = Ink,
                shadowOffset = 1.dp,
                onClick = { showHotelWifiDialog = true }
            )
        }

        // Active Profile Switcher Banner (Parent vs Extended Family Caller)
        val isDebug = (context.applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0
        if (isDebug) {
            TactileCard(
                modifier = Modifier.fillMaxWidth(),
                backgroundColor = if (AndroidProfileManager.activeRole.hasStation) CanvasBg else com.treefort.guardian.theme.ColorYellowPale,
                shadowOffset = 3.dp,
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TactilePill(
                            text = "🧪 TEST PREVIEW · REMOVE BEFORE LAUNCH",
                            backgroundColor = com.treefort.guardian.theme.ColorYellowPale,
                            textColor = Ink,
                            shadowOffset = 0.dp
                        )

                        TactilePill(
                            text = if (AndroidProfileManager.activeRole.hasStation) "STATION ADMIN" else "CALLER ONLY",
                            backgroundColor = if (AndroidProfileManager.activeRole.hasStation) com.treefort.guardian.theme.ColorSafeGreen else com.treefort.guardian.theme.ColorPrimaryBlue,
                            textColor = if (AndroidProfileManager.activeRole.hasStation) Ink else Color.White,
                            shadowOffset = 1.dp
                        )
                    }

                    Column {
                        Text("ACTIVE PROFILE MODE", fontSize = 10.sp, fontWeight = FontWeight.Black, color = MutedText)
                        Text(AndroidProfileManager.activeRole.title, fontSize = 14.sp, fontWeight = FontWeight.Black, color = Ink)
                    }

                    Text(AndroidProfileManager.activeRole.subtitle, fontSize = 11.5.sp, color = MutedText)

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf(ProfileRole.PARENT_TWO_STATIONS, ProfileRole.CO_PARENT_TWO_HOMES).forEach { role ->
                                val isSelected = AndroidProfileManager.activeRole == role
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .background(if (isSelected) Ink else CardBg, RoundedCornerShape(8.dp))
                                        .border(1.5.dp, Ink, RoundedCornerShape(8.dp))
                                        .clickable {
                                            AndroidProfileManager.activeRole = role
                                            AndroidProfileManager.phoneTreeFortNumber = "*10001"
                                            val dial = AndroidProfileManager.phoneTreeFortNumber
                                            WebRTCManager.connectSignaling(dialCode = dial)
                                            context.getSharedPreferences("treefort_prefs", android.content.Context.MODE_PRIVATE)
                                                .edit().putString("active_role", role.name).apply()
                                            Toast.makeText(context, "Switched to ${role.title}", Toast.LENGTH_SHORT).show()
                                        }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = if (role == ProfileRole.PARENT_TWO_STATIONS) "1 Home (2 Stations)" else "Co-Parent (2 Homes)",
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.Black,
                                        color = if (isSelected) Color.White else Ink
                                    )
                                }
                            }
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(if (AndroidProfileManager.activeRole == ProfileRole.FAMILY_CALLER_ONLY) Ink else CardBg, RoundedCornerShape(8.dp))
                                .border(1.5.dp, Ink, RoundedCornerShape(8.dp))
                                .clickable {
                                    AndroidProfileManager.activeRole = ProfileRole.FAMILY_CALLER_ONLY
                                    AndroidProfileManager.phoneTreeFortNumber = "*38912"
                                    WebRTCManager.connectSignaling(dialCode = "*38912")
                                    context.getSharedPreferences("treefort_prefs", android.content.Context.MODE_PRIVATE)
                                        .edit().putString("active_role", ProfileRole.FAMILY_CALLER_ONLY.name).apply()
                                    Toast.makeText(context, "Switched to Uncle Bob (Caller Only)", Toast.LENGTH_SHORT).show()
                                }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Uncle Bob (Extended Family Caller · No Station)",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                color = if (AndroidProfileManager.activeRole == ProfileRole.FAMILY_CALLER_ONLY) Color.White else Ink
                            )
                        }
                    }
                }
            }
        }

        // Overnight Relocation Assistant Card
        TactileCard(
            modifier = Modifier.fillMaxWidth(),
            backgroundColor = if (AndroidProfileManager.isTemporaryMoveActive) com.treefort.guardian.theme.ColorYellowPale else CardBg,
            shadowOffset = 3.dp,
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("Overnight Relocation", fontSize = 14.sp, fontWeight = FontWeight.Black, color = Ink)
                            if (AndroidProfileManager.isTemporaryMoveActive) {
                                Text(
                                    text = "ACTIVE",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Black,
                                    color = com.treefort.guardian.theme.ColorDangerRed,
                                    modifier = Modifier
                                        .background(com.treefort.guardian.theme.ColorWarningYellow, RoundedCornerShape(4.dp))
                                        .padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        }
                        Text("Taking phone to Uncle Bob's or Grandma's for a sleepover", fontSize = 11.5.sp, color = MutedText)
                    }

                    Box(
                        modifier = Modifier
                            .border(1.5.dp, Ink, RoundedCornerShape(16.dp))
                            .padding(horizontal = 2.dp)
                    ) {
                        Switch(
                            checked = AndroidProfileManager.isTemporaryMoveActive,
                            onCheckedChange = { AndroidProfileManager.isTemporaryMoveActive = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = com.treefort.guardian.theme.ColorWarningYellow
                            )
                        )
                    }
                }

                if (AndroidProfileManager.isTemporaryMoveActive) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(CanvasBg, RoundedCornerShape(8.dp))
                            .border(1.dp, Ink, RoundedCornerShape(8.dp))
                            .padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("🏠 Host: ${AndroidProfileManager.tempMoveHostName}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Ink)
                            Text("⏳ Auto-Returns: ${AndroidProfileManager.tempMoveReturnDate}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = ForestGreen)
                        }
                        Text("📍 Temporary E911: ${AndroidProfileManager.tempMoveAddress}", fontSize = 11.sp, color = MutedText)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "✅ Temporary Wi-Fi & local E911 dispatch active.",
                                fontSize = 11.sp,
                                color = Ink,
                                modifier = Modifier.weight(1f)
                            )
                            TactilePill(
                                text = "EDIT MOVE",
                                icon = "✏️",
                                backgroundColor = SunburstAmber,
                                textColor = Ink,
                                shadowOffset = 1.dp,
                                onClick = { showEditOvernightMoveDialog = true }
                            )
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Preset family locations and custom schedule ready", fontSize = 11.sp, color = MutedText)
                        TactilePill(
                            text = "EDIT MOVE",
                            icon = "✏️",
                            backgroundColor = CardBg,
                            textColor = Ink,
                            shadowOffset = 1.dp,
                            onClick = { showEditOvernightMoveDialog = true }
                        )
                    }
                }
            }
        }

        // Section 1: Household TreeForts (Only if active role has physical stations)
        if (AndroidProfileManager.activeRole.hasStation) {
            TactileCard(
                modifier = Modifier.fillMaxWidth(),
            backgroundColor = CardBg,
            shadowOffset = 3.dp,
            shape = RoundedCornerShape(14.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Household TreeForts",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Black,
                            color = Ink
                        )
                        Text(
                            text = "${stations.size} active station${if (stations.size == 1) "" else "s"} in this home",
                            fontSize = 12.sp,
                            color = MutedText
                        )
                    }

                    TactilePill(
                        text = "ADD TREEFORT",
                        icon = "➕",
                        backgroundColor = com.treefort.guardian.theme.ColorSafeGreen,
                        textColor = Ink,
                        shadowOffset = 1.dp,
                        onClick = { showAddTreeFortDialog = true }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    stations.forEach { station ->
                        val isSelected = station.name == stations[selectedStationIndex].name
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(CanvasBg, RoundedCornerShape(8.dp))
                                .border(1.dp, Ink, RoundedCornerShape(8.dp))
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(if (isSelected) SunburstAmber else com.treefort.guardian.theme.ColorSafeGreen, CircleShape)
                                        .border(1.5.dp, Ink, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("🏠", fontSize = 16.sp)
                                }
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Text(station.name, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Ink)
                                        if (isSelected) {
                                            Text(
                                                text = "SELECTED",
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Black,
                                                color = Ink,
                                                modifier = Modifier
                                                    .background(SunburstAmber, RoundedCornerShape(4.dp))
                                                    .padding(horizontal = 4.dp, vertical = 1.dp)
                                            )
                                        }
                                        if (AndroidProfileManager.activeRole.isCoParentDualHome) {
                                            Text(
                                                text = station.homeLabel,
                                                fontSize = 9.5.sp,
                                                fontWeight = FontWeight.Black,
                                                color = GuardianBlue,
                                                modifier = Modifier
                                                    .background(com.treefort.guardian.theme.ColorBluePowder, RoundedCornerShape(4.dp))
                                                    .padding(horizontal = 4.dp, vertical = 1.dp)
                                            )
                                        }
                                    }
                                    Text("Number: ${station.number} · Room: ${station.room}", fontSize = 11.sp, color = MutedText)
                                }
                            }
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                TactilePill(
                                    text = if (isSelected) "ACTIVE" else "SWITCH",
                                    icon = if (isSelected) null else "🔄",
                                    backgroundColor = if (isSelected) com.treefort.guardian.theme.ColorYellowPale else CanvasBg,
                                    textColor = Ink,
                                    shadowOffset = 1.dp,
                                    onClick = {
                                        val idx = stations.indexOf(station)
                                        if (idx >= 0) selectedStationIndex = idx
                                    }
                                )
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .background(if (station.isOnline) com.treefort.guardian.theme.ColorSafeGreen else com.treefort.guardian.theme.ColorWarningYellow, CircleShape)
                                        .border(1.5.dp, Ink, CircleShape)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

        // Section 2: Manage E911 Location (Better Address Formatting)
        TactileCard(
            modifier = Modifier.fillMaxWidth(),
            backgroundColor = CardBg,
            shadowOffset = 3.dp,
            shape = RoundedCornerShape(14.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "E911 Location",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Black,
                            color = Ink
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(e911AddressStreet, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Ink)
                        Text(e911AddressCityStateZip, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = MutedText)
                        Text(
                            text = "Verified FCC/CRTC Emergency Dispatch Address",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = ForestGreen
                        )
                    }

                    TactilePill(
                        text = "MANAGE",
                        icon = "✏️",
                        backgroundColor = com.treefort.guardian.theme.ColorBluePowder,
                        textColor = Ink,
                        shadowOffset = 1.dp,
                        onClick = { showE911Dialog = true }
                    )
                }
            }
        }

        // Section 3: Add and Manage Guardians (Showing Guardians List directly)
        TactileCard(
            modifier = Modifier.fillMaxWidth(),
            backgroundColor = CardBg,
            shadowOffset = 3.dp,
            shape = RoundedCornerShape(14.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Household Guardians",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Black,
                            color = Ink
                        )
                        Text(
                            text = "${guardians.size} approved guardians",
                            fontSize = 12.sp,
                            color = MutedText
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .background(com.treefort.guardian.theme.ColorBluePowder, RoundedCornerShape(6.dp))
                                .border(1.dp, Ink, RoundedCornerShape(6.dp))
                                .clickable { showCoParentDialog = true }
                                .padding(horizontal = 7.dp, vertical = 5.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("❓", fontSize = 11.sp)
                                Text("INFO", fontSize = 10.sp, fontWeight = FontWeight.Black, color = GuardianBlue)
                            }
                        }

                        TactilePill(
                            text = "MANAGE",
                            icon = "✏️",
                            backgroundColor = com.treefort.guardian.theme.ColorYellowPale,
                            textColor = Ink,
                            shadowOffset = 1.dp,
                            onClick = { showGuardiansDialog = true }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    guardians.forEach { guardian ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(CanvasBg, RoundedCornerShape(8.dp))
                                .border(1.dp, Ink, RoundedCornerShape(8.dp))
                                .padding(8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .background(parseHexColor(guardian.avatarBg), CircleShape)
                                        .border(1.5.dp, Ink, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(guardian.avatarEmoji, fontSize = 15.sp)
                                }
                                Column {
                                    Text(guardian.name, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Ink)
                                    Text(guardian.role, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MutedText)
                                    if (AndroidProfileManager.activeRole.isCoParentDualHome) {
                                        Row(
                                            modifier = Modifier.padding(top = 2.dp),
                                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text("📍 ${guardian.assignedHome}", fontSize = 9.5.sp, fontWeight = FontWeight.Black, color = GuardianBlue)
                                            Text("•", fontSize = 9.5.sp, color = MutedText)
                                            Text("🎛️ ${guardian.stationAccess}", fontSize = 9.5.sp, fontWeight = FontWeight.Bold, color = Ink)
                                        }
                                    }
                                }
                            }
                            Text("🛡️ Active", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = ForestGreen)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                TactileButton(
                    text = "🤝 Co-Parenting & Family Dynamic Safety Rules",
                    backgroundColor = com.treefort.guardian.theme.ColorYellowPale,
                    textColor = Ink,
                    onClick = { showCoParentDialog = true },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // Section 4: Edit Speed Dials (#1 – #9 Thematic Grid) - Only if active role has physical station
        if (AndroidProfileManager.activeRole.hasStation) {
            TactileCard(
                modifier = Modifier.fillMaxWidth(),
                backgroundColor = CardBg,
                shadowOffset = 3.dp,
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Keypad Speed Dials",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Black,
                            color = Ink
                        )
                        Text(
                            text = "Fast 1-touch dial keys on phone keypad",
                            fontSize = 12.sp,
                            color = MutedText
                        )
                    }

                    TactilePill(
                        text = "EDIT KEYPAD",
                        icon = "✏️",
                        backgroundColor = com.treefort.guardian.theme.ColorRetroGrape,
                        textColor = Color.White,
                        shadowOffset = 1.dp,
                        onClick = { showSpeedDialsDialog = true }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Display 3 columns of thematic speed dial keys
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    speedDials.chunked(3).forEach { rowDials ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            rowDials.forEach { item ->
                                Row(
                                    modifier = Modifier
                                        .weight(1f)
                                        .background(CanvasBg, RoundedCornerShape(6.dp))
                                        .border(1.dp, Ink, RoundedCornerShape(6.dp))
                                        .padding(6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .background(
                                                if (item.contactName == "Unassigned") CanvasBg else com.treefort.guardian.theme.ColorWarningYellow,
                                                RoundedCornerShape(4.dp)
                                            )
                                            .border(1.dp, Ink, RoundedCornerShape(4.dp))
                                            .padding(horizontal = 5.dp, vertical = 2.dp)
                                    ) {
                                        Text(item.slot, fontSize = 10.5.sp, fontWeight = FontWeight.Black, color = Ink)
                                    }
                                    Text(
                                        text = item.contactName,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (item.contactName == "Unassigned") MutedText else Ink,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

        // Section 5: Network & E911 Safety Subscription
        TactileCard(
            modifier = Modifier.fillMaxWidth(),
            backgroundColor = CardBg,
            shadowOffset = 3.dp,
            shape = RoundedCornerShape(14.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "TreeFort Network Subscription",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Black,
                            color = Ink
                        )
                        Text(
                            text = "WebRTC signaling & FCC E911 dispatch",
                            fontSize = 12.sp,
                            color = MutedText
                        )
                    }

                    TactilePill(
                        text = "ACTIVE",
                        backgroundColor = Color(0xFFD1F2D9),
                        textColor = ForestGreen,
                        shadowOffset = 1.dp
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(PillBg, RoundedCornerShape(10.dp))
                        .border(1.5.dp, Ink, RoundedCornerShape(10.dp))
                        .padding(12.dp)
                ) {
                    Text("Plan: Household Unlimited", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Ink)
                    Text("$49.00 / year · Billed via Stripe", fontSize = 12.sp, color = MutedText)
                    Text("Next Renewal: November 14, 2027", fontSize = 11.sp, color = GuardianBlue, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(10.dp))

                TactileButton(
                    text = "Manage Subscription",
                    backgroundColor = CardBg,
                    textColor = Ink,
                    onClick = {
                        Toast.makeText(context, "Opening Stripe Customer Portal...", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // Section 6: Station Hardware & Audio Volume (Only if managing physical station)
        if (AndroidProfileManager.activeRole.hasStation) {
            TactileCard(
                modifier = Modifier.fillMaxWidth(),
                backgroundColor = CardBg,
                shadowOffset = 3.dp,
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Station Hardware & Audio",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Black,
                        color = Ink
                    )
                    Text(
                        text = "Kid's Bedroom Station",
                        fontSize = 11.5.sp,
                        color = MutedText
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Handset Volume
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Handset Ear Volume", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Ink)
                            Text("${(handsetVol * 100).toInt()}%", fontSize = 12.sp, fontWeight = FontWeight.Black, color = GuardianBlue)
                        }
                        Slider(
                            value = handsetVol,
                            onValueChange = { handsetVol = it },
                            colors = SliderDefaults.colors(
                                thumbColor = GuardianBlue,
                                activeTrackColor = GuardianBlue,
                                inactiveTrackColor = Color(0xFFE5E7EB)
                            )
                        )
                    }

                    // Ringer Bell Volume
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Physical Ringer Bell Volume", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Ink)
                            Text("${(ringerVol * 100).toInt()}%", fontSize = 12.sp, fontWeight = FontWeight.Black, color = ForestGreen)
                        }
                        Slider(
                            value = ringerVol,
                            onValueChange = { ringerVol = it },
                            colors = SliderDefaults.colors(
                                thumbColor = ForestGreen,
                                activeTrackColor = ForestGreen,
                                inactiveTrackColor = Color(0xFFE5E7EB)
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Bedtime Curfew Switch
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(PillBg, RoundedCornerShape(10.dp))
                            .border(1.5.dp, Ink, RoundedCornerShape(10.dp))
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Bedtime Quiet Hours Curfew", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Ink)
                            Text("Silence bells 8:30 PM – 7:00 AM", fontSize = 11.sp, color = MutedText)
                        }
                        Switch(
                            checked = curfewEnabled,
                            onCheckedChange = { curfewEnabled = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = TreeFortGreen,
                                uncheckedThumbColor = Ink,
                                uncheckedTrackColor = Color(0xFFE5E7EB)
                            )
                        )
                    }
                }
            }
        }

        // Section: Boring Text Mode (Persistent Across Sessions)
        TactileCard(
            modifier = Modifier.fillMaxWidth(),
            backgroundColor = CardBg,
            shadowOffset = 3.dp,
            shape = RoundedCornerShape(14.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Boring Text Mode", fontSize = 15.sp, fontWeight = FontWeight.Black, color = Ink)
                        Text(
                            text = "Simpler, less whimsical text for easier reading across the app",
                            fontSize = 11.5.sp,
                            color = MutedText
                        )
                    }

                    Switch(
                        checked = boringTextEnabled,
                        onCheckedChange = { checked ->
                            boringTextEnabled = checked
                            com.treefort.guardian.theme.ThemeConfig.isBoringTextEnabled = checked
                            prefs.edit { putBoolean("boring_text_enabled", checked) }
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = GuardianBlue
                        )
                    )
                }

                // Quick Typographic Comparison Specimen (Fun vs Boring)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Playful Sample
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .background(if (boringTextEnabled) CanvasBg else com.treefort.guardian.theme.ColorYellowPale, RoundedCornerShape(6.dp))
                            .border(if (boringTextEnabled) 1.dp else 1.5.dp, Ink, RoundedCornerShape(6.dp))
                            .padding(8.dp)
                    ) {
                        Text("PLAYFUL", fontSize = 9.sp, fontWeight = FontWeight.Black, color = MutedText)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text("TreeFort Club", fontSize = 14.sp, fontWeight = FontWeight.Black, color = Ink)
                    }

                    // Boring Sample
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .background(if (boringTextEnabled) com.treefort.guardian.theme.ColorYellowPale else CanvasBg, RoundedCornerShape(6.dp))
                            .border(if (boringTextEnabled) 1.5.dp else 1.dp, Ink, RoundedCornerShape(6.dp))
                            .padding(8.dp)
                    ) {
                        Text("BORING", fontSize = 9.sp, fontWeight = FontWeight.Black, color = MutedText)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text("TreeFort Club", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Ink)
                    }
                }
            }
        }

        // Section 6: Phone System Health (Simple Stoplights)
        TactileCard(
            modifier = Modifier.fillMaxWidth(),
            backgroundColor = CardBg,
            shadowOffset = 3.dp,
            shape = RoundedCornerShape(14.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Phone System Health",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Black,
                    color = Ink
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(CanvasBg, RoundedCornerShape(8.dp))
                        .border(1.dp, Ink, RoundedCornerShape(8.dp))
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Wi-Fi Access Stoplight
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .background(com.treefort.guardian.theme.ColorSafeGreen, CircleShape)
                                    .border(1.5.dp, Ink, CircleShape)
                            )
                            Text("Wi-Fi Access", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Ink)
                        }
                        Text("CONNECTED (5 GHz / 2.4 GHz)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = ForestGreen)
                    }

                    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Ink.copy(alpha = 0.1f)))

                    // Network Access Stoplight
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .background(com.treefort.guardian.theme.ColorSafeGreen, CircleShape)
                                    .border(1.5.dp, Ink, CircleShape)
                            )
                            Text("Network Access", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Ink)
                        }
                        Text("CONNECTED (WebRTC Hub)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = ForestGreen)
                    }

                    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Ink.copy(alpha = 0.1f)))

                    // System Status Stoplight
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .background(com.treefort.guardian.theme.ColorSafeGreen, CircleShape)
                                    .border(1.5.dp, Ink, CircleShape)
                            )
                            Text("System Status", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Ink)
                        }
                        Text("ACTIVE & READY", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = ForestGreen)
                    }

                    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Ink.copy(alpha = 0.1f)))

                    // Signaling Gateway & Host Selector
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Box(
                                    modifier = Modifier
                                        .size(12.dp)
                                        .background(if (isOnline) com.treefort.guardian.theme.ColorSafeGreen else com.treefort.guardian.theme.ColorWarningYellow, CircleShape)
                                        .border(1.5.dp, Ink, CircleShape)
                                )
                                Text("Signaling Gateway", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Ink)
                            }
                            Text(if (isOnline) "CONNECTED" else "RECONNECTING", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (isOnline) ForestGreen else SunburstAmber)
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Host: ${WebRTCManager.signalingHost}", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = Ink)

                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Box(
                                    modifier = Modifier
                                        .background(SunburstAmber, RoundedCornerShape(4.dp))
                                        .border(1.dp, Ink, RoundedCornerShape(4.dp))
                                        .clickable {
                                            WebRTCManager.signalingHost = "192.168.111.115:8000"
                                            val dial = if (AndroidProfileManager.activeRole == ProfileRole.FAMILY_CALLER_ONLY) "*38912" else AndroidProfileManager.phoneTreeFortNumber
                                            WebRTCManager.connectSignaling(dialCode = dial)
                                            Toast.makeText(context, "Connected to LAN 192.168.111.115:8000", Toast.LENGTH_SHORT).show()
                                        }
                                        .padding(horizontal = 6.dp, vertical = 3.dp)
                                ) {
                                    Text("LAN (192.168.111.115)", fontSize = 9.5.sp, fontWeight = FontWeight.Black, color = Ink)
                                }

                                Box(
                                    modifier = Modifier
                                        .background(PillBg, RoundedCornerShape(4.dp))
                                        .border(1.dp, Ink, RoundedCornerShape(4.dp))
                                        .clickable {
                                            WebRTCManager.signalingHost = "10.0.2.2:8000"
                                            val dial = if (AndroidProfileManager.activeRole == ProfileRole.FAMILY_CALLER_ONLY) "*38912" else AndroidProfileManager.phoneTreeFortNumber
                                            WebRTCManager.connectSignaling(dialCode = dial)
                                            Toast.makeText(context, "Connected to Emulator 10.0.2.2:8000", Toast.LENGTH_SHORT).show()
                                        }
                                        .padding(horizontal = 6.dp, vertical = 3.dp)
                                ) {
                                    Text("Emulator (10.0.2.2)", fontSize = 9.5.sp, fontWeight = FontWeight.Black, color = Ink)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Section 7: System Version (Clean Display)
        TactileCard(
            modifier = Modifier.fillMaxWidth(),
            backgroundColor = CanvasBg,
            shadowOffset = 2.dp,
            shape = RoundedCornerShape(10.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val context = androidx.compose.ui.platform.LocalContext.current
                val pkgInfo = remember(context) {
                    try {
                        context.packageManager.getPackageInfo(context.packageName, 0)
                    } catch (e: Exception) {
                        null
                    }
                }
                val vName = pkgInfo?.versionName ?: "0.0.1"
                val vCode = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                    pkgInfo?.longVersionCode ?: 1L
                } else {
                    @Suppress("DEPRECATION")
                    (pkgInfo?.versionCode?.toLong() ?: 1L)
                }
                Text(
                    text = "TreeFort System v$vName ($vCode)",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = com.treefort.guardian.theme.ColorSlateMid
                )
                TactilePill(
                    text = "UP TO DATE",
                    backgroundColor = com.treefort.guardian.theme.ColorGreenMint,
                    textColor = com.treefort.guardian.theme.ColorGreenForest,
                    shadowOffset = 1.dp
                )
            }
        }

        // Section: Anti-Kid Code Lock (At bottom of menu)
        TactileCard(
            modifier = Modifier.fillMaxWidth(),
            backgroundColor = CardBg,
            shadowOffset = 3.dp,
            shape = RoundedCornerShape(14.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Anti-Kid Code Lock", fontSize = 15.sp, fontWeight = FontWeight.Black, color = Ink)
                        Text(
                            text = if (isPinLockEnabled) "Requires PIN to edit contacts & settings" else "Unlocked · Anyone can make changes",
                            fontSize = 12.sp,
                            color = MutedText
                        )
                    }

                    Switch(
                        checked = isPinLockEnabled,
                        onCheckedChange = { checked ->
                            if (checked) {
                                showPinDialog = true
                            } else {
                                isPinLockEnabled = false
                            }
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = com.treefort.guardian.theme.ColorWarningYellow
                        )
                    )
                }

                if (isPinLockEnabled) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(CanvasBg, RoundedCornerShape(6.dp))
                            .border(1.dp, Ink, RoundedCornerShape(6.dp))
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("PIN: • • • • (Active · 4–8 digits)", fontSize = 11.sp, fontWeight = FontWeight.Black, color = ForestGreen)
                        TactilePill(
                            text = "CHANGE PIN",
                            icon = "✏️",
                            backgroundColor = com.treefort.guardian.theme.ColorBluePowder,
                            textColor = Ink,
                            shadowOffset = 1.dp,
                            onClick = { showPinDialog = true }
                        )
                    }
                }
            }
        }

        // Section: Privacy Policy Link
        TactileButton(
            text = "🔒 PRIVACY POLICY (treefort.kids/privacy)",
            backgroundColor = CardBg,
            textColor = Ink,
            onClick = {
                val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse("https://treefort.kids/privacy"))
                context.startActivity(intent)
            },
            modifier = Modifier.fillMaxWidth(),
            fontSize = 13.sp
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Section: Delete Guardian Account Button
        TactileButton(
            text = "⚠️ DELETE GUARDIAN ACCOUNT",
            backgroundColor = CardBg,
            textColor = com.treefort.guardian.theme.ColorDangerRed,
            onClick = {
                deleteAccountAckStep = 1
                deleteCheck1 = false
                deleteCheck2 = false
                deleteConfirmText = ""
                showDeleteAccountDialog = true
            },
            modifier = Modifier.fillMaxWidth(),
            fontSize = 13.sp
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Section: Tactile Logout Button
        TactileButton(
            text = "🚪 LOG OUT OF GUARDIAN",
            backgroundColor = CardBg,
            textColor = com.treefort.guardian.theme.ColorDangerRed,
            onClick = { showLogoutDialog = true },
            modifier = Modifier.fillMaxWidth(),
            fontSize = 14.sp
        )

        if (showLogoutDialog) {
            Dialog(onDismissRequest = { showLogoutDialog = false }) {
                TactileCard(
                    modifier = Modifier.fillMaxWidth(0.92f),
                    backgroundColor = CardBg,
                    shadowOffset = 5.dp,
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text("Log Out of TreeFort?", fontSize = 18.sp, fontWeight = FontWeight.Black, color = Ink)
                        Text(
                            text = "Are you sure you want to log out of TreeFort Guardian on this device?",
                            fontSize = 13.sp,
                            color = MutedText
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            TactileButton(
                                text = "Cancel",
                                backgroundColor = PillBg,
                                textColor = Ink,
                                onClick = { showLogoutDialog = false },
                                modifier = Modifier.weight(1f),
                                fontSize = 13.sp
                            )
                            TactileButton(
                                text = "Log Out",
                                backgroundColor = com.treefort.guardian.theme.ColorDangerRed,
                                textColor = Color.White,
                                onClick = {
                                    showLogoutDialog = false
                                    AndroidProfileManager.isLoggedIn = false
                                    Toast.makeText(context, "Logged out successfully", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.weight(1f),
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }
        }

        if (showDeleteAccountDialog) {
            Dialog(onDismissRequest = { showDeleteAccountDialog = false }) {
                TactileCard(
                    modifier = Modifier.fillMaxWidth(0.96f),
                    backgroundColor = CardBg,
                    shadowOffset = 5.dp,
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("⚠️", fontSize = 22.sp)
                            Text(
                                text = if (deleteAccountAckStep == 1) "Delete Account (Step 1 of 2)" else "Confirm Erasure (Step 2 of 2)",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Black,
                                color = com.treefort.guardian.theme.ColorDangerRed
                            )
                        }

                        if (deleteAccountAckStep == 1) {
                            Text(
                                text = "30-DAY TOTAL ACCOUNT ERASURE POLICY",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                color = com.treefort.guardian.theme.ColorDangerRed
                            )

                            Text(
                                text = "Upon confirmation, all household data, base station pairings (*14185, *14186), safe contacts, call logs, and credentials will be scheduled for permanent purging in 30 days. You will be signed out immediately.",
                                fontSize = 12.sp,
                                color = Ink,
                                lineHeight = 16.sp
                            )

                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { deleteCheck1 = !deleteCheck1 }
                                ) {
                                    Checkbox(
                                        checked = deleteCheck1,
                                        onCheckedChange = { deleteCheck1 = it },
                                        colors = CheckboxDefaults.colors(checkedColor = com.treefort.guardian.theme.ColorDangerRed)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "I understand all stations and contacts will be permanently purged in 30 days.",
                                        fontSize = 11.sp,
                                        color = Ink
                                    )
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { deleteCheck2 = !deleteCheck2 }
                                ) {
                                    Checkbox(
                                        checked = deleteCheck2,
                                        onCheckedChange = { deleteCheck2 = it },
                                        colors = CheckboxDefaults.colors(checkedColor = com.treefort.guardian.theme.ColorDangerRed)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "I acknowledge that this action cannot be undone once confirmed.",
                                        fontSize = 11.sp,
                                        color = Ink
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                TactileButton(
                                    text = "Cancel",
                                    backgroundColor = PillBg,
                                    textColor = Ink,
                                    onClick = { showDeleteAccountDialog = false },
                                    modifier = Modifier.weight(1f),
                                    fontSize = 13.sp
                                )
                                TactileButton(
                                    text = "Continue",
                                    backgroundColor = if (deleteCheck1 && deleteCheck2) com.treefort.guardian.theme.ColorDangerRed else PillBg,
                                    textColor = if (deleteCheck1 && deleteCheck2) Color.White else MutedText,
                                    onClick = {
                                        if (deleteCheck1 && deleteCheck2) {
                                            deleteAccountAckStep = 2
                                        }
                                    },
                                    modifier = Modifier.weight(1f),
                                    fontSize = 13.sp
                                )
                            }
                        } else {
                            Text(
                                text = "FINAL SAFETY CONFIRMATION",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                color = com.treefort.guardian.theme.ColorDangerRed
                            )

                            Text(
                                text = "Type DELETE below to confirm that your account and all associated household records should be scheduled for permanent erasure in 30 days.",
                                fontSize = 12.sp,
                                color = Ink
                            )

                            OutlinedTextField(
                                value = deleteConfirmText,
                                onValueChange = { deleteConfirmText = it },
                                placeholder = { Text("Type DELETE", fontSize = 12.sp) },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                TactileButton(
                                    text = "Back",
                                    backgroundColor = PillBg,
                                    textColor = Ink,
                                    onClick = { deleteAccountAckStep = 1 },
                                    modifier = Modifier.weight(1f),
                                    fontSize = 13.sp
                                )
                                val isConfirmed = deleteConfirmText.trim().uppercase() == "DELETE"
                                TactileButton(
                                    text = "Delete Account",
                                    backgroundColor = if (isConfirmed) com.treefort.guardian.theme.ColorDangerRed else PillBg,
                                    textColor = if (isConfirmed) Color.White else MutedText,
                                    onClick = {
                                        if (isConfirmed) {
                                            showDeleteAccountDialog = false
                                            coroutineScope.launch {
                                                SupabaseManager.scheduleAccountDeletion()
                                                SupabaseManager.signOut()
                                            }
                                            AndroidProfileManager.isLoggedIn = false
                                            Toast.makeText(context, "Account scheduled for deletion. All data will be purged in 30 days.", Toast.LENGTH_LONG).show()
                                        }
                                    },
                                    modifier = Modifier.weight(1f),
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        if (showPinDialog) {
            AntiKidPinDialog(
                onDismiss = { showPinDialog = false },
                onSavePin = { pin ->
                    val digest = java.security.MessageDigest.getInstance("SHA-256")
                        .digest("treefort_pin_v1:$pin".toByteArray(Charsets.UTF_8))
                        .joinToString("") { "%02x".format(it) }
                    prefs.edit {
                        putString("anti_kid_pin_hash", digest)
                        remove("anti_kid_pin")
                    }
                    antiKidPin = pin
                    isPinLockEnabled = true
                    showPinDialog = false
                    Toast.makeText(context, "Anti-Kid PIN lock saved!", Toast.LENGTH_SHORT).show()
                }
            )
        }

        if (showHotelWifiDialog) {
            HotelWifiDialog(
                onDismiss = { showHotelWifiDialog = false },
                onAuthorizeForwarding = {
                    Toast.makeText(context, "Forwarded captive portal token to TreeFort Base Station!", Toast.LENGTH_LONG).show()
                    showHotelWifiDialog = false
                }
            )
        }

        if (showCoParentDialog) {
            CoParentSafetyDialog(
                onDismiss = { showCoParentDialog = false }
            )
        }

        Spacer(modifier = Modifier.height(10.dp))
    }
}

@Composable
fun AntiKidPinDialog(
    onDismiss: () -> Unit,
    onSavePin: (String) -> Unit
) {
    var pinEntered by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        TactileCard(
            modifier = Modifier.fillMaxWidth(),
            backgroundColor = CardBg,
            shadowOffset = 6.dp,
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Guardian Anti-Kid Lock", fontSize = 17.sp, fontWeight = FontWeight.Black, color = Ink)
                        Text("Enter 4-8 digit PIN to protect settings", fontSize = 11.5.sp, color = MutedText)
                    }
                    TactilePill(text = "SECURITY", backgroundColor = com.treefort.guardian.theme.ColorWarningYellow, textColor = Ink, shadowOffset = 1.dp)
                }

                // PIN dots preview (Shows exactly 4 slots for numbers, supporting 4 to 8 digits)
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        (0..3).forEach { index ->
                            Box(
                                modifier = Modifier
                                    .size(44.dp, 50.dp)
                                    .background(CanvasBg, RoundedCornerShape(8.dp))
                                    .border(2.dp, Ink, RoundedCornerShape(8.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                if (index < pinEntered.length) {
                                    Box(
                                        modifier = Modifier
                                            .size(14.dp)
                                            .background(Ink, CircleShape)
                                    )
                                }
                            }
                        }
                    }

                    if (pinEntered.length > 4) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "${pinEntered.length} of 8 digits entered",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = GuardianBlue
                        )
                    } else if (pinEntered.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "${pinEntered.length} of min 4 digits entered",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = MutedText
                        )
                    }
                }

                if (errorMessage.isNotBlank()) {
                    Text(errorMessage, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = com.treefort.guardian.theme.ColorDangerRed)
                }

                // 3x4 Number Keypad
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        listOf("1", "2", "3"),
                        listOf("4", "5", "6"),
                        listOf("7", "8", "9"),
                        listOf("CLR", "0", "⌫")
                    ).forEach { row ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            row.forEach { key ->
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(46.dp)
                                        .background(if (key == "CLR" || key == "⌫") PillBg else CardBg, RoundedCornerShape(8.dp))
                                        .border(1.5.dp, Ink, RoundedCornerShape(8.dp))
                                        .clickable {
                                            when (key) {
                                                "CLR" -> pinEntered = ""
                                                "⌫" -> if (pinEntered.isNotEmpty()) pinEntered = pinEntered.dropLast(1)
                                                else -> if (pinEntered.length < 8) pinEntered += key
                                            }
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = key,
                                        fontSize = if (key.length > 1) 12.sp else 18.sp,
                                        fontWeight = FontWeight.Black,
                                        color = if (key == "CLR") com.treefort.guardian.theme.ColorDangerRed else Ink
                                    )
                                }
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    TactileButton(
                        text = "Cancel",
                        backgroundColor = CardBg,
                        textColor = Ink,
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        fontSize = 12.5.sp
                    )
                    TactileButton(
                        text = "🛡️ Save PIN",
                        backgroundColor = com.treefort.guardian.theme.ColorPrimaryBlue,
                        textColor = Color.White,
                        onClick = {
                            if (pinEntered.length in 4..8) {
                                onSavePin(pinEntered)
                            } else {
                                errorMessage = "PIN must be between 4 and 8 digits"
                            }
                        },
                        modifier = Modifier.weight(1.2f),
                        fontSize = 12.5.sp
                    )
                }
            }
        }
    }
}

@Composable
fun HotelWifiDialog(
    onDismiss: () -> Unit,
    onAuthorizeForwarding: () -> Unit
) {
    var hotelName by remember { mutableStateOf("Marriott Courtyard Downtown") }
    var roomNumber by remember { mutableStateOf("Room 412") }
    var tempStreet by remember { mutableStateOf("110 SW Jefferson Ave") }
    var tempCityStateZip by remember { mutableStateOf("Portland, OR 97201") }
    var isForwarding by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        TactileCard(
            modifier = Modifier.fillMaxWidth(),
            backgroundColor = CardBg,
            shadowOffset = 6.dp,
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Hotel Wi-Fi Assistant", fontSize = 17.sp, fontWeight = FontWeight.Black, color = Ink)
                        Text("Captive Portal Bridge & Temporary E911", fontSize = 11.5.sp, color = MutedText)
                    }
                    TactilePill(text = "VACATION", backgroundColor = SunburstAmber, textColor = Ink, shadowOffset = 1.dp)
                }

                // Explainer Card
                TactileCard(
                    modifier = Modifier.fillMaxWidth(),
                    backgroundColor = PillBg,
                    shadowOffset = 2.dp,
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("How it works:", fontSize = 12.sp, fontWeight = FontWeight.Black, color = Ink)
                        Text("1. Connect your smartphone to the hotel Wi-Fi and accept the browser terms / room number login.", fontSize = 11.sp, color = Ink)
                        Text("2. Tap 'Forward Portal Credentials' below to authorize the headless TreeFort base station with the hotel router.", fontSize = 11.sp, color = Ink)
                        Text("3. We temporarily update the FCC/CRTC E911 dispatch location so emergency calls reach local hotel dispatchers.", fontSize = 11.sp, color = Ink)
                    }
                }

                Text("HOTEL INFORMATION", fontSize = 10.sp, fontWeight = FontWeight.Black, color = Ink)
                OutlinedTextField(
                    value = hotelName,
                    onValueChange = { hotelName = it },
                    label = { Text("Hotel / Resort Name", fontSize = 11.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = roomNumber,
                    onValueChange = { roomNumber = it },
                    label = { Text("Room / Suite Number", fontSize = 11.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Text("TEMPORARY E911 VACATION DISPATCH ADDRESS", fontSize = 10.sp, fontWeight = FontWeight.Black, color = ForestGreen)
                OutlinedTextField(
                    value = tempStreet,
                    onValueChange = { tempStreet = it },
                    label = { Text("Street Address", fontSize = 11.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = tempCityStateZip,
                    onValueChange = { tempCityStateZip = it },
                    label = { Text("City, State, Zip Code", fontSize = 11.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TactileButton(
                        text = "Cancel",
                        backgroundColor = CardBg,
                        textColor = Ink,
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    )
                    TactileButton(
                        text = if (isForwarding) "Forwarding..." else "Forward Portal Access",
                        backgroundColor = com.treefort.guardian.theme.ColorSafeGreen,
                        textColor = Ink,
                        onClick = {
                            isForwarding = true
                            onAuthorizeForwarding()
                        },
                        modifier = Modifier.weight(1.4f)
                    )
                }
            }
        }
    }
}

@Composable
fun CoParentSafetyDialog(
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        TactileCard(
            modifier = Modifier.fillMaxWidth(),
            backgroundColor = CardBg,
            shadowOffset = 6.dp,
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Family Dynamic Protection", fontSize = 17.sp, fontWeight = FontWeight.Black, color = Ink)
                        Text("Equal Guardian Parity & Safeguards", fontSize = 11.5.sp, color = MutedText)
                    }
                    TactilePill(text = "SAFETY FIRST", backgroundColor = com.treefort.guardian.theme.ColorSafeGreen, textColor = Ink, shadowOffset = 1.dp)
                }

                // Why we ask
                TactileCard(
                    modifier = Modifier.fillMaxWidth(),
                    backgroundColor = com.treefort.guardian.theme.ColorYellowPale,
                    shadowOffset = 2.dp,
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Why TreeFort asks about your family setup:", fontSize = 12.sp, fontWeight = FontWeight.Black, color = Ink)
                        Text(
                            "Every family is structured differently — single parents, two-household co-parents, or extended grandparent guardians. TreeFort exists to keep your child safe and connected without putting children in the middle of adult dynamics.",
                            fontSize = 11.sp,
                            color = Ink
                        )
                    }
                }

                // Guardrails
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("CO-PARENTING GUARANTEES", fontSize = 10.sp, fontWeight = FontWeight.Black, color = Ink)

                    listOf(
                        "🛡️ No Single-Parent Lockout" to "Neither verified guardian can remove or revoke the other parent's access, dial code, or emergency speed dial.",
                        "📖 Transparent Shared Audit Log" to "Both households have equal, real-time access to the exact same call logs and contact safelist updates.",
                        "❤️ Safe Whitelist Consensus" to "Any contact added by either verified parent requires mutual notification, preventing unapproved third-party dialing.",
                        "🚨 Emergency Direct Dial Parity" to "Speed dial #1 (Mom) and #2 (Dad) remain permanently accessible to the child from both stations."
                    ).forEach { (headline, body) ->
                        TactileCard(
                            modifier = Modifier.fillMaxWidth(),
                            backgroundColor = CanvasBg,
                            shadowOffset = 1.5.dp,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(headline, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Ink)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(body, fontSize = 10.5.sp, color = MutedText)
                            }
                        }
                    }
                }

                TactileButton(
                    text = "Understood & Close",
                    backgroundColor = com.treefort.guardian.theme.ColorPrimaryBlue,
                    textColor = Color.White,
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
