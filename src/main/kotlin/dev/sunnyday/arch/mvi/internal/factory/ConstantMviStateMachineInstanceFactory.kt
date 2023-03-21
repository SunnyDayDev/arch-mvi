package dev.sunnyday.arch.mvi.internal.factory

import dev.sunnyday.arch.mvi.StateMachine
import dev.sunnyday.arch.mvi.factory.MviStateMachineInstanceFactory

internal data class ConstantMviStateMachineInstanceFactory<State : Any, Event : Any, SideEffect : Any>(
    val stateMachine: StateMachine<State, Event, SideEffect>,
) : MviStateMachineInstanceFactory<State, Event, SideEffect> {

    override fun MviStateMachineInstanceFactory.FactoryScope<State, Event, SideEffect>.createStateMachine()
            : StateMachine<State, Event, SideEffect> {
        return stateMachine
    }
}