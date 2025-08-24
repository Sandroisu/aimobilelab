package dev.sandroisu.aimobilelab.core.llm

import dev.sandroisu.aimobilelab.core.chat.StreamEvent
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.isActive
import kotlin.coroutines.coroutineContext

class LlmStreamClientImpl: LlmStreamClient {
    override fun stream(
        id: String, attempt: Int,
    ): Flow<StreamEnvelope> {
        return flow {
            val key = "$id#$attempt"
            emit(StreamEnvelope(key, StreamEvent.Start(id, attempt)))
            delay(300)
            val parts = listOf("Пр", "ив", "ет", "!\n")
            for (p in parts) {
                if (!coroutineContext.isActive) return@flow
                emit(StreamEnvelope(key, StreamEvent.Delta(p)))
                delay(300)
            }
            emit(StreamEnvelope(key, StreamEvent.End))
        }
    }
}
