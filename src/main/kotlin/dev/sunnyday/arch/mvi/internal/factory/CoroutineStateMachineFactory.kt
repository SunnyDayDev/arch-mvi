package dev.sunnyday.arch.mvi.internal.factory

import kotlinx.coroutines.CoroutineScope
import dev.sunnyday.arch.mvi.*
import dev.sunnyday.arch.mvi.factory.MviStateMachineFactory
import dev.sunnyday.arch.mvi.internal.StateMachineImpl
import dev.sunnyday.arch.mvi.internal.coroutine.MviCoroutineScope
import dev.sunnyday.arch.mvi.internal.coroutine.isMviCoroutineScope

internal class CoroutineStateMachineFactory : MviStateMachineFactory {

    override fun <State : Any, Event : Any, SideEffect : Any> createStateMachine(
        initialState: State,
        reducer: Reducer<State, Event, Update<State, SideEffect>>,
        stateTransitionListener: StateTransitionListener<StateTransition<State, Event, SideEffect>>?
    ): StateMachine<State, Event, SideEffect> {
        return createStateMachine(
            coroutineScope = MviCoroutineScope(),
            initialState = initialState,
            reducer = reducer,
            stateTransitionListener = stateTransitionListener,
        )
    }

    fun <State : Any, Event : Any, SideEffect : Any> createStateMachine(
        coroutineScope: CoroutineScope?,
        initialState: State,
        reducer: Reducer<State, Event, Update<State, SideEffect>>,
        stateTransitionListener: StateTransitionListener<StateTransition<State, Event, SideEffect>>?
    ): StateMachine<State, Event, SideEffect> {
        val stateMachineCoroutineScope = coroutineScope?.takeIf { it.isMviCoroutineScope }
            ?: MviCoroutineScope(coroutineScope)

        return StateMachineImpl(
            initialState = initialState,
            coroutineScope = stateMachineCoroutineScope,
            reducer = reducer,
            stateTransitionListener = stateTransitionListener,
        )
    }
}