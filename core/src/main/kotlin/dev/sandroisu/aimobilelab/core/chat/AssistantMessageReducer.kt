package dev.sandroisu.aimobilelab.core.chat

object AssistantMessageReducer {
    fun reduce(
        current: AssistantMessageState?,
        event: StreamEvent,
    ): AssistantMessageState =
        when (event) {
            is StreamEvent.Start -> {
                AssistantMessageState(
                    id = event.id,
                    attempt = event.attempt,
                    isStreaming = true,
                    partialText = "",
                    finalText = null,
                    error = null,
                )
            }

            is StreamEvent.Delta -> {
                if (current == null) {
                    AssistantMessageState(
                        id = "",
                        attempt = 1,
                        isStreaming = false,
                        partialText = "",
                        finalText = null,
                        error = "delta_without_start",
                    )
                } else {
                    if (!current.isStreaming) {
                        current
                    } else {
                        current.copy(partialText = current.partialText + event.text)
                    }
                }
            }

            StreamEvent.End -> {
                if (current == null) {
                    AssistantMessageState(
                        id = "",
                        attempt = 1,
                        isStreaming = false,
                        partialText = "",
                        finalText = null,
                        error = "end_without_start",
                    )
                } else {
                    if (!current.isStreaming) {
                        current
                    } else {
                        current.copy(
                            isStreaming = false,
                            finalText = current.partialText,
                        )
                    }
                }
            }

            is StreamEvent.Error -> {
                current?.copy(
                    isStreaming = false,
                    error = event.message,
                ) ?: AssistantMessageState(
                    id = "",
                    attempt = 1,
                    isStreaming = false,
                    partialText = "",
                    finalText = null,
                    error = event.message,
                )
            }
        }
}
