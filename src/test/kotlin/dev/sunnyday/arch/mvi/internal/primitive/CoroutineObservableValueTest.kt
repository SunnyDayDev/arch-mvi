package dev.sunnyday.arch.mvi.internal.primitive

import dev.sunnyday.arch.mvi.primitive.Cancellable
import dev.sunnyday.arch.mvi.test.runUnconfinedTest
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CoroutineObservableValueTest {

    @Test
    fun `value gets from flow`() {
        val flow = mockk<StateFlow<String>> {
            every { value } returns "flowValue"
        }
        val observable = CoroutineObservableValue(flow)

        assertEquals("flowValue", observable.value)
    }

    @Test
    fun `if coroutine scope is innactive just emit the value`() = runUnconfinedTest {
        val flow = mockk<StateFlow<String>> {
            every { value } returns "flowValue"
        }

        val scope = CoroutineScope(SupervisorJob())
        scope.cancel()

        val observable = CoroutineObservableValue(flow)
        val items = mutableListOf<String>()
        val cancellable = observable.observe(items::add, scope)

        assertEquals(listOf("flowValue"), items)
        assertSame(Cancellable.empty(), cancellable)
    }

    @Test
    fun `observe flow`() = runUnconfinedTest {
        val stateFlow = MutableStateFlow("initial")

        val observable = CoroutineObservableValue(stateFlow)
        val items = mutableListOf<String>()
        observable.observe(items::add, this)

        stateFlow.emit("other")

        assertEquals(listOf("initial", "other"), items)
    }

    @Test
    fun `cancel unsubscribes flow`() = runUnconfinedTest {
        val stateFlow = MutableStateFlow("initial")

        val observable = CoroutineObservableValue(stateFlow)
        val items = mutableListOf<String>()
        val cancellable = observable.observe(items::add, this)
        cancellable.cancel()

        stateFlow.emit("other")

        assertEquals(0, stateFlow.subscriptionCount.first())
        assertEquals(listOf("initial"), items)
    }
}