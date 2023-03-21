package dev.sunnyday.arch.mvi.internal.primitive

import dev.sunnyday.arch.mvi.coroutines.toFlow
import dev.sunnyday.arch.mvi.internal.coroutine.MviCoroutineScope
import dev.sunnyday.arch.mvi.primitive.Cancellable
import dev.sunnyday.arch.mvi.primitive.Observable
import dev.sunnyday.arch.mvi.test.collectWithScope
import dev.sunnyday.arch.mvi.test.runUnconfinedTest
import io.mockk.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.test.assertEquals
import kotlin.test.assertSame

@OptIn(ExperimentalCoroutinesApi::class)
class CoroutineObservableTest {

    @Test
    fun `observe flow`() = runUnconfinedTest {
        val flow = flowOf("1", "2", "3")

        val observable = CoroutineObservable(flow)
        val items = mutableListOf<String>()
        observable.observe(items::add, this)

        assertEquals(listOf("1", "2", "3"), items)
    }

    @Test
    fun `cancel flow`() = runTest {
        val flow = flow {
            emit(1)
            delay(10)
            emit(2)
            delay(100)
            emit(3)
        }

        val observable = CoroutineObservable(flow)
        val items = mutableListOf<Int>()
        val cancellable = observable.observe(items::add, this)

        runCurrent()
        assertEquals(listOf(1), items)

        advanceTimeBy(10)
        runCurrent()
        assertEquals(listOf(1, 2), items)

        cancellable.cancel()

        advanceTimeBy(100)
        runCurrent()
        assertEquals(listOf(1, 2), items)
    }

    @Test
    fun `if coroutine is innactive do nothing`() = runUnconfinedTest {
        val flow = flowOf("1")

        val scope = CoroutineScope(SupervisorJob())
        scope.cancel()

        val observable = CoroutineObservable(flow)
        val items = mutableListOf<String>()
        val cancellable = observable.observe(items::add, scope)

        assertEquals(emptyList(), items)
        assertSame(Cancellable.empty(), cancellable)
    }

    @Test
    fun `observe non coroutine observable`() = runTest {
        mockkStatic(::MviCoroutineScope) {
            every { MviCoroutineScope(any()) } returns this

            val items = mutableListOf<Int>()
            val observable = CoroutineObservable(flowOf(1, 2))

            observable.observe(items::add)
            runCurrent()

            assertEquals(listOf(1, 2), items)
        }
    }
}