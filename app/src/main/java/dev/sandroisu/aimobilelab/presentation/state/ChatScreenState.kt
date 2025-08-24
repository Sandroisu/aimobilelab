package dev.sandroisu.aimobilelab.presentation.state

import dev.sandroisu.aimobilelab.core.chat.Role

data class ChatScreenState(
    val messages: List<ChatMessageUi>,
    val input: String,
    val canSend: Boolean,
    val canCancel: Boolean,
    val canRetry: Boolean,
)

data class ChatMessageUi(
    val id: String,
    val role: Role,
    val text: String,
    val isStreaming: Boolean,
)
