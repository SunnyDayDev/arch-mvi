package dev.sunnyday.arch.mvi.coroutines

import dev.sunnyday.arch.mvi.internal.primitive.CoroutineObservable
import dev.sunnyday.arch.mvi.internal.primitive.CoroutineObservableValue
import dev.sunnyday.arch.mvi.primitive.Cancellable
import dev.sunnyday.arch.mvi.primitive.Observable
import dev.sunnyday.arch.mvi.primitive.ObservableValue
import dev.sunnyday.arch.mvi.test.collectWithScope
import dev.sunnyday.arch.mvi.test.runUnconfinedTest
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import kotlin.properties.Delegates
import kotlin.test.assertEquals
import kotlin.test.assertSame

@OptIn(ExperimentalCoroutinesApi::class)
class FlowMappingsKtTest {

    @Test
    fun `toFlow on CoroutineObservables just takes it's flow`() {
        val flow = flow<Unit> { }
        val observable = CoroutineObservable(flow)

        val resultFlow = observable.toFlow()

        assertSame(resultFlow, flow)
    }

    @Test
    fun `toFlow on other Observable properly maps to flow`() = runTest {
        val cancellable = mockk<Cancellable>(relaxed = true)
        val emittedValues = mutableListOf<String>()
        val observable = Observable { observer ->
            observer.invoke("one")
            emittedValues.add("one")
            observer.invoke("two")
            emittedValues.add("two")
            cancellable
        }

        val flowItems = observable.toFlow()
            .flowOn(Dispatchers.Default)
            .buffer(0)
            .onEach { if (it == "one") delay(100) }
            .take(2)
            .collectWithScope()

        advanceTimeBy(50)
        assertEquals(listOf("one"), emittedValues)

        advanceTimeBy(100)
        assertEquals(listOf("one", "two"), emittedValues)

        assertEquals(listOf("one", "two"), flowItems)
        verify { cancellable.cancel() }
    }


    @Test
    fun `toStateFlow on CoroutineObservableValues just takes it's state flow`() {
        val stateFlow = MutableStateFlow(1)
        val observable = CoroutineObservableValue(stateFlow)

        val resultFlow = observable.toFlow()

        assertSame(resultFlow, stateFlow)
    }

    @Test
    fun `toStateFlow on other ObservableValues properly maps to flow`() = runUnconfinedTest {
        val cancellable = mockk<Cancellable>(relaxed = true)
        val observable = object  : ObservableValue<String> {

            override var value: String by Delegates.observable( "value") { _, _, newValue ->
                observer.invoke(newValue)
            }

            lateinit var observer: (String) -> Unit

            override fun observe(observer: (String) -> Unit): Cancellable {
                this.observer = observer
                observer.invoke(value)
                return cancellable
            }
        }

        val scope = CoroutineScope(currentCoroutineContext() + SupervisorJob())

        val stateFlow = observable.toStateFlow(scope)
        val flowItems = stateFlow
            .take(3)
            .collectWithScope()

        assertEquals(stateFlow.value, "value")

        observable.value = "one"
        observable.value = "two"

        scope.cancel()

        assertEquals("two", stateFlow.value)
        assertEquals(listOf("value", "one", "two"), flowItems)
        verify { cancellable.cancel() }
    }
}