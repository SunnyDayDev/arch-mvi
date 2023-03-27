package dev.sunnyday.arch.mvi.factory.instance

import dev.sunnyday.arch.mvi.*

fun interface StateMachineInstanceFactory<State : Any, Event : Any, SideEffect : Any> {

    fun FactoryScope<State, Event, SideEffect>.createStateMachine(): StateMachine<State, Event, SideEffect>

    interface FactoryScope<State : Any, Event : Any, SideEffect : Any> {

        fun createStateMachine(
            reducer: Reducer<State, Event, Update<State, SideEffect>>,
            stateTransitionListener: StateTransitionListener<StateTransition<State, Event, SideEffect>>? = null,
        ): StateMachine<State, Event, SideEffect>
    }
}