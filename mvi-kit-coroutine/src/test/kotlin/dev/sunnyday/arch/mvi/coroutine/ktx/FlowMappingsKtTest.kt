package dev.sunnyday.arch.mvi.coroutine.ktx

import dev.sunnyday.arch.mvi.coroutine.primitive.CoroutineObservableEvent
import dev.sunnyday.arch.mvi.coroutine.primitive.CoroutineObservableState
import dev.sunnyday.arch.mvi.primitive.*
import dev.sunnyday.arch.mvi.test.ConstructorRule
import dev.sunnyday.arch.mvi.test.collectWithScope
import dev.sunnyday.arch.mvi.test.runUnconfinedTest
import io.mockk.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.junit.jupiter.api.Test
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.properties.Delegates
import kotlin.test.assertEquals
import kotlin.test.assertSame

@OptIn(ExperimentalCoroutinesApi::class)
class FlowMappingsKtTest {

    @Test
    fun `toFlow on CoroutineObservables just takes it's flow`() {
        val flow = flow<Unit> { }
        val observable = CoroutineObservableEvent(flow)

        val resultFlow = observable.toFlow()

        assertSame(resultFlow, flow)
    }

    @Test
    fun `toFlow on other Observable properly maps to flow`() = runUnconfinedTest {
        val cancellable = mockk<Cancellable>(relaxed = true)
        val observable = ObservableEvent { observer ->
            observer.onEvent("one")
            observer.onEvent("two")
            cancellable
        }

        val flowItems = observable.toFlow()
            .take(2)
            .collectWithScope()

        assertEquals(listOf("one", "two"), flowItems)
        verify { cancellable.cancel() }
    }

    @Test
    fun `toStateFlow on CoroutineObservableValues just takes it's state flow`() {
        val stateFlow = MutableStateFlow(1)
        val observable = CoroutineObservableState(stateFlow)

        val resultFlow = observable.toStateFlow(CoroutineScope(EmptyCoroutineContext))

        assertSame(resultFlow, stateFlow)
    }

    @Test
    fun `toStateFlow on other ObservableValues properly maps to flow`() = runUnconfinedTest {
        val cancellable = mockk<Cancellable>(relaxed = true)
        val observable = object : ObservableState<String> {

            override var value: String by Delegates.observable("value") { _, _, newValue ->
                observer.onEvent(newValue)
            }

            lateinit var observer: EventConsumer<String>

            override fun observe(observer: EventConsumer<String>): Cancellable {
                this.observer = observer
                observer.onEvent(value)
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

    @Test
    fun `toObservable returns coroutine`() = mockkConstructor(CoroutineObservableEvent::class) {
        val flow = emptyFlow<Unit>()
        val coroutineScope = CoroutineScope(EmptyCoroutineContext)

        val observableConstructorRule = ConstructorRule.create<CoroutineObservableEvent<Unit>>(
            EqMatcher(flow, ref = true),
            EqMatcher(coroutineScope, ref = true)
        )

        val observable = flow.toObservable(coroutineScope)

        observableConstructorRule.verifyConstructorCalled(observable)
    }

    @Test
    fun `default toObservable returns coroutine`() = mockkConstructor(CoroutineObservableEvent::class) {
        val flow = emptyFlow<Unit>()

        val observableConstructorRule = ConstructorRule.create<CoroutineObservableEvent<Unit>>(
            EqMatcher(flow, ref = true),
            NullCheckMatcher<CoroutineScope>(),
        )

        val observable = flow.toObservable()

        observableConstructorRule.verifyConstructorCalled(observable)
    }

    @Test
    fun `toObservableState returns coroutine`() = mockkConstructor(CoroutineObservableState::class) {
        val flow = MutableStateFlow(Unit)
        val coroutineScope = CoroutineScope(EmptyCoroutineContext)

        val observableConstructorRule = ConstructorRule.create<CoroutineObservableState<Unit>>(
            EqMatcher(flow, ref = true),
            EqMatcher(coroutineScope, ref = true)
        )

        val observable = flow.toObservable(coroutineScope)

        observableConstructorRule.verifyConstructorCalled(observable)
    }

    @Test
    fun `default toObservableState returns coroutine`() = mockkConstructor(CoroutineObservableState::class) {
        val flow = MutableStateFlow(Unit)

        val observableConstructorRule = ConstructorRule.create<CoroutineObservableState<Unit>>(
            EqMatcher(flow, ref = true),
            NullCheckMatcher<CoroutineScope>(),
        )

        val observable = flow.toObservable()

        observableConstructorRule.verifyConstructorCalled(observable)
    }
}