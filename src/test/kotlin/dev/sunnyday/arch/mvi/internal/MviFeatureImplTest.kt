package dev.sunnyday.arch.mvi.internal

import dev.sunnyday.arch.mvi.EventHandler
import dev.sunnyday.arch.mvi.MviFeature
import dev.sunnyday.arch.mvi.SideEffectHandler
import dev.sunnyday.arch.mvi.StateMachine
import dev.sunnyday.arch.mvi.coroutines.toObservable
import dev.sunnyday.arch.mvi.primitive.OnReadyCallback
import dev.sunnyday.arch.mvi.test.*
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class MviFeatureImplTest {

    private val eventsFlow = MutableSharedFlow<Event>()
    private val sideEffectEventsFlow = MutableSharedFlow<Event>()
    private val sideEffectsFlow = MutableSharedFlow<SideEffect>()
    private val stateFlow = MutableStateFlow(State())

    private val stateMachine = mockk<StateMachine<State, Event, SideEffect>>(relaxed = true)
    private val eventHandler = mockk<EventHandler<InputEvent, Event>>(relaxed = true)
    private val sideEffectHandler = mockk<SideEffectHandler<SideEffect, Event>>(relaxed = true)

    @BeforeEach
    fun setUp() {
        every { eventHandler.outputEvents } returns eventsFlow.toObservable()
        every { sideEffectHandler.outputEvents } returns sideEffectEventsFlow.toObservable()
        every { stateMachine.sideEffects } returns sideEffectsFlow.toObservable()
        every { stateMachine.state } returns stateFlow.toObservable()
    }

    @Test
    fun `on start calls on event subscription ready`() = runTest {
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
    fun `events from eventHandler sends to state machine`() = runUnconfinedTest {
        val event = Event()
        createProcessor()

        eventsFlow.emit(event)

        excludeRecords { stateMachine.sideEffects }
        verify { stateMachine.onEvent(refEq(event)) }
        confirmVerified(stateMachine)
    }

    @Test
    fun `input event sends to event handler`() = runUnconfinedTest {
        val event = InputEvent()
        val processor = createProcessor()

        processor.onEvent(event)

        excludeRecords { eventHandler.outputEvents }
        verify { eventHandler.onEvent(refEq(event)) }
        confirmVerified(eventHandler)
    }

    @Test
    fun `side effects sends to side effect handler`() = runUnconfinedTest {
        val sideEffect = SideEffect()
        createProcessor()

        sideEffectsFlow.emit(sideEffect)

        excludeRecords { sideEffectHandler.outputEvents }
        verify { sideEffectHandler.onSideEffect(refEq(sideEffect)) }
        confirmVerified(sideEffectHandler)
    }

    @Test
    fun `processor state is state machine state`() = runUnconfinedTest {
        val processor = createProcessor()
        val states = processor.state.collectWithScope()

        stateFlow.emit(State("2"))

        assertEquals(listOf(State(), State("2")), states)
    }

    @Test
    fun `on cancel processor stops event handling`() = runUnconfinedTest {
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
        return MviFeatureImpl(
            coroutineScope = createTestSubScope(),
            eventHandler = eventHandler,
            stateMachine = stateMachine,
            sideEffectHandler = sideEffectHandler,
            onReadyCallback = onReadyCallback,
        )
    }
}