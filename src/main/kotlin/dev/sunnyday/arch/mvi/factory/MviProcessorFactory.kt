package dev.sunnyday.arch.mvi.factory

import dev.sunnyday.arch.mvi.*
import dev.sunnyday.arch.mvi.internal.sideeffect_handler.EmptySideEffectHandler
import dev.sunnyday.arch.mvi.SideEffectHandler

interface MviProcessorFactory {

    fun <State : Any, Event : Any, SideEffect : Any> createProcessor(
        initialState: State,
        reducer: Reducer<State, Event, Update<State, SideEffect>>,
        sideEffectHandler: SideEffectHandler<SideEffect, Event> = EmptySideEffectHandler(),
        stateTransitionListener: StateTransitionListener<StateTransition<State, Event, SideEffect>>? = null,
    ): MviProcessor<State, Event>

    fun <State : Any, InputEvent : Any, Event : Any, SideEffect : Any> createProcessor(
        initialState: State,
        reducer: Reducer<State, Event, Update<State, SideEffect>>,
        eventHandler: EventHandler<InputEvent, Event>,
        sideEffectHandler: SideEffectHandler<SideEffect, Event> = EmptySideEffectHandler(),
        stateTransitionListener: StateTransitionListener<StateTransition<State, Event, SideEffect>>? = null,
    ): MviProcessor<State, InputEvent>
}