package dev.sandroisu.aimobilelab.core.chat

sealed class StreamEvent {
    data class Start(
        val id: String,
        val attempt: Int,
    ) : StreamEvent()

    data class Delta(
        val text: String,
    ) : StreamEvent()

    data class Error(
        val message: String,
    ) : StreamEvent()

    data object End : StreamEvent()
}
