package dev.sunnyday.arch.mvi.coroutine

import dev.sunnyday.arch.mvi.EventHandler
import dev.sunnyday.arch.mvi.MviFeature
import dev.sunnyday.arch.mvi.SideEffectHandler
import dev.sunnyday.arch.mvi.StateMachine
import dev.sunnyday.arch.mvi.coroutine.ktx.toObservable
import dev.sunnyday.arch.mvi.coroutine.primitive.CoroutineObservableEvent
import dev.sunnyday.arch.mvi.coroutine.test.CoroutineObservableEventFlowProvider
import dev.sunnyday.arch.mvi.primitive.ObservableEvent
import dev.sunnyday.arch.mvi.primitive.OnReadyCallback
import dev.sunnyday.arch.mvi.test.*
import dev.sunnyday.arch.mvi.test.createTestSubScope
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class CoroutineMviFeatureTest {

    private val eventsFlow = MutableSharedFlow<Event>()
    private val sideEffectEventsFlow = MutableSharedFlow<Event>()
    private val sideEffectsFlow = MutableSharedFlow<SideEffect>()
    private val stateFlow = MutableStateFlow(State())

    private val stateMachine = mockk<StateMachine<State, Event, SideEffect>>(relaxed = true)
    private val eventHandler = mockk<EventHandler<InputEvent, Event>>(relaxed = true)
    private val sideEffectHandler = mockk<SideEffectHandler<SideEffect, Event>>(relaxed = true)
    
    private val testScope = TestScope(UnconfinedTestDispatcher())

    @BeforeEach
    fun setUp() {
        CoroutineTestUtil.observableEventFlowProvider = CoroutineObservableEventFlowProvider()
        
        every { eventHandler.outputEvents } returns eventsFlow.toObservable(testScope)
        every { sideEffectHandler.outputEvents } returns sideEffectEventsFlow.toObservable(testScope)
        every { stateMachine.sideEffects } returns sideEffectsFlow.toObservable(testScope)
        every { stateMachine.state } returns stateFlow.toObservable(testScope)
    }

    @Test
    fun `on start calls on event subscription ready`() = testScope.runTest {
        every { eventHandler.outputEvents } returns eventsFlow
            .onStart { emit(Event("e:1")) }
            .toObservable()
        every { sideEffectHandler.outputEvents } returns sideEffectEventsFlow
            .onStart { emit(Event("s:1")) }
            .toObservable()

        createProcessor {
            launch {
                delay(1)
                eventsFlow.emit(Event("e:2"))
                sideEffectEventsFlow.emit(Event("s:2"))
            }
        }

        advanceUntilIdle()

        excludeRecords { stateMachine.sideEffects }
        verifyAll {
            stateMachine.onEvent(Event("e:1"))
            stateMachine.onEvent(Event("e:2"))
            stateMachine.onEvent(Event("s:1"))
            stateMachine.onEvent(Event("s:2"))
        }
        confirmVerified(stateMachine)
    }

    @Test
    fun `events from eventHandler sends to state machine`() = testScope.runTest {
        val event = Event()
        createProcessor()

        eventsFlow.emit(event)

        excludeRecords { stateMachine.sideEffects }
        verify { stateMachine.onEvent(refEq(event)) }
        confirmVerified(stateMachine)
    }

    @Test
    fun `input event sends to event handler`() = testScope.runTest {
        val event = InputEvent()
        val processor = createProcessor()

        processor.onEvent(event)

        excludeRecords { eventHandler.outputEvents }
        verify { eventHandler.onEvent(refEq(event)) }
        confirmVerified(eventHandler)
    }

    @Test
    fun `side effects sends to side effect handler`() = testScope.runTest {
        val sideEffect = SideEffect()
        createProcessor()

        sideEffectsFlow.emit(sideEffect)

        excludeRecords { sideEffectHandler.outputEvents }
        verify { sideEffectHandler.onSideEffect(refEq(sideEffect)) }
        confirmVerified(sideEffectHandler)
    }

    @Test
    fun `processor state is state machine state`() = testScope.runTest {
        val processor = createProcessor()
        val states = processor.state.collectWithScope()

        stateFlow.emit(State("2"))

        assertEquals(listOf(State(), State("2")), states)
    }

    @Test
    fun `on cancel processor stops event handling`() = testScope.runTest {
        val processor = createProcessor()
        processor.cancel()

        processor.onEvent(InputEvent())
        sideEffectsFlow.emit(SideEffect())
        eventsFlow.emit(Event())

        excludeRecords {
            eventHandler.outputEvents
            sideEffectHandler.outputEvents
            stateMachine.sideEffects
        }
        verify { stateMachine.cancel() }
        confirmVerified(eventHandler, stateMachine, sideEffectHandler)
    }

    private suspend fun createProcessor(onReadyCallback: OnReadyCallback? = null): MviFeature<State, InputEvent> {
        return CoroutineMviFeature(
            coroutineScope = createTestSubScope(),
            eventHandler = eventHandler,
            stateMachine = stateMachine,
            sideEffectHandler = sideEffectHandler,
            onReadyCallback = onReadyCallback,
        )
    }
}