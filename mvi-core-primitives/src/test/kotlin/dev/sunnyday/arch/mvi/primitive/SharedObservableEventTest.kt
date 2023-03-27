package dev.sunnyday.arch.mvi.primitive

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import java.util.concurrent.TimeUnit
import kotlin.test.assertEquals
import kotlin.test.assertSame
import kotlin.test.assertTrue

class SharedObservableEventTest {

    @Test
    fun `resend event as is`() {
        val event = "event"
        val observable = SharedObservableEvent<String>()
        val outputEvents = mutableListOf<String>()
        observable.observe(outputEvents::add)

        observable.onEvent(event)

        assertSame(event, outputEvents.singleOrNull())
    }

    @Test
    fun `do nothing on cancelled`() {
        val event = "event"
        val observable = SharedObservableEvent<String>()
        val outputEvents = mutableListOf<String>()
        observable.observe(outputEvents::add).cancel()

        observable.onEvent(event)

        assertTrue(outputEvents.isEmpty())
    }

    @Test
    fun `on cancel only triggered subscription is cancelled`() {
        val event = "event"
        val observable = SharedObservableEvent<String>()
        val outputEvents1 = mutableListOf<String>()
        val outputEvents3 = mutableListOf<String>()

        observable.observe(outputEvents1::add)
        val cancellable = observable.observe { }
        observable.observe(outputEvents3::add)

        cancellable.cancel()

        observable.onEvent(event)

        assertEquals(listOf("event"), outputEvents1)
        assertEquals(listOf("event"), outputEvents3)
    }

    @Test
    @Timeout(10, unit = TimeUnit.SECONDS)
    fun `properly handle concurrent subscriptions`() {
        val store = mockk<SharedObservableEvent.EventConsumersStore<String>> {
            var subscribers = emptyArray<EventConsumer<String>>()
            var isConcurrentChecked = false
            every { get() } answers { subscribers }
            every { compareAndSet(any(), any()) } answers {
                if (isConcurrentChecked) {
                    subscribers = secondArg()
                    true
                } else {
                    isConcurrentChecked = true
                    false
                }
            }
        }

        val observable = SharedObservableEvent(store)

        val outputEvents = mutableListOf<String>()
        observable.observe(outputEvents::add)

        observable.onEvent("event")

        assertEquals(1, outputEvents.size)
        verify(exactly = 2) { store.compareAndSet(any(), any()) }
    }
}