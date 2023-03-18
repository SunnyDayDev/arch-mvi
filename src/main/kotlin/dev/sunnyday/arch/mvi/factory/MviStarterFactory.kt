package dev.sunnyday.arch.mvi.factory

import dev.sunnyday.arch.mvi.*
import dev.sunnyday.arch.mvi.internal.sideeffect_handler.EmptySideEffectHandler
import dev.sunnyday.arch.mvi.SideEffectHandler
import dev.sunnyday.arch.mvi.starter.InitialEventsProvider
import dev.sunnyday.arch.mvi.starter.InitialSideEffectsProvider
import dev.sunnyday.arch.mvi.starter.MviProcessorStarter

interface MviStarterFactory {

    fun <State : Any, Event : Any, SideEffect : Any> createStarter(
        reducer: Reducer<State, Event, Update<State, SideEffect>>,
        initialEventsProvider: InitialEventsProvider<State, Event>? = null,
        initialSideEffectsProvider: InitialSideEffectsProvider<State, SideEffect>? = null,
        sideEffectHandler: SideEffectHandler<SideEffect, Event> = EmptySideEffectHandler(),
        stateTransitionListener: StateTransitionListener<StateTransition<State, Event, SideEffect>>? = null,
    ): MviProcessorStarter<State, Event>

    fun <State : Any, InputEvent : Any, Event : Any, SideEffect : Any> createStarter(
        reducer: Reducer<State, Event, Update<State, SideEffect>>,
        eventHandler: EventHandler<InputEvent, Event>,
        initialEventsProvider: InitialEventsProvider<State, Event>? = null,
        initialInputEventsProvider: InitialEventsProvider<State, InputEvent>? = null,
        initialSideEffectsProvider: InitialSideEffectsProvider<State, SideEffect>? = null,
        sideEffectHandler: SideEffectHandler<SideEffect, Event> = EmptySideEffectHandler(),
        stateTransitionListener: StateTransitionListener<StateTransition<State, Event, SideEffect>>? = null,
    ): MviProcessorStarter<State, InputEvent>
}