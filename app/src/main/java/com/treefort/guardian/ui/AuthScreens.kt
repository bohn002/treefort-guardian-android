package com.treefort.guardian.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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

private enum class AuthMode(val label: String) { SignIn("Sign in"), SignUp("Create account"), Code("Email code") }

/** Supabase Auth is the only identity provider. Email + password, or a one-time email code. */
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

    fun run(block: suspend () -> Unit) {
        scope.launch { busy = true; error = null; info = null; try { block() } finally { busy = false } }
    }

    AuthFrame {
        ScreenColumn {
            Text("TreeFort", style = Tf.brand, color = Ink)
            Text("Guardian sign-in", style = Tf.title, color = Ink)
            SegmentedControl(AuthMode.entries.map { it.label }, mode.ordinal, { mode = AuthMode.entries[it]; error = null; info = null; codeSent = false })

            TactileCard {
                Column(verticalArrangement = Arrangement.spacedBy(S.Md)) {
                    TfTextField(email, { email = it }, "Email", keyboardType = KeyboardType.Email, enabled = !busy)
                    when (mode) {
                        AuthMode.SignIn, AuthMode.SignUp -> {
                            TfTextField(
                                password, { password = it }, "Password", password = true, enabled = !busy,
                                supportingText = if (mode == AuthMode.SignUp) "At least 8 characters." else null,
                            )
                        }
                        AuthMode.Code -> if (codeSent) {
                            TfTextField(code, { code = it.filter(Char::isDigit).take(8) }, "Code from your email", keyboardType = KeyboardType.NumberPassword, enabled = !busy)
                        }
                    }
                    ErrorText(error)
                    info?.let { Banner(it, kind = BannerKind.Success) }

                    when (mode) {
                        AuthMode.SignIn -> TactileButton("Sign in", {
                            run { AppStore.signIn(email, password).onFailure { error = it.errorText } }
                        }, Modifier.fillMaxWidth(), enabled = email.isNotBlank() && password.isNotEmpty(), loading = busy)

                        AuthMode.SignUp -> TactileButton("Create account", {
                            if (password.length < 8) { error = "Use at least 8 characters for your password." }
                            else run {
                                AppStore.signUp(email, password)
                                    .onSuccess { signedIn -> if (!signedIn) { info = "Check your email and confirm your address, then sign in."; mode = AuthMode.SignIn } }
                                    .onFailure { error = it.errorText }
                            }
                        }, Modifier.fillMaxWidth(), enabled = email.isNotBlank() && password.isNotEmpty(), loading = busy)

                        AuthMode.Code -> if (!codeSent) {
                            TactileButton("Email me a code", {
                                run { AppStore.sendEmailCode(email).onSuccess { codeSent = true; info = "We sent a code to ${email.trim()}." }.onFailure { error = it.errorText } }
                            }, Modifier.fillMaxWidth(), enabled = email.isNotBlank(), loading = busy)
                        } else {
                            TactileButton("Sign in", {
                                run { AppStore.verifyEmailCode(email, code).onFailure { error = it.errorText } }
                            }, Modifier.fillMaxWidth(), enabled = code.length >= 6, loading = busy)
                            TactileButton("Send a new code", {
                                run { AppStore.sendEmailCode(email).onSuccess { info = "New code sent." }.onFailure { error = it.errorText } }
                            }, Modifier.fillMaxWidth(), backgroundColor = PillBg, enabled = !busy)
                        }
                    }
                }
            }
            OAuthButtons(busy = busy, run = ::run, onError = { error = it })
            Text(
                "Guardian accounts are for adults 18 and over. Kids never sign in: they use the TreeFort phone.",
                style = Tf.bodySm, color = MutedText, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth(),
            )
            val ctx = rememberContext()
            Text(
                "Privacy policy", style = Tf.bodySm, color = LinkColor, textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().clickable(role = Role.Button) {
                    ctx.startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(AppConfig.PRIVACY_URL)).addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK))
                },
            )
        }
    }
}

/**
 * Sign in with Google / Apple. A provider whose build flag is off renders nothing; one that is on but
 * missing build settings renders disabled with the reason. A new account still has to pass
 * [CertifyScreen] before any household data loads.
 */
@Composable
private fun OAuthButtons(busy: Boolean, run: (suspend () -> Unit) -> Unit, onError: (String?) -> Unit) {
    val cfg = remember { OAuthConfig.current }
    if (!cfg.anyVisible) return
    val ctx = rememberContext()
    AppStore.oauthError?.let { Banner(it, kind = BannerKind.Danger) }
    Text("or", style = Tf.bodySm, color = MutedText, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
    OAuthProviderButton("Sign in with Google", cfg.google, busy) {
        AppStore.oauthError = null
        run { AppStore.signInWithGoogle(ctx).onFailure { if (it !is OAuthSignIn.Cancelled) onError(it.errorText) } }
    }
    OAuthProviderButton("Sign in with Apple", cfg.apple, busy) {
        AppStore.oauthError = null
        run { AppStore.signInWithApple().onFailure { onError(it.errorText) } }
    }
}

@Composable
private fun OAuthProviderButton(label: String, availability: OAuthConfig.Availability, busy: Boolean, onClick: () -> Unit) {
    when (availability) {
        OAuthConfig.Availability.Off -> Unit
        OAuthConfig.Availability.Ready ->
            TactileButton(label, onClick, Modifier.fillMaxWidth(), backgroundColor = CardBg, enabled = !busy)
        is OAuthConfig.Availability.Misconfigured -> Column(verticalArrangement = Arrangement.spacedBy(S.Xs)) {
            TactileButton(label, {}, Modifier.fillMaxWidth(), backgroundColor = CardBg, enabled = false)
            Text(availability.reason, style = Tf.bodySm, color = MutedText, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        }
    }
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
