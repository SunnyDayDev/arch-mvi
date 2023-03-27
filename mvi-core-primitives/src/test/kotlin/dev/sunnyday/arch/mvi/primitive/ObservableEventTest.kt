package dev.sunnyday.arch.mvi.primitive

import org.junit.jupiter.api.Test
import kotlin.test.assertSame

class ObservableEventTest {

    @Test
    fun `empty returns same instance`() {
        assertSame(ObservableEvent.empty<Int>(), ObservableEvent.empty())
    }
}