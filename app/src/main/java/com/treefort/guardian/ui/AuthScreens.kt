package com.treefort.guardian.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.activity.compose.BackHandler
import androidx.compose.material3.HorizontalDivider
import androidx.compose.ui.text.input.ImeAction
import com.treefort.guardian.theme.DividerColor
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.res.stringResource
import com.treefort.guardian.EmailConfirmation
import com.treefort.guardian.R
import kotlinx.coroutines.delay
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.treefort.guardian.AppConfig
import com.treefort.guardian.AppStore
import com.treefort.guardian.OAuthConfig
import com.treefort.guardian.OAuthSignIn
import com.treefort.guardian.errorText
import com.treefort.guardian.theme.CardBg
import com.treefort.guardian.theme.ColorPrimaryBlue
import com.treefort.guardian.theme.ColorSafeGreen
import com.treefort.guardian.theme.Ink
import com.treefort.guardian.theme.LinkColor
import com.treefort.guardian.theme.MutedText
import com.treefort.guardian.theme.PillBg
import com.treefort.guardian.theme.Tf
import com.treefort.guardian.theme.TreeFortTokens
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.Period
import java.time.format.DateTimeParseException

private val S = TreeFortTokens.Space

@Composable
private fun AuthFrame(content: @Composable () -> Unit) {
    Box(
        Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().imePadding(),
        contentAlignment = Alignment.TopCenter,
    ) { content() }
}

@Composable
fun LoadingScreen(text: String = "Loading TreeFort") {
    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text("TreeFort", style = Tf.brand, color = Ink)
        Spacer(Modifier.height(S.Md))
        CircularProgressIndicator(color = Ink, strokeWidth = 3.dp)
        Spacer(Modifier.height(S.Md))
        Text(text, style = Tf.body, color = MutedText)
    }
}

@Composable
fun ConfigMissingScreen() {
    AuthFrame {
        ScreenColumn {
            Text("TreeFort", style = Tf.brand, color = Ink)
            Banner(
                "This build has no TreeFort server settings, so it can't sign in. Build with SUPABASE_URL and SUPABASE_ANON_KEY set (see android/README or local.properties).",
                kind = BannerKind.Danger,
            )
        }
    }
}

@Composable
fun LoadFailedScreen() {
    AuthFrame {
        ScreenColumn {
            Text("TreeFort", style = Tf.brand, color = Ink)
            Banner(AppStore.loadError ?: "Couldn't load your account.", kind = BannerKind.Danger)
            TactileButton("Try again", { AppStore.retryBootstrap() }, Modifier.fillMaxWidth())
            TactileButton("Sign out", { AppStore.signOut() }, Modifier.fillMaxWidth(), backgroundColor = PillBg)
        }
    }
}

private enum class AuthMode { SignIn, SignUp, Code }

/** Supabase Auth is the only identity provider. Three separate screens: Sign in (landing), Create account, Email code. */
@Composable
fun AuthScreen() {
    val scope = rememberCoroutineScope()
    var mode by rememberSaveable { mutableStateOf(AuthMode.SignIn) }
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var code by rememberSaveable { mutableStateOf("") }
    var codeSent by rememberSaveable { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var info by remember { mutableStateOf<String?>(null) }
    // Non-null = the "Check your email" state for this address; unsent = no email was just sent (sign-in hit "not confirmed").
    var checkEmail by rememberSaveable { mutableStateOf<String?>(null) }
    var checkEmailJustSent by rememberSaveable { mutableStateOf(true) }
    val passwordFocus = remember { FocusRequester() }
    var focusPassword by remember { mutableStateOf(false) }

    fun goTo(m: AuthMode) { mode = m; error = null; info = null; codeSent = false; code = ""; AppStore.oauthError = null }

    LaunchedEffect(AppStore.emailConfirmedPending) {
        if (AppStore.emailConfirmedPending) {
            AppStore.emailConfirmedPending = false
            checkEmail = null; goTo(AuthMode.SignIn)
            focusPassword = true
        }
    }
    LaunchedEffect(focusPassword, checkEmail, mode) {
        if (focusPassword && checkEmail == null && mode == AuthMode.SignIn) {
            focusPassword = false
            try { passwordFocus.requestFocus() } catch (_: Exception) { /* field not attached yet; user can tap it */ }
        }
    }
    // Create and Email code return to Sign in.
    BackHandler(enabled = checkEmail == null && mode != AuthMode.SignIn) { goTo(AuthMode.SignIn) }

    fun run(block: suspend () -> Unit) {
        scope.launch { busy = true; error = null; info = null; try { block() } finally { busy = false } }
    }

    checkEmail?.let { addr ->
        AuthFrame {
            ScreenColumn {
                Text("TreeFort", style = Tf.brand, color = Ink)
                CheckEmailCard(addr, justSent = checkEmailJustSent, onConfirmed = {
                    email = addr; password = ""; checkEmail = null; goTo(AuthMode.SignIn); focusPassword = true
                })
            }
        }
        return
    }

    val signIn = { run { AppStore.signIn(email, password).onFailure {
        if (it is EmailConfirmation.NotConfirmed) { checkEmailJustSent = false; checkEmail = email.trim() } else error = it.errorText
    } } }
    val createAccount = {
        if (password.length < 8) { error = "Use at least 8 characters for your password." }
        else run {
            AppStore.signUp(email, password)
                .onSuccess { signedIn -> if (!signedIn) { checkEmailJustSent = true; checkEmail = email.trim() } }
                .onFailure { error = it.errorText }
        }
    }

    AuthFrame {
        ScreenColumn {
            Text("TreeFort", style = Tf.brand, color = Ink)
            Text(
                when (mode) {
                    AuthMode.SignIn -> "Guardian sign-in"
                    AuthMode.SignUp -> "Create your guardian account"
                    AuthMode.Code -> "Sign in with an email code"
                },
                style = Tf.title, color = Ink,
            )
            if (mode == AuthMode.SignUp) LinkText("Already have an account? Sign in", enabled = !busy) { goTo(AuthMode.SignIn) }

            TactileCard {
                Column(verticalArrangement = Arrangement.spacedBy(S.Md)) {
                    TfTextField(
                        email, { email = it }, "Email", keyboardType = KeyboardType.Email, enabled = !busy,
                        imeAction = if (mode == AuthMode.Code) ImeAction.Done else ImeAction.Next,
                    )
                    if (mode != AuthMode.Code) {
                        TfTextField(
                            password, { password = it }, "Password", password = true, enabled = !busy,
                            focusRequester = passwordFocus,
                            supportingText = if (mode == AuthMode.SignUp) "At least 8 characters." else null,
                            imeAction = ImeAction.Done,
                            onImeAction = {
                                if (email.isNotBlank() && password.isNotEmpty() && !busy) {
                                    if (mode == AuthMode.SignIn) signIn() else createAccount()
                                }
                            },
                        )
                    } else if (codeSent) {
                        TfTextField(code, { code = it.filter(Char::isDigit).take(8) }, "Code from your email", keyboardType = KeyboardType.NumberPassword, enabled = !busy)
                    }
                    ErrorText(error)
                    info?.let { Banner(it, kind = BannerKind.Success) }

                    when (mode) {
                        AuthMode.SignIn -> {
                            TactileButton("Sign in", { signIn() }, Modifier.fillMaxWidth(), enabled = email.isNotBlank() && password.isNotEmpty(), loading = busy)
                            LinkText("Email me a sign-in code", enabled = !busy, modifier = Modifier.fillMaxWidth()) { goTo(AuthMode.Code) }
                        }
                        AuthMode.SignUp -> {
                            Text(
                                "Guardian accounts are for adults 18 and over. Kids never sign in: they use the TreeFort phone.",
                                style = Tf.bodySm, color = MutedText,
                            )
                            TactileButton("Create account", { createAccount() }, Modifier.fillMaxWidth(), enabled = email.isNotBlank() && password.isNotEmpty(), loading = busy)
                        }
                        AuthMode.Code -> if (!codeSent) {
                            TactileButton("Send code", {
                                run { AppStore.sendEmailCode(email).onSuccess { codeSent = true; info = "We sent a code to ${email.trim()}." }.onFailure { error = it.errorText } }
                            }, Modifier.fillMaxWidth(), enabled = email.isNotBlank(), loading = busy)
                        } else {
                            TactileButton("Verify", {
                                run { AppStore.verifyEmailCode(email, code).onFailure { error = it.errorText } }
                            }, Modifier.fillMaxWidth(), enabled = code.length >= 6, loading = busy)
                            TactileButton("Send a new code", {
                                run { AppStore.sendEmailCode(email).onSuccess { info = "New code sent." }.onFailure { error = it.errorText } }
                            }, Modifier.fillMaxWidth(), backgroundColor = PillBg, enabled = !busy)
                        }
                    }
                }
            }
            when (mode) {
                AuthMode.SignIn -> {
                    OAuthButtons(busy = busy, run = ::run, onError = { error = it })
                    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(S.Sm)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                            Text("New here? ", style = Tf.body, color = Ink)
                            LinkText("Create an account", enabled = !busy) { goTo(AuthMode.SignUp) }
                        }
                        Text(
                            "Guardian accounts are for adults 18 and over. Kids never sign in: they use the TreeFort phone.",
                            style = Tf.bodySm, color = MutedText, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth(),
                        )
                        PrivacyLink()
                    }
                }
                AuthMode.SignUp -> {
                    OAuthButtons(busy = busy, run = ::run, onError = { error = it })
                    PrivacyLink()
                }
                AuthMode.Code -> {
                    LinkText("Use a password instead", enabled = !busy, modifier = Modifier.fillMaxWidth()) { goTo(AuthMode.SignIn) }
                    PrivacyLink()
                }
            }
        }
    }
}

/** Text link with a 48 dp touch target. */
@Composable
private fun LinkText(text: String, modifier: Modifier = Modifier, enabled: Boolean = true, onClick: () -> Unit) {
    Box(
        modifier.heightIn(min = 48.dp).clickable(enabled = enabled, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Text(text, style = Tf.bodyStrong, color = LinkColor, textAlign = TextAlign.Center) }
}

@Composable
private fun PrivacyLink() {
    val ctx = rememberContext()
    LinkText("Privacy policy", modifier = Modifier.fillMaxWidth()) {
        ctx.startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(AppConfig.PRIVACY_URL)).addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}

/** Shown after a sign-up without a session (email confirmation on) or a sign-in that hit "Email not confirmed". */
@Composable
private fun CheckEmailCard(address: String, justSent: Boolean, onConfirmed: () -> Unit) {
    val scope = rememberCoroutineScope()
    val ctx = rememberContext()
    var lastSentAt by rememberSaveable { mutableLongStateOf(if (justSent) System.currentTimeMillis() else 0L) }
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var info by remember { mutableStateOf<String?>(null) }
    val remaining = EmailConfirmation.cooldownRemaining(lastSentAt, now)
    LaunchedEffect(lastSentAt) {
        while (EmailConfirmation.cooldownRemaining(lastSentAt, System.currentTimeMillis()) > 0) {
            now = System.currentTimeMillis(); delay(500)
        }
        now = System.currentTimeMillis()
    }
    val noEmailApp = stringResource(R.string.open_email_none)

    Text(stringResource(R.string.check_email_title), style = Tf.title, color = Ink)
    TactileCard {
        Column(verticalArrangement = Arrangement.spacedBy(S.Md)) {
            Text(
                stringResource(if (justSent) R.string.check_email_body else R.string.check_email_unconfirmed, address),
                style = Tf.body, color = Ink,
            )
            ErrorText(error)
            info?.let { Banner(it, kind = BannerKind.Success) }
            TactileButton(stringResource(R.string.open_email_app), {
                error = null
                val intent = android.content.Intent(android.content.Intent.ACTION_MAIN)
                    .addCategory(android.content.Intent.CATEGORY_APP_EMAIL)
                    .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                try { ctx.startActivity(android.content.Intent.createChooser(intent, null).addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)) }
                catch (_: android.content.ActivityNotFoundException) { error = noEmailApp }
            }, Modifier.fillMaxWidth())
            TactileButton(
                if (remaining > 0) stringResource(R.string.resend_email_wait, remaining) else stringResource(R.string.resend_email),
                {
                    scope.launch {
                        busy = true; error = null; info = null
                        AppStore.resendConfirmation(address)
                            .onSuccess { lastSentAt = System.currentTimeMillis(); now = lastSentAt; info = ctx.getString(R.string.resend_email_sent) }
                            .onFailure { error = it.errorText }
                        busy = false
                    }
                },
                Modifier.fillMaxWidth(), backgroundColor = PillBg, enabled = remaining == 0 && !busy, loading = busy,
            )
            TactileButton(stringResource(R.string.confirmed_sign_in), onConfirmed, Modifier.fillMaxWidth(), backgroundColor = CardBg)
        }
    }
}

/**
 * Continue with Apple / Google, always visible (Apple first). A provider that is off or missing build
 * settings still shows; tapping it explains it is not set up yet. A new account still has to pass
 * [CertifyScreen] before any household data loads.
 */
@Composable
private fun OAuthButtons(busy: Boolean, run: (suspend () -> Unit) -> Unit, onError: (String?) -> Unit) {
    val cfg = remember { OAuthConfig.current }
    val ctx = rememberContext()
    var notSet by remember { mutableStateOf<String?>(null) }
    AppStore.oauthError?.let { Banner(it, kind = BannerKind.Danger) }
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(S.Md)) {
        HorizontalDivider(Modifier.weight(1f), color = DividerColor)
        Text("or", style = Tf.bodySm, color = MutedText)
        HorizontalDivider(Modifier.weight(1f), color = DividerColor)
    }
    OAuthProviderButton("Continue with Apple", cfg.apple, busy, onNotSet = { notSet = OAuthConfig.notSetUpMessage("Apple") }) {
        AppStore.oauthError = null; notSet = null
        run { AppStore.signInWithApple().onFailure { onError(it.errorText) } }
    }
    OAuthProviderButton("Continue with Google", cfg.google, busy, onNotSet = { notSet = OAuthConfig.notSetUpMessage("Google") }) {
        AppStore.oauthError = null; notSet = null
        run { AppStore.signInWithGoogle(ctx).onFailure { if (it !is OAuthSignIn.Cancelled) onError(it.errorText) } }
    }
    notSet?.let { Banner(it, kind = BannerKind.Info) }
}

@Composable
private fun OAuthProviderButton(label: String, availability: OAuthConfig.Availability, busy: Boolean, onNotSet: () -> Unit, onClick: () -> Unit) {
    val ready = availability == OAuthConfig.Availability.Ready
    TactileButton(label, if (ready) onClick else onNotSet, Modifier.fillMaxWidth(), backgroundColor = CardBg, enabled = !busy)
}

/** First sign-in: confirm the account holder is an adult (POST /api/auth/certify). */
@Composable
fun CertifyScreen() {
    val scope = rememberCoroutineScope()
    var dob by rememberSaveable { mutableStateOf("") }
    var adult by rememberSaveable { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    fun parsed(): LocalDate? = try { LocalDate.parse(dob.trim()) } catch (_: DateTimeParseException) { null }

    AuthFrame {
        ScreenColumn {
            Text("TreeFort", style = Tf.brand, color = Ink)
            Text("One quick check", style = Tf.title, color = Ink)
            TactileCard {
                Column(verticalArrangement = Arrangement.spacedBy(S.Md)) {
                    Text("TreeFort guardian accounts are for adults. Confirm your birth date to continue.", style = Tf.body, color = Ink)
                    TfTextField(dob, { dob = it.take(10) }, "Birth date", placeholder = "YYYY-MM-DD", keyboardType = KeyboardType.Number, enabled = !busy)
                    ToggleRow("I'm 18 or older", "I'm the parent, guardian or trusted adult for this TreeFort.", adult, { adult = it }, enabled = !busy)
                    ErrorText(error)
                    TactileButton("Continue", {
                        val d = parsed()
                        when {
                            d == null -> error = "Enter your birth date as YYYY-MM-DD."
                            Period.between(d, LocalDate.now()).years < 18 -> error = "You need to be 18 or older to use a guardian account."
                            else -> scope.launch {
                                busy = true; error = null
                                AppStore.certify(d.toString()).onFailure { error = it.errorText }
                                busy = false
                            }
                        }
                    }, Modifier.fillMaxWidth(), enabled = adult && dob.length == 10, loading = busy)
                    TactileButton("Sign out", { AppStore.signOut() }, Modifier.fillMaxWidth(), backgroundColor = PillBg, enabled = !busy)
                }
            }
        }
    }
}
