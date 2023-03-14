package dev.sunnyday.arch.mvi.internal.event_handler

import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import dev.sunnyday.arch.mvi.primitive.EventConsumer
import dev.sunnyday.arch.mvi.test.collectWithScope

@OptIn(ExperimentalCoroutinesApi::class)
class TransparentEventHandlerTest {

    @Test
    fun `resend event as is`() = runTest(UnconfinedTestDispatcher()) {
        val event = "event"
        val receiver = mockk<EventConsumer<String>>(relaxed = true)
        val handler = TransparentEventHandler<String>().apply {
            this.receiver = receiver
        }
        val outputEvents = handler.outputEvents.collectWithScope()

        handler.onEvent(event)

        assertTrue(outputEvents.isEmpty())
        verify { receiver.onEvent(refEq(event)) }
    }
}