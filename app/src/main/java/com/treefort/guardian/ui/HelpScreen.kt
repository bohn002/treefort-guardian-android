package com.treefort.guardian.ui

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
import androidx.compose.ui.semantics.Role
import com.treefort.guardian.AppConfig
import com.treefort.guardian.theme.Ink
import com.treefort.guardian.theme.MutedText
import com.treefort.guardian.theme.Tf
import com.treefort.guardian.theme.TreeFortTokens

private val S = TreeFortTokens.Space

private data class Faq(val q: String, val a: String)

private val FAQS = listOf(
    Faq("How do I set up a TreeFort phone?",
        "Plug the TreeFort phone in and connect it to Wi-Fi. Wait for the phone's chime, then enter the claim code it gives you on the TreeFort tab (Add). Give it your child's name and the room it lives in."),
    Faq("Who can call my child?",
        "Only TreeFort numbers on your Safelist. Open Friends to add a number, or invite a grown-up with a code. Anyone not on the Safelist can't ring the phone."),
    Faq("What is a TreeFort number?",
        "A short code that starts with * and has five digits, like *12345. Every TreeFort phone has one, and so does each guardian app. Regular phone numbers can't be added."),
    Faq("What is a caller?",
        "A grown-up with the app and no phone, like Uncle Bob. Invite them from Friends and switch on \"They don't have a TreeFort phone\". Callers can call the household's phones and be called by them. They can't change anything."),
    Faq("Can someone else help me manage the household?",
        "Yes. In Settings, open Guardians and callers and invite a co-guardian. A household can have up to 6 guardians and up to 50 callers. Owners can invite and remove guardians; at least one owner always stays."),
    Faq("What are Quiet Hours?",
        "Times when the phone won't ring, like bedtime. Set them for each phone on the TreeFort tab. Calls from the Safelist wait in the Logbook."),
    Faq("What is vacation mode?",
        "Plan a trip in Settings and the phone follows it: the right time zone, optional Quiet Hours and a temporary emergency address. When the trip ends the phone goes back to its home settings on its own."),
    Faq("What happens if my child dials 911?",
        "Emergency calling isn't available yet. Today the phone plays a prompt asking your child to use a grown-up's phone. You can save an emergency address now so it's ready for later."),
    Faq("What is the Campfire?",
        "A household room that every TreeFort phone reaches on speed dial #0. Joining it from this app is coming soon."),
    Faq("What is Guardian Lock?",
        "A PIN you choose that keeps kids out of Settings when your phone is unlocked. It starts off, with no default PIN. If you forget it, signing out removes it."),
    Faq("Where do I manage billing?",
        "On the web at ${AppConfig.WEB_APP_URL}. This app shows your plan and has no in-app purchases."),
)

/** Help: short answers in TreeFort's own words (TreeFort phone, Safelist, Quiet Hours, Logbook, Claim code). */
@Composable
fun HelpScreen() {
    ScreenColumn {
        SectionTitle("Help")
        FAQS.forEachIndexed { i, f ->
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
