package dev.sunnyday.arch.mvi.factory

import kotlinx.coroutines.CoroutineScope
import dev.sunnyday.arch.mvi.*
import dev.sunnyday.arch.mvi.internal.StateMachineImpl

object MviStateMachineFactory {

    fun <State : Any, Event : Any, SideEffect : Any> createStateMachine(
        coroutineScope: CoroutineScope,
        initialState: State,
        reducer: Reducer<State, Event, Update<State, SideEffect>>,
        stateTransitionListener: StateTransitionListener<StateTransition<State, Event, SideEffect>>? = null,
    ): StateMachine<State, Event, SideEffect> {
        return StateMachineImpl(
            initialState = initialState,
            coroutineScope = coroutineScope,
            reducer = reducer,
            stateTransitionListener = stateTransitionListener,
        )
    }
}