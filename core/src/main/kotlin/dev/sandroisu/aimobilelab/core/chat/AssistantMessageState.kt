package dev.sandroisu.aimobilelab.core.chat

data class AssistantMessageState(
    val id: String,
    val attempt: Int,
    val isStreaming: Boolean,
    val partialText: String,
    val finalText: String?,
    val error: String?,
)
