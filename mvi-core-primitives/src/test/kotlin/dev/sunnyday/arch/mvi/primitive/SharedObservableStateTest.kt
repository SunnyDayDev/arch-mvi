package dev.sunnyday.arch.mvi.primitive

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class SharedObservableStateTest {

    @Test
    fun `resend event as is`() {
        val observable = SharedObservableState("initial")
        val outputEvents = mutableListOf<String>()
        observable.observe(outputEvents::add)

        observable.onEvent("event")

        assertEquals("event", observable.value)
        assertEquals(listOf("initial", "event"), outputEvents)
    }

    @Test
    fun `on canceled observe only last value`() {
        val observable = SharedObservableState("initial")
        observable.onEvent("event")

        val outputEvents = mutableListOf<String>()
        observable.observe(outputEvents::add).cancel()

        observable.onEvent("event2")

        assertEquals(listOf("event"), outputEvents)
    }

    @Test
    fun `value changes on ony update`() {
        val observable = SharedObservableState("initial")
        val outputEvents = mutableListOf<String>()

        assertEquals("initial", observable.value)

        val cancellable = observable.observe(outputEvents::add)
        observable.onEvent("event")

        assertEquals("event", observable.value)

        cancellable.cancel()
        observable.onEvent("event2")

        assertEquals("event2", observable.value)
    }
}