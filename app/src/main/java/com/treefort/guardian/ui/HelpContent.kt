package com.treefort.guardian.ui

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Shared Help/FAQ content. Source of truth is docs/help_content.json in the meta repo; the
 * `copyHelpContent` gradle task copies it into app assets at build time (no committed duplicate).
 */
@Serializable
data class HelpContent(
    val version: Int,
    val categories: List<HelpCategory>,
    val faqs: List<HelpFaq>,
) {
    companion object {
        const val ASSET_NAME = "help_content.json"
        private val json = Json { ignoreUnknownKeys = true }
        fun parse(text: String): HelpContent = json.decodeFromString(serializer(), text)
    }
}

@Serializable data class HelpCategory(val id: String, val label: String)

@Serializable
data class HelpFaq(
    val id: String,
    val category: String,
    val question: String,
    val badge: String? = null,
    val lead: String = "",
    val callout: HelpCallout? = null,
    val points: List<HelpPoint> = emptyList(),
    val footnote: String? = null,
)

@Serializable data class HelpCallout(val type: String = "", val text: String)

@Serializable data class HelpPoint(val title: String, val desc: String)
