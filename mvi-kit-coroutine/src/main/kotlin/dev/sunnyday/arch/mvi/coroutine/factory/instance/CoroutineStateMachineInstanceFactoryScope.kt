package dev.sunnyday.arch.mvi.coroutine.factory.instance

import dev.sunnyday.arch.mvi.*
import dev.sunnyday.arch.mvi.coroutine.factory.CoroutineMviKitStateMachineFactory
import dev.sunnyday.arch.mvi.factory.MviFactoryCallContext
import dev.sunnyday.arch.mvi.factory.MviKitStateMachineFactory
import dev.sunnyday.arch.mvi.factory.instance.StateMachineInstanceFactory

internal class CoroutineStateMachineInstanceFactoryScope<State : Any, Event : Any, SideEffect : Any>(
    private val initialState: State,
) : StateMachineInstanceFactory.FactoryScope<State, Event, SideEffect> {

    override fun createStateMachine(
        reducer: Reducer<State, Event, Update<State, SideEffect>>,
        stateTransitionListener: StateTransitionListener<StateTransition<State, Event, SideEffect>>?
    ): StateMachine<State, Event, SideEffect> {
        val stateMachineFactory = getStateMachineFactory()

        return stateMachineFactory.createStateMachine(
            initialState = initialState,
            reducer = reducer,
            stateTransitionListener = stateTransitionListener,
        )
    }

    private fun getStateMachineFactory(): MviKitStateMachineFactory {
        return MviFactoryCallContext.getCurrentFactoryContext()?.get(MviKitStateMachineFactory)
            ?: CoroutineMviKitStateMachineFactory()
    }
}