package dev.sandroisu.aimobilelab.core.chat

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

class AssistantMessageReducerTest {
    @Test
    fun success_flow_start_delta_delta_end() {
        var s: AssistantMessageState? = null
        s = AssistantMessageReducer.reduce(s, StreamEvent.Start(id = "m_01", attempt = 1))
        s = AssistantMessageReducer.reduce(s, StreamEvent.Delta("Gre"))
        s = AssistantMessageReducer.reduce(s, StreamEvent.Delta("at"))
        s = AssistantMessageReducer.reduce(s, StreamEvent.End)

        assertEquals("m_01", s.id)
        assertEquals(1, s.attempt)
        assertFalse(s.isStreaming)
        assertEquals("Great", s.finalText)
        assertEquals("Great", s.partialText)
        assertNull(s.error)
    }

    @Test
    fun error_flow_keeps_partial_text() {
        var s: AssistantMessageState? = null
        s = AssistantMessageReducer.reduce(s, StreamEvent.Start(id = "m_02", attempt = 1))
        s = AssistantMessageReducer.reduce(s, StreamEvent.Delta("Hour"))
        s = AssistantMessageReducer.reduce(s, StreamEvent.Error("timeout"))

        assertEquals("m_02", s.id)
        assertEquals("Hour", s.partialText)
        assertNull(s.finalText)
        assertEquals("timeout", s.error)
        assertFalse(s.isStreaming)
    }

    @Test
    fun late_delta_after_end_is_ignored() {
        var s: AssistantMessageState? = null
        s = AssistantMessageReducer.reduce(s, StreamEvent.Start(id = "m_03", attempt = 1))
        s = AssistantMessageReducer.reduce(s, StreamEvent.Delta("A"))
        s = AssistantMessageReducer.reduce(s, StreamEvent.End)
        val before = s
        s = AssistantMessageReducer.reduce(s, StreamEvent.Delta("tail"))

        assertEquals(before, s)
    }

    @Test
    fun second_start_closes_previous_and_starts_new() {
        var s: AssistantMessageState? = null
        s = AssistantMessageReducer.reduce(s, StreamEvent.Start(id = "m_04", attempt = 1))
        s = AssistantMessageReducer.reduce(s, StreamEvent.Delta("Old"))
        s = AssistantMessageReducer.reduce(s, StreamEvent.Start(id = "m_04", attempt = 2))

        assertEquals("m_04", s.id)
        assertEquals(2, s.attempt)
        assertEquals("", s.partialText)
        assertNull(s.finalText)
        assertEquals(true, s.isStreaming)
        assertNull(s.error)
    }

    @Test
    fun delta_without_start_sets_error() {
        var s: AssistantMessageState? = null
        s = AssistantMessageReducer.reduce(s, StreamEvent.Delta("X"))
        assertEquals("", s.id)
        assertEquals(1, s.attempt)
        assertFalse(s.isStreaming)
        assertNull(s.finalText)
        assertEquals("", s.partialText)
        assertEquals("delta_without_start", s.error)
    }

    @Test
    fun end_without_start_sets_error() {
        var s: AssistantMessageState? = null
        s = AssistantMessageReducer.reduce(s, StreamEvent.End)
        assertEquals("", s.id)
        assertEquals(1, s.attempt)
        assertFalse(s.isStreaming)
        assertNull(s.finalText)
        assertEquals("", s.partialText)
        assertEquals("end_without_start", s.error)
    }

    @Test
    fun cancel_marks_error_and_preserves_partial() {
        var s: AssistantMessageState? = null
        s = AssistantMessageReducer.reduce(s, StreamEvent.Start(id = "m_05", attempt = 1))
        s = AssistantMessageReducer.reduce(s, StreamEvent.Delta("Hel"))
        s = AssistantMessageReducer.reduce(s, StreamEvent.Error("canceled"))

        assertEquals("m_05", s.id)
        assertEquals(1, s.attempt)
        assertFalse(s.isStreaming)
        assertEquals("Hel", s.partialText)
        assertNull(s.finalText)
        assertEquals("canceled", s.error)
    }
}
