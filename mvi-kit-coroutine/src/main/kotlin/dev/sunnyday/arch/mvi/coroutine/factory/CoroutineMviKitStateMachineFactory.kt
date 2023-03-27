package dev.sunnyday.arch.mvi.coroutine.factory

import dev.sunnyday.arch.mvi.*
import dev.sunnyday.arch.mvi.coroutine.CoroutineScopes
import dev.sunnyday.arch.mvi.coroutine.CoroutineStateMachine
import dev.sunnyday.arch.mvi.factory.MviKitStateMachineFactory
import kotlinx.coroutines.CoroutineScope

class CoroutineMviKitStateMachineFactory : MviKitStateMachineFactory {

    override fun <State : Any, Event : Any, SideEffect : Any> createStateMachine(
        initialState: State,
        reducer: Reducer<State, Event, Update<State, SideEffect>>,
        stateTransitionListener: StateTransitionListener<StateTransition<State, Event, SideEffect>>?
    ): StateMachine<State, Event, SideEffect> {
        val coroutineScope = getCoroutineScope()

        return CoroutineStateMachine(
            initialState = initialState,
            reducer = reducer,
            stateTransitionListener = stateTransitionListener,
            coroutineScope = coroutineScope,
        )
    }

    private fun getCoroutineScope(): CoroutineScope {
        return CoroutineScopes.MviCoroutineScope(
            parent = CoroutineFactoryContext.getParentCoroutineScope(),
        )
    }
}