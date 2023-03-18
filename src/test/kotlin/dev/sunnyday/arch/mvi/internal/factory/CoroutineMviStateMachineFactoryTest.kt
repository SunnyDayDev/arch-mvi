package dev.sunnyday.arch.mvi.internal.factory

import dev.sunnyday.arch.mvi.Reducer
import dev.sunnyday.arch.mvi.StateTransition
import dev.sunnyday.arch.mvi.StateTransitionListener
import dev.sunnyday.arch.mvi.Update
import dev.sunnyday.arch.mvi.internal.StateMachineImpl
import dev.sunnyday.arch.mvi.test.Event
import dev.sunnyday.arch.mvi.test.SideEffect
import dev.sunnyday.arch.mvi.test.State
import io.mockk.*
import kotlinx.coroutines.CoroutineScope
import org.junit.jupiter.api.Test

class CoroutineMviStateMachineFactoryTest {

    @Test
    fun `construct state machine`() = mockkConstructor(StateMachineImpl::class) {
        val state = State()
        val reducer = Reducer<State, Event, Update<State, SideEffect>> { _, _ -> Update.nothing() }

        every { constructed(state, reducer).onEvent(any()) } returns Unit

        CoroutineMviStateMachineFactory.createStateMachine(
            initialState = state,
            reducer = reducer,
        ).onEvent(Event())

        verify { constructed(state, reducer).onEvent(any()) }
    }

    private fun MockKMatcherScope.constructed(
        state: State,
        reducer: Reducer<State, Event, Update<State, SideEffect>>,
    ): StateMachineImpl<State, Event, SideEffect> {
        return constructedWith(
            EqMatcher(state, true),
            ConstantMatcher<CoroutineScope>(true),
            EqMatcher(reducer, true),
            ConstantMatcher<StateTransitionListener<StateTransition<State, Event, SideEffect>>>(true)
        )
    }
}