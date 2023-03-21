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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.test.advanceUntilIdle
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
    fun `toFlow on other Observable properly maps to flow`() = runUnconfinedTest {
        val cancellable = mockk<Cancellable>(relaxed = true)
        val observable = Observable { observer ->
            observer.invoke("one")
            observer.invoke("two")
            cancellable
        }

        val flowItems = observable.toFlow()
            .take(2)
            .collectWithScope()

        assertEquals(flowItems, listOf("one", "two"))
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