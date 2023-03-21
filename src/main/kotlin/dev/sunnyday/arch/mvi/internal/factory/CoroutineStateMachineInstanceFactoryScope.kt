package dev.sunnyday.arch.mvi.internal.factory

import dev.sunnyday.arch.mvi.*
import dev.sunnyday.arch.mvi.factory.MviStateMachineFactory
import dev.sunnyday.arch.mvi.factory.MviStateMachineInstanceFactory
import kotlinx.coroutines.CoroutineScope

internal class CoroutineStateMachineInstanceFactoryScope<State : Any, Event : Any, SideEffect : Any>(
    private val stateMachineFactory: MviStateMachineFactory,
    private val coroutineScope: CoroutineScope,
    private val initialState: State,
): MviStateMachineInstanceFactory.FactoryScope<State, Event, SideEffect> {

        override fun createStateMachine(
            reducer: Reducer<State, Event, Update<State, SideEffect>>,
            stateTransitionListener: StateTransitionListener<StateTransition<State, Event, SideEffect>>?
        ): StateMachine<State, Event, SideEffect> {
            return if (stateMachineFactory is CoroutineStateMachineFactory) {
                stateMachineFactory.createStateMachine(
                    coroutineScope = coroutineScope,
                    initialState = initialState,
                    reducer = reducer,
                    stateTransitionListener = stateTransitionListener,
                )
            } else {
                stateMachineFactory.createStateMachine(
                    initialState = initialState,
                    reducer = reducer,
                    stateTransitionListener = stateTransitionListener,
                )
            }
        }
    }