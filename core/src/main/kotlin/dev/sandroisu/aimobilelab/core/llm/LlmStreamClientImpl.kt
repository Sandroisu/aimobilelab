package dev.sandroisu.aimobilelab.core.llm

import dev.sandroisu.aimobilelab.core.chat.StreamEvent
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class LlmStreamClientImpl: LLMStreamClient {
    override fun stream(
        id: String, attempt: Int,
    ): Flow<StreamEnvelope> {
        return flow {
            emit(StreamEnvelope("key1#1", StreamEvent.Start("key1", 1)))
            delay(500)
            for (i in 0..3 ) {
                emit(StreamEnvelope("key1#1", StreamEvent.Delta("hello $i\n")))
                delay(1000)
            }
            emit(StreamEnvelope("key1#1", StreamEvent.End))
        }
    }
}
