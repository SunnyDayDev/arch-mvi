package dev.sunnyday.arch.mvi.factory

import dev.sunnyday.arch.mvi.*

interface MviStateMachineFactory {

    fun <State : Any, Event : Any, SideEffect : Any> createStateMachine(
        initialState: State,
        reducer: Reducer<State, Event, Update<State, SideEffect>>,
        stateTransitionListener: StateTransitionListener<StateTransition<State, Event, SideEffect>>? = null,
    ): StateMachine<State, Event, SideEffect>
}