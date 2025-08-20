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
        s = AssistantMessageReducer.reduce(s, StreamEvent.Delta("Отл"))
        s = AssistantMessageReducer.reduce(s, StreamEvent.Delta("ично"))
        s = AssistantMessageReducer.reduce(s, StreamEvent.End)

        assertEquals("m_01", s.id)
        assertEquals(1, s.attempt)
        assertFalse(s.isStreaming ?: true)
        assertEquals("Отлично", s.finalText)
        assertEquals("Отлично", s.partialText)
        assertNull(s.error)
    }

    @Test
    fun error_flow_keeps_partial_text() {
        var s: AssistantMessageState? = null
        s = AssistantMessageReducer.reduce(s, StreamEvent.Start(id = "m_02", attempt = 1))
        s = AssistantMessageReducer.reduce(s, StreamEvent.Delta("Час"))
        s = AssistantMessageReducer.reduce(s, StreamEvent.Error("timeout"))

        assertEquals("m_02", s.id)
        assertEquals("Час", s.partialText)
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
        s = AssistantMessageReducer.reduce(s, StreamEvent.Delta("Хвост"))

        assertEquals(before, s)
    }

    @Test
    fun second_start_closes_previous_and_starts_new() {
        var s: AssistantMessageState? = null
        s = AssistantMessageReducer.reduce(s, StreamEvent.Start(id = "m_04", attempt = 1))
        s = AssistantMessageReducer.reduce(s, StreamEvent.Delta("Старый"))
        s = AssistantMessageReducer.reduce(s, StreamEvent.Start(id = "m_04", attempt = 2))

        assertEquals("m_04", s.id)
        assertEquals(2, s.attempt)
        assertEquals("", s.partialText)
        assertNull(s.finalText)
        assertEquals(true, s.isStreaming)
        assertNull(s.error)
    }
}
