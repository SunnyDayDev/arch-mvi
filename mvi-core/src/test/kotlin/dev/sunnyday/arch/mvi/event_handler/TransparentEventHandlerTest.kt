package dev.sunnyday.arch.mvi.event_handler

import org.junit.jupiter.api.Test
import kotlin.test.assertSame
import kotlin.test.assertTrue

class TransparentEventHandlerTest {

    @Test
    fun `resend event as is`() {
        val event = "event"
        val handler = TransparentEventHandler<String>()
        val outputEvents = mutableListOf<String>()
        handler.outputEvents.observe(outputEvents::add)

        handler.onEvent(event)

        assertSame(event, outputEvents.singleOrNull())
    }

    @Test
    fun `do nothing on cancelled`() {
        val event = "event"
        val handler = TransparentEventHandler<String>()
        val outputEvents = mutableListOf<String>()
        handler.outputEvents.observe(outputEvents::add).cancel()

        handler.onEvent(event)

        assertTrue(outputEvents.isEmpty())
    }
}