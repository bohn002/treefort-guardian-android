package com.treefort.guardian

/**
 * WebSocket close codes the signaling client treats specially.
 * 4409: the hub closed our (oldest) socket because a newer one took the dial code's slot.
 * Auto-reconnecting would evict a sibling device in turn, so it is terminal until the user asks.
 */
object SignalingClose {
    const val REPLACED = 4409
    const val REPLACED_MESSAGE = "Signed in on another device"

    fun isTerminal(code: Int): Boolean = code == REPLACED

    /** Normal closure (1000) is deliberate; any other non-terminal code retries with the usual delay. */
    fun shouldAutoReconnect(code: Int): Boolean = code != 1000 && !isTerminal(code)
}
