package com.treefort.guardian.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Contact(
    val id: Int,
    val name: String,
    @SerialName("phone_number") val phone: String? = null,
    @SerialName("treefort_id") val treefortId: String? = null,
    @SerialName("allow_incoming") val allowIncoming: Boolean = true,
    @SerialName("allow_outgoing") val allowOutgoing: Boolean = true,
    @SerialName("target_station") val targetStation: String = "All",
    @SerialName("speed_dial") val speedDial: String? = null,
    @SerialName("avatar_emoji") val avatarEmoji: String? = "👤",
    @SerialName("avatar_bg") val avatarBg: String? = "#3DCC79"
)
