package dev.sandroisu.aimobilelab.core.chat

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

class AssistantMessageReducerTest {

    @Test
    fun success_flow_start_delta_delta_end() {
        var state: AssistantMessageState? = null
        state = AssistantMessageReducer.reduce(state, StreamEvent.Start(id = "m_01", attempt = 1))
        state = AssistantMessageReducer.reduce(state, StreamEvent.Delta("Gre"))
        state = AssistantMessageReducer.reduce(state, StreamEvent.Delta("at"))
        state = AssistantMessageReducer.reduce(state, StreamEvent.End)

        assertEquals("m_01", state.id)
        assertEquals(1, state.attempt)
        assertFalse(state.isStreaming)
        assertEquals("Great", state.finalText)
        assertEquals("Great", state.partialText)
        assertNull(state.error)
    }

    @Test
    fun error_flow_keeps_partial_text() {
        var state: AssistantMessageState? = null
        state = AssistantMessageReducer.reduce(state, StreamEvent.Start(id = "m_02", attempt = 1))
        state = AssistantMessageReducer.reduce(state, StreamEvent.Delta("Hour"))
        state = AssistantMessageReducer.reduce(state, StreamEvent.Error("timeout"))

        assertEquals("m_02", state.id)
        assertEquals("Hour", state.partialText)
        assertNull(state.finalText)
        assertEquals("timeout", state.error)
        assertFalse(state.isStreaming)
    }

    @Test
    fun late_delta_after_end_is_ignored() {
        var state: AssistantMessageState? = null
        state = AssistantMessageReducer.reduce(state, StreamEvent.Start(id = "m_03", attempt = 1))
        state = AssistantMessageReducer.reduce(state, StreamEvent.Delta("A"))
        state = AssistantMessageReducer.reduce(state, StreamEvent.End)
        val before = state
        state = AssistantMessageReducer.reduce(state, StreamEvent.Delta("tail"))

        assertEquals(before, state)
    }

    @Test
    fun second_start_closes_previous_and_starts_new() {
        var state: AssistantMessageState? = null
        state = AssistantMessageReducer.reduce(state, StreamEvent.Start(id = "m_04", attempt = 1))
        state = AssistantMessageReducer.reduce(state, StreamEvent.Delta("Old"))
        state = AssistantMessageReducer.reduce(state, StreamEvent.Start(id = "m_04", attempt = 2))

        assertEquals("m_04", state.id)
        assertEquals(2, state.attempt)
        assertEquals("", state.partialText)
        assertNull(state.finalText)
        assertEquals(true, state.isStreaming)
        assertNull(state.error)
    }

    @Test
    fun delta_without_start_sets_error() {
        var state: AssistantMessageState? = null
        state = AssistantMessageReducer.reduce(state, StreamEvent.Delta("X"))
        assertEquals("", state.id)
        assertEquals(1, state.attempt)
        assertFalse(state.isStreaming)
        assertNull(state.finalText)
        assertEquals("", state.partialText)
        assertEquals("delta_without_start", state.error)
    }

    @Test
    fun end_without_start_sets_error() {
        var state: AssistantMessageState? = null
        state = AssistantMessageReducer.reduce(state, StreamEvent.End)
        assertEquals("", state.id)
        assertEquals(1, state.attempt)
        assertFalse(state.isStreaming)
        assertNull(state.finalText)
        assertEquals("", state.partialText)
        assertEquals("end_without_start", state.error)
    }

    @Test
    fun cancel_marks_error_and_preserves_partial() {
        var state: AssistantMessageState? = null
        state = AssistantMessageReducer.reduce(state, StreamEvent.Start(id = "m_05", attempt = 1))
        state = AssistantMessageReducer.reduce(state, StreamEvent.Delta("Hel"))
        state = AssistantMessageReducer.reduce(state, StreamEvent.Error("canceled"))

        assertEquals("m_05", state.id)
        assertEquals(1, state.attempt)
        assertFalse(state.isStreaming)
        assertEquals("Hel", state.partialText)
        assertNull(state.finalText)
        assertEquals("canceled", state.error)
    }
}
