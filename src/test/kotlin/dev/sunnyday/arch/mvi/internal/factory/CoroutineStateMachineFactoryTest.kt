package dev.sunnyday.arch.mvi.internal.factory

import dev.sunnyday.arch.mvi.Reducer
import dev.sunnyday.arch.mvi.StateTransition
import dev.sunnyday.arch.mvi.StateTransitionListener
import dev.sunnyday.arch.mvi.Update
import dev.sunnyday.arch.mvi.factory.MviStateMachineFactory
import dev.sunnyday.arch.mvi.internal.StateMachineImpl
import dev.sunnyday.arch.mvi.internal.coroutine.MviCoroutineMarker
import dev.sunnyday.arch.mvi.internal.coroutine.MviCoroutineScope
import dev.sunnyday.arch.mvi.test.ConstructorRule
import dev.sunnyday.arch.mvi.test.Event
import dev.sunnyday.arch.mvi.test.SideEffect
import dev.sunnyday.arch.mvi.test.State
import io.mockk.*
import kotlinx.coroutines.CoroutineScope
import org.junit.jupiter.api.Test
import kotlin.coroutines.EmptyCoroutineContext

class CoroutineStateMachineFactoryTest {

    @Test
    fun `construct state machine with mvi parent coroutine scope`() = mockkConstructor(StateMachineImpl::class) {
        val state = State()
        val reducer = Reducer<State, Event, Update<State, SideEffect>> { _, _ -> Update.nothing() }
        val coroutineScope = MviCoroutineScope()
        val transitionListener = StateTransitionListener<StateTransition<State, Event, SideEffect>> { }

        val rule = ConstructorRule.create<StateMachineImpl<State, Event, SideEffect>>(
            EqMatcher(state, true),
            EqMatcher(coroutineScope, true),
            EqMatcher(reducer, true),
            EqMatcher(transitionListener, true),
        )

        val obj = CoroutineStateMachineFactory().createStateMachine(
            coroutineScope = coroutineScope,
            initialState = state,
            reducer = reducer,
            stateTransitionListener = transitionListener,
        )

        rule.verifyConstructorCalled(obj)
    }

    @Test
    fun `construct state machine with any parent coroutine scope`() {
        mockkConstructor(StateMachineImpl::class)
        mockkStatic(::MviCoroutineScope)

        val state = State()
        val reducer = Reducer<State, Event, Update<State, SideEffect>> { _, _ -> Update.nothing() }
        val coroutineScope = CoroutineScope(EmptyCoroutineContext + MviCoroutineMarker)
        val transitionListener = StateTransitionListener<StateTransition<State, Event, SideEffect>> { }

        every { MviCoroutineScope(any()) } returns coroutineScope

        val rule = ConstructorRule.create<StateMachineImpl<State, Event, SideEffect>>(
            EqMatcher(state, true),
            EqMatcher(coroutineScope, true),
            EqMatcher(reducer, true),
            EqMatcher(transitionListener, true),
        )

        val obj = CoroutineStateMachineFactory().createStateMachine(
            coroutineScope = CoroutineScope(EmptyCoroutineContext),
            initialState = state,
            reducer = reducer,
            stateTransitionListener = transitionListener,
        )

        rule.verifyConstructorCalled(obj)

        unmockkAll()
    }

    @Test
    fun `construct state machine without parent coroutine scope`() {
        mockkConstructor(StateMachineImpl::class)
        mockkStatic(::MviCoroutineScope)

        val state = State()
        val reducer = Reducer<State, Event, Update<State, SideEffect>> { _, _ -> Update.nothing() }
        val coroutineScope = CoroutineScope(EmptyCoroutineContext + MviCoroutineMarker)
        val transitionListener = StateTransitionListener<StateTransition<State, Event, SideEffect>> { }

        every { MviCoroutineScope(any()) } returns coroutineScope

        val rule = ConstructorRule.create<StateMachineImpl<State, Event, SideEffect>>(
            EqMatcher(state, true),
            EqMatcher(coroutineScope, true),
            EqMatcher(reducer, true),
            EqMatcher(transitionListener, true),
        )

        val factory: MviStateMachineFactory = CoroutineStateMachineFactory()
        val obj = factory.createStateMachine(
            initialState = state,
            reducer = reducer,
            stateTransitionListener = transitionListener,
        )

        rule.verifyConstructorCalled(obj)

        unmockkAll()
    }
}