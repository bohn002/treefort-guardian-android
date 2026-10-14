package com.treefort.guardian.ui

import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import com.treefort.guardian.AppConfig
import com.treefort.guardian.theme.Ink
import com.treefort.guardian.theme.MutedText
import com.treefort.guardian.theme.Tf
import com.treefort.guardian.theme.TreeFortTokens

private val S = TreeFortTokens.Space

private data class Faq(val q: String, val a: String)

// Native-only copy: topics the shared docs/help_content.json does not cover (Quiet Hours,
// Campfire, Guardian Lock, where billing lives in this app). Move to the shared file when added.
private val NATIVE_ONLY_FAQS = listOf(
    Faq("What are Quiet Hours?",
        "Times when the phone won't ring, like bedtime. Set them for each phone on the TreeFort tab. Calls from the Safelist wait in the Logbook."),
    Faq("What is the Campfire?",
        "A household room that every TreeFort phone reaches on speed dial #0. Guardians can join it from the TreeFort tab to talk with every phone at once. Callers can call a phone directly but can't join the Campfire."),
    Faq("What is Guardian Lock?",
        "A PIN you choose that keeps kids out of Settings when your phone is unlocked. It starts off, with no default PIN. If you forget it, signing out removes it."),
    Faq("Where do I manage billing?",
        "On the web at ${AppConfig.WEB_APP_URL}. This app shows your plan and has no in-app purchases."),
)

private fun HelpFaq.toFaq(): Faq {
    val body = buildList {
        if (lead.isNotBlank()) add(lead)
        callout?.let { add(it.text) }
        points.forEach { add("${it.title}: ${it.desc}") }
        footnote?.let { add(it) }
    }
    return Faq(question, body.joinToString("\n\n"))
}

private fun loadFaqs(context: Context): List<Faq> {
    val shared = runCatching {
        context.assets.open(HelpContent.ASSET_NAME).bufferedReader().use { HelpContent.parse(it.readText()) }
    }.getOrNull()
    return (shared?.faqs?.map { it.toFaq() } ?: emptyList()) + NATIVE_ONLY_FAQS
}

/** Help: short answers in TreeFort's own words (TreeFort phone, Safelist, Quiet Hours, Logbook, Claim code). */
@Composable
fun HelpScreen() {
    ScreenColumn {
        SectionTitle("Help")
        val context = LocalContext.current
        val faqs = remember { loadFaqs(context) }
        faqs.forEachIndexed { i, f ->
            var open by rememberSaveable(i) { mutableStateOf(false) }
            TactileCard(padding = S.Md) {
                Column(verticalArrangement = Arrangement.spacedBy(S.Sm)) {
                    Row(
                        Modifier.fillMaxWidth().clickable(role = Role.Button) { open = !open },
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(S.Md),
                    ) {
                        Text(f.q, style = Tf.titleSm, color = Ink, modifier = Modifier.weight(1f))
                        Icon(if (open) Ph.Close else Ph.Plus, contentDescription = if (open) "Collapse" else "Expand", tint = Ink)
                    }
                    if (open) Text(f.a, style = Tf.body, color = MutedText)
                }
            }
        }
        Text("Still stuck? Email ${AppConfig.SUPPORT_EMAIL}.", style = Tf.body, color = MutedText)
    }
}
