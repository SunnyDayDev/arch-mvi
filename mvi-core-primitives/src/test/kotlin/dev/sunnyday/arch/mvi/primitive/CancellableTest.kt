package dev.sunnyday.arch.mvi.primitive

import org.junit.jupiter.api.Test
import kotlin.test.assertSame

class CancellableTest {

    @Test
    fun `empty returns same instance`() {
        assertSame(Cancellable.empty(), Cancellable.empty())
    }
}