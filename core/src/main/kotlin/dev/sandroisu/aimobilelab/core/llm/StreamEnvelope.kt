package dev.sandroisu.aimobilelab.core.llm

import dev.sandroisu.aimobilelab.core.chat.StreamEvent

data class StreamEnvelope(val key: String, val streamEvent: StreamEvent)
