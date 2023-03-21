package dev.sunnyday.arch.mvi.factory

import dev.sunnyday.arch.mvi.*
import dev.sunnyday.arch.mvi.internal.sideeffect_handler.EmptySideEffectHandler
import dev.sunnyday.arch.mvi.SideEffectHandler
import dev.sunnyday.arch.mvi.primitive.OnReadyCallback

interface MviFeatureFactory {

    fun <State : Any, InputEvent : Any, Event : Any, SideEffect : Any> createFeature(
        initialState: State,
        eventHandler: EventHandler<InputEvent, Event>,
        sideEffectHandler: SideEffectHandler<SideEffect, Event> = EmptySideEffectHandler(),
        stateMachineFactory: MviStateMachineInstanceFactory<State, Event, SideEffect>,
        onReadyCallback: OnReadyCallback? = null,
    ): MviFeature<State, InputEvent>
}