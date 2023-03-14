package dev.sunnyday.arch.mvi.internal

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.*
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.ArgumentsProvider
import org.junit.jupiter.params.provider.ArgumentsSource
import dev.sunnyday.arch.mvi.Reducer
import dev.sunnyday.arch.mvi.StateTransition
import dev.sunnyday.arch.mvi.StateTransitionListener
import dev.sunnyday.arch.mvi.Update
import dev.sunnyday.arch.mvi.test.collectWithScope
import java.util.concurrent.TimeUnit
import java.util.stream.Stream
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
@Timeout(3, unit = TimeUnit.SECONDS)
class StateMachineImplTest {

    private val reducer = mockk<Reducer<State, Event, Update<State, SideEffect>>>()

    @Test
    fun `starts with initial state`() = runTest(UnconfinedTestDispatcher()) {
        val expectedInitialState = State("initial")
        val stateMachine = createStateMachine(initialState = expectedInitialState)
        assertEquals(expectedInitialState, stateMachine.state.value)
    }

    @Test
    fun `on event, reduce state`() = runTest(UnconfinedTestDispatcher()) {
        val initialState = State("initial")
        val expectedState = State("reduced")
        val event = Event("event")
        every { reducer.reduce(any(), any()) } returns Update.state(expectedState)
        val stateMachine = createStateMachine(initialState = initialState)

        stateMachine.onEvent(event)

        assertEquals(expectedState, stateMachine.state.value)
        verify { reducer.reduce(initialState, event) }
    }

    @ParameterizedTest
    @ArgumentsSource(NoStateUpdatesProvider::class)
    fun `if state isn't changed on event, nothing happens`(
        update: Update<State, SideEffect>,
    ) = runTest(UnconfinedTestDispatcher()) {
        every { reducer.reduce(any(), any()) } returns update

        val stateMachine = createStateMachine()
        val statesLog = stateMachine.state.collectWithScope()
        val initialSize = statesLog.size

        stateMachine.onEvent(Event())

        assertTrue(statesLog.drop(initialSize).isEmpty())
        verify(exactly = 1) { reducer.reduce(any(), any()) }
    }

    @Test
    fun `on event, reduce side effects`() = runTest(UnconfinedTestDispatcher()) {
        val event = Event("event")
        val sideEffect = SideEffect("sideEffect")
        every { reducer.reduce(any(), any()) } returns Update.sideEffects(sideEffect)
        val stateMachine = createStateMachine()

        val sidEffects = stateMachine.sideEffects.collectWithScope()
        stateMachine.onEvent(event)

        assertEquals(listOf(sideEffect), sidEffects)
    }

    @Test
    fun `sideEffects doesn't replay`() = runTest(UnconfinedTestDispatcher()) {
        every { reducer.reduce(any(), any()) } returns Update.sideEffects(SideEffect())
        val stateMachine = createStateMachine()
        stateMachine.sideEffects.collectWithScope() // first subscriber side effects

        stateMachine.onEvent(Event())
        val nextSubscriberSideEffects = stateMachine.sideEffects.collectWithScope()

        assertTrue(nextSubscriberSideEffects.isEmpty())
    }

    @Test
    fun `sideEffects awaits for the first subscriber`() = runTest(UnconfinedTestDispatcher()) {
        val sideEffect = SideEffect("some")
        every { reducer.reduce(any(), any()) } returns Update.sideEffects(listOf(sideEffect))
        val stateMachine = createStateMachine()

        stateMachine.onEvent(Event())
        val firstSubscriberSideEffects = stateMachine.sideEffects.collectWithScope()

        assertEquals(listOf(sideEffect), firstSubscriberSideEffects)
    }

    @ParameterizedTest
    @ArgumentsSource(NoSideEffectUpdatesProvider::class)
    fun `if sideEffects isn't reduced on event, nothing happens`(
        update: Update<State, SideEffect>,
    ) = runTest(UnconfinedTestDispatcher()) {
        every { reducer.reduce(any(), any()) } returns update

        val stateMachine = createStateMachine()
        val sideEffects = stateMachine.sideEffects.collectWithScope()

        stateMachine.onEvent(Event())

        assertTrue(sideEffects.isEmpty())
        verify(exactly = 1) { reducer.reduce(any(), any()) }
    }

    @Test
    fun `on event, can reduce both, state and sideEffect`() = runTest(UnconfinedTestDispatcher()) {
        every { reducer.reduce(any(), any()) } returnsMany listOf(
            Update.stateWithSideEffects(State("1"), SideEffect("1")),
            Update.stateWithSideEffects(State("2"), listOf(SideEffect("2"))),
        )

        val stateMachine = createStateMachine()
        val states = stateMachine.state.collectWithScope()
        val sideEffects = stateMachine.sideEffects.collectWithScope()

        stateMachine.onEvent(Event())
        stateMachine.onEvent(Event())

        assertEquals(
            listOf(State("1"), State("2")),
            states.drop(1),
        )
        assertEquals(
            listOf(SideEffect("1"), SideEffect("2")),
            sideEffects,
        )
    }

    @ParameterizedTest
    @ArgumentsSource(AllUpdatesProvider::class)
    fun `on event, notify state transition listener`(
        update: Update<State, SideEffect>,
    ) = runTest(UnconfinedTestDispatcher()) {
        val initialState = State("initial")
        val triggerEvent = Event("trigger")
        every { reducer.reduce(any(), any()) } returns update

        val stateTransitionLog = mutableListOf<StateTransition<State, Event, SideEffect>>()

        val stateMachine = createStateMachine(
            initialState = initialState,
            stateTransitionListener = stateTransitionLog::add,
        )

        stateMachine.onEvent(triggerEvent)

        assertEquals(
            StateTransition(
                previousState = initialState,
                newState = update.state ?: initialState,
                triggerEvent = triggerEvent,
                sideEffects = update.sideEffects,
            ),
            stateTransitionLog.singleOrNull()
        )
    }

    private suspend fun createStateMachine(
        initialState: State = State(),
        stateTransitionListener: StateTransitionListener<StateTransition<State, Event, SideEffect>>? = null,
    ): StateMachineImpl<State, Event, SideEffect> {
        return StateMachineImpl(
            initialState = initialState,
            coroutineScope = createStateMachineCoroutineScope(),
            reducer = reducer,
            stateTransitionListener = stateTransitionListener,
        )
    }

    private suspend fun createStateMachineCoroutineScope(): CoroutineScope {
        val testCoroutineContext = currentCoroutineContext()
        val testJob = testCoroutineContext.job
        val stateMachineCoroutineScope = CoroutineScope(testCoroutineContext + SupervisorJob())
        stateMachineCoroutineScope.launch {
            testJob.join()
            cancel()
        }

        return stateMachineCoroutineScope
    }

    data class State(val name: String = "state")
    data class Event(val name: String = "event")
    data class SideEffect(val name: String = "sideEffect")

    class AllUpdatesProvider : ArgumentsProvider {

        override fun provideArguments(context: ExtensionContext?): Stream<out Arguments> {
            return Stream.of(
                Update.nothing(),
                Update.sideEffects(),
                Update.state(State("2")),
                Update.stateWithSideEffects(State("3")),
                Update.sideEffects(SideEffect("4")),
                Update.sideEffects(listOf(SideEffect("5"))),
                Update.stateWithSideEffects(State("6"), SideEffect("6")),
                Update.stateWithSideEffects(State("7"), listOf(SideEffect("7"))),
            ).map(Arguments::of)
        }
    }

    class NoSideEffectUpdatesProvider : ArgumentsProvider {

        override fun provideArguments(context: ExtensionContext?): Stream<out Arguments> {
            return Stream.of(
                Update.nothing(),
                Update.state(State("1")),
                Update.stateWithSideEffects(State("2")),
                Update.stateWithSideEffects(State("3")),
            ).map(Arguments::of)
        }
    }

    class NoStateUpdatesProvider : ArgumentsProvider {

        override fun provideArguments(context: ExtensionContext?): Stream<out Arguments> {
            return Stream.of(
                Update.nothing(),
                Update.sideEffects(SideEffect("1")),
                Update.sideEffects(listOf(SideEffect("2"))),
            ).map(Arguments::of)
        }
    }
}