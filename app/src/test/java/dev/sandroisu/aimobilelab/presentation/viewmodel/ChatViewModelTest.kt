package dev.sandroisu.aimobilelab.presentation.viewmodel

import dev.sandroisu.aimobilelab.core.chat.ChatMessage
import dev.sandroisu.aimobilelab.core.chat.Role
import dev.sandroisu.aimobilelab.core.chat.StreamEvent
import dev.sandroisu.aimobilelab.core.llm.LlmStreamClient
import dev.sandroisu.aimobilelab.core.llm.StreamEnvelope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ChatViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private lateinit var scope: TestScope

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        scope = TestScope(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun send_success_emits_final_assistant_message() =
        runTest(dispatcher) {
            val client = FakeClient(parts = listOf("Пр", "ив", "ет"), errorAt = null, chunkDelayMs = 100)
            val vm = ChatViewModel(client)
            vm.onInputChange("Привет")
            vm.send()
            advanceTimeBy(500)
            advanceUntilIdle()
            val ui = vm.screenState.value
            assertTrue(ui.messages.any { it.role == Role.User && it.text == "Привет" })
            assertTrue(ui.messages.any { it.role == Role.Assistant && it.text == "Привет" && !it.isStreaming })
            assertFalse(ui.canCancel)
            assertFalse(ui.canRetry)
        }

    @Test
    fun cancel_preserves_partial_and_enables_retry() =
        runTest(dispatcher) {
            val client = FakeClient(parts = listOf("Ча", "ст", "ь"), errorAt = null, chunkDelayMs = 200)
            val vm = ChatViewModel(client)
            vm.onInputChange("X")
            vm.send()
            advanceTimeBy(250)
            vm.cancel()
            runCurrent()
            val ui = vm.screenState.value
            assertTrue(ui.messages.any { it.role == Role.User && it.text == "X" })
            assertTrue(ui.canRetry)
            assertFalse(ui.canCancel)
            val last = ui.messages.last()
            assertEquals(Role.Assistant, last.role)
            assertTrue(last.text.isNotEmpty())
        }

    @Test
    fun retry_after_error_finishes_successfully() =
        runTest(dispatcher) {
            val client = FakeClient(parts = listOf("А", "Б", "В"), errorAt = 1, chunkDelayMs = 100)
            val vm = ChatViewModel(client)
            vm.onInputChange("Q")
            vm.send()
            advanceTimeBy(250)
            runCurrent()
            val ui1 = vm.screenState.value
            assertTrue(ui1.canRetry)
            client.errorAt = null
            vm.retry()
            advanceTimeBy(400)
            runCurrent()
            val ui2 = vm.screenState.value
            val assistantFinal = ui2.messages.filter { it.role == Role.Assistant && !it.isStreaming }
            assertEquals(1, assistantFinal.size)
            assertEquals("АБВ", assistantFinal.first().text)
            assertFalse(ui2.canRetry)
            assertFalse(ui2.canCancel)
        }

    @Test
    fun second_send_cancels_first_stream() =
        runTest(dispatcher) {
            val client = FakeClient(parts = listOf("1", "2", "3"), errorAt = null, chunkDelayMs = 200)
            val vm = ChatViewModel(client)
            vm.onInputChange("first")
            vm.send()
            advanceTimeBy(150)
            vm.onInputChange("second")
            vm.send()
            advanceTimeBy(600)
            runCurrent()
            val ui = vm.screenState.value
            val assistantFinal = ui.messages.filter { it.role == Role.Assistant && !it.isStreaming }
            assertEquals(1, assistantFinal.size)
        }

    @Test
    fun late_chunk_after_end_is_ignored() =
        runTest(dispatcher) {
            val client = FakeLateChunkClient
            val vm = ChatViewModel(client)
            vm.onInputChange("Hi")
            vm.send()
            advanceTimeBy(800)
            runCurrent()
            val ui = vm.screenState.value
            val assistantFinal = ui.messages.filter { it.role == Role.Assistant && !it.isStreaming }
            assertEquals(1, assistantFinal.size)
            assertEquals("A", assistantFinal.first().text)
            assertFalse(ui.canRetry)
            assertFalse(ui.canCancel)
        }

    @Test
    fun send_is_disabled_while_streaming_and_enabled_after() =
        runTest(dispatcher) {
            val client = FakeClient(parts = listOf("X", "Y", "Z"), errorAt = null, chunkDelayMs = 300)
            val vm = ChatViewModel(client)
            vm.onInputChange("msg")
            vm.send()
            val uiDuring = vm.screenState.value
            assertTrue(uiDuring.canCancel)
            assertFalse(uiDuring.canSend)
            advanceTimeBy(1200)
            runCurrent()
            val uiAfter = vm.screenState.value
            assertFalse(uiAfter.canCancel)
            vm.onInputChange("next")
            val uiReady = vm.screenState.value
            assertTrue(uiReady.canSend)
        }

    private object FakeLateChunkClient : LlmStreamClient {
        override fun stream(
            id: String,
            attempt: Int,
            history: List<ChatMessage>,
        ): Flow<StreamEnvelope> =
            flow {
                val key = "$id#$attempt"
                emit(StreamEnvelope(key, StreamEvent.Start(id, attempt)))
                delay(200)
                emit(StreamEnvelope(key, StreamEvent.Delta("A")))
                delay(200)
                emit(StreamEnvelope(key, StreamEvent.End))
                delay(200)
                emit(StreamEnvelope(key, StreamEvent.Delta("TAIL")))
            }
    }

    private class FakeClient(
        private val parts: List<String>,
        var errorAt: Int?,
        private val chunkDelayMs: Long,
    ) : LlmStreamClient {
        override fun stream(
            id: String,
            attempt: Int,
            history: List<ChatMessage>,
        ): Flow<StreamEnvelope> =
            flow {
                val key = "$id#$attempt"
                emit(StreamEnvelope(key, StreamEvent.Start(id, attempt)))
                for ((i, p) in parts.withIndex()) {
                    if (!kotlin.coroutines.coroutineContext.isActive) return@flow
                    delay(chunkDelayMs)
                    emit(StreamEnvelope(key, StreamEvent.Delta(p)))
                    if (errorAt != null && i == errorAt) {
                        emit(StreamEnvelope(key, StreamEvent.Error("timeout")))
                        return@flow
                    }
                }
                emit(StreamEnvelope(key, StreamEvent.End))
            }
    }
}
