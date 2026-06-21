package co.hermesdispatch.app.data.remote

import co.hermesdispatch.app.data.remote.sse.SseParser
import co.hermesdispatch.app.data.remote.sse.StreamEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SseParserTest {

    @Test
    fun `token event by event name`() {
        val e = SseParser.parse("token", """{"text":"hello"}""")
        assertEquals(StreamEvent.Token("hello"), e)
    }

    @Test
    fun `token event inferred from type field when event name absent`() {
        val e = SseParser.parse(null, """{"type":"delta","text":"hi"}""")
        assertEquals(StreamEvent.Token("hi"), e)
    }

    @Test
    fun `tool event maps name and preview`() {
        val e = SseParser.parse("tool", """{"name":"web_search","preview":"q: cats"}""")
        assertEquals(StreamEvent.Tool("web_search", "q: cats"), e)
    }

    @Test
    fun `tool_use alias maps to Tool`() {
        val e = SseParser.parse("tool_use", """{"tool_name":"gmail"}""")
        assertTrue(e is StreamEvent.Tool && (e as StreamEvent.Tool).name == "gmail")
    }

    @Test
    fun `status event`() {
        val e = SseParser.parse("status", """{"text":"Searching Web"}""")
        assertEquals(StreamEvent.Status("Searching Web"), e)
    }

    @Test
    fun `completed event`() {
        val e = SseParser.parse("completed", """{"session":{}}""")
        assertTrue(e is StreamEvent.Completed)
    }

    @Test
    fun `error event maps message and type`() {
        val e = SseParser.parse("error", """{"message":"boom","type":"rate_limit"}""")
        assertEquals(StreamEvent.Error("boom", "rate_limit"), e)
    }

    @Test
    fun `interrupted and cancelled both map to Interrupted`() {
        assertEquals(StreamEvent.Interrupted, SseParser.parse("interrupted", "{}"))
        assertEquals(StreamEvent.Interrupted, SseParser.parse("cancelled", "{}"))
    }

    @Test
    fun `unknown event is not fatal`() {
        val e = SseParser.parse("totally_new_event", """{"x":1}""")
        assertTrue(e is StreamEvent.Unknown)
    }

    @Test
    fun `malformed json does not throw`() {
        val e = SseParser.parse("token", "not-json")
        assertEquals(StreamEvent.Token("not-json"), e)
    }
}
