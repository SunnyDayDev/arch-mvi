package dev.sunnyday.arch.mvi.internal.factory

import dev.sunnyday.arch.mvi.Reducer
import dev.sunnyday.arch.mvi.StateTransition
import dev.sunnyday.arch.mvi.StateTransitionListener
import dev.sunnyday.arch.mvi.Update
import dev.sunnyday.arch.mvi.internal.StateMachineImpl
import dev.sunnyday.arch.mvi.internal.coroutine.MviCoroutineScope
import dev.sunnyday.arch.mvi.internal.coroutine.isMviCoroutineScope
import dev.sunnyday.arch.mvi.test.ConstructorRule
import dev.sunnyday.arch.mvi.test.ConstructorRule.Companion.Matcher
import dev.sunnyday.arch.mvi.test.Event
import dev.sunnyday.arch.mvi.test.SideEffect
import dev.sunnyday.arch.mvi.test.State
import io.mockk.*
import kotlinx.coroutines.CoroutineScope
import org.junit.jupiter.api.Test
import kotlin.coroutines.EmptyCoroutineContext

class CoroutineMviStateMachineFactoryTest {

    @Test
    fun `construct state machine`() = mockkConstructor(StateMachineImpl::class) {
        val state = State()
        val reducer = Reducer<State, Event, Update<State, SideEffect>> { _, _ -> Update.nothing() }

        val rule = ConstructorRule.create<StateMachineImpl<State, Event, SideEffect>>(
            EqMatcher(state, true),
            Matcher<CoroutineScope> { it.isMviCoroutineScope },
            EqMatcher(reducer, true),
            ConstantMatcher<StateTransitionListener<StateTransition<State, Event, SideEffect>>>(true)
        )

        val obj = CoroutineMviStateMachineFactory.createStateMachine(
            initialState = state,
            reducer = reducer,
        )

        rule.verifyConstructorCalled(obj)
    }

    @Test
    fun `construct state machine with mvi coroutine scope`() = mockkConstructor(StateMachineImpl::class) {
        val state = State()
        val reducer = Reducer<State, Event, Update<State, SideEffect>> { _, _ -> Update.nothing() }
        val coroutineScope = MviCoroutineScope()

        val rule = ConstructorRule.create<StateMachineImpl<State, Event, SideEffect>>(
            EqMatcher(state, true),
            EqMatcher(coroutineScope, true),
            EqMatcher(reducer, true),
            ConstantMatcher<StateTransitionListener<StateTransition<State, Event, SideEffect>>>(true)
        )

        val obj = CoroutineMviStateMachineFactory.createStateMachine(
            initialState = state,
            reducer = reducer,
            coroutineScope = coroutineScope,
        )

        rule.verifyConstructorCalled(obj)
    }

    @Test
    fun `construct state machine with any coroutine scope`() = mockkConstructor(StateMachineImpl::class) {
        val state = State()
        val reducer = Reducer<State, Event, Update<State, SideEffect>> { _, _ -> Update.nothing() }
        val coroutineScope = CoroutineScope(EmptyCoroutineContext)

        val rule = ConstructorRule.create<StateMachineImpl<State, Event, SideEffect>>(
            EqMatcher(state, true),
            Matcher<CoroutineScope> { it !== coroutineScope && it.isMviCoroutineScope },
            EqMatcher(reducer, true),
            ConstantMatcher<StateTransitionListener<StateTransition<State, Event, SideEffect>>>(true)
        )

        val obj = CoroutineMviStateMachineFactory.createStateMachine(
            initialState = state,
            reducer = reducer,
            coroutineScope = coroutineScope,
        )

        rule.verifyConstructorCalled(obj)
    }
}