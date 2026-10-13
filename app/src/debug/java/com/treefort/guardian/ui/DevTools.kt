package com.treefort.guardian.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.treefort.guardian.AppConfig
import com.treefort.guardian.AppPrefs
import com.treefort.guardian.AppStore
import com.treefort.guardian.theme.CardBg
import com.treefort.guardian.theme.MutedText
import com.treefort.guardian.theme.Tf
import com.treefort.guardian.theme.TreeFortTokens

/** Debug builds only (this file is not in the release source set): point the app at a LAN or emulator backend. */
@Composable
fun DevToolsSection() {
    var host by remember { mutableStateOf(AppConfig.apiHost) }
    FieldLabel("Developer: API host")
    TfTextField(host, { host = it.trim() }, "Host", placeholder = "10.0.2.2:8000")
    Text("Plain HTTP is allowed for LAN and emulator hosts in debug builds only. Sign out and in again after changing it.", style = Tf.caption, color = MutedText)
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(TreeFortTokens.Space.Md)) {
        TactileButton("Use this host", { AppPrefs.setDevApiHost(host); AppStore.refresh() }, Modifier.weight(1f), backgroundColor = CardBg)
        TactileButton("Reset", { AppPrefs.setDevApiHost(null); host = AppConfig.apiHost; AppStore.refresh() }, Modifier.weight(1f), backgroundColor = CardBg)
    }
}
