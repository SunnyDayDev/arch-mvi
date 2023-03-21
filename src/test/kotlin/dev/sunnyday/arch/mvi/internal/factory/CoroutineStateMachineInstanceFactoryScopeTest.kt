package dev.sunnyday.arch.mvi.internal.factory

import dev.sunnyday.arch.mvi.*
import dev.sunnyday.arch.mvi.factory.MviStateMachineFactory
import dev.sunnyday.arch.mvi.test.Event
import dev.sunnyday.arch.mvi.test.SideEffect
import dev.sunnyday.arch.mvi.test.State
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.CoroutineScope
import org.junit.jupiter.api.Test
import kotlin.test.assertSame

class CoroutineStateMachineInstanceFactoryScopeTest {

    @Test
    fun `create state machine by provided factory`() {
        val factory = mockk<MviStateMachineFactory>()
        val coroutineScope = mockk<CoroutineScope>()
        val initialState = State()
        val stateMachine = mockk<StateMachine<State, Event, SideEffect>>()
        val reducer = mockk<Reducer<State, Event, Update<State, SideEffect>>>()
        val stateTransitionListener = mockk<StateTransitionListener<StateTransition<State, Event, SideEffect>>>()

        every { factory.createStateMachine<State, Event, SideEffect>(any(), any(), any()) } returns stateMachine

        val actualStateMachine = CoroutineStateMachineInstanceFactoryScope<State, Event, SideEffect>(
            stateMachineFactory = factory,
            coroutineScope = coroutineScope,
            initialState = initialState
        )
            .createStateMachine(
                reducer = reducer,
                stateTransitionListener = stateTransitionListener,
            )

        assertSame(stateMachine, actualStateMachine)
        verify {
            factory.createStateMachine(
                initialState = initialState,
                reducer = reducer,
                stateTransitionListener = stateTransitionListener,
            )
        }
        confirmVerified(factory)
    }

    @Test
    fun `if factory is CoroutineStateMachineFactory use internal optimized method`() {
        val factory = mockk<CoroutineStateMachineFactory>()
        val coroutineScope = mockk<CoroutineScope>()
        val initialState = State()
        val stateMachine = mockk<StateMachine<State, Event, SideEffect>>()
        val reducer = mockk<Reducer<State, Event, Update<State, SideEffect>>>()
        val stateTransitionListener = mockk<StateTransitionListener<StateTransition<State, Event, SideEffect>>>()

        every { factory.createStateMachine<State, Event, SideEffect>(any(), any(), any(), any()) } returns stateMachine

        val actualStateMachine = CoroutineStateMachineInstanceFactoryScope<State, Event, SideEffect>(
            stateMachineFactory = factory,
            coroutineScope = coroutineScope,
            initialState = initialState
        )
            .createStateMachine(
                reducer = reducer,
                stateTransitionListener = stateTransitionListener,
            )

        assertSame(stateMachine, actualStateMachine)
        verify {
            factory.createStateMachine(
                coroutineScope = coroutineScope,
                initialState = initialState,
                reducer = reducer,
                stateTransitionListener = stateTransitionListener,
            )
        }
        confirmVerified(factory)
    }
}