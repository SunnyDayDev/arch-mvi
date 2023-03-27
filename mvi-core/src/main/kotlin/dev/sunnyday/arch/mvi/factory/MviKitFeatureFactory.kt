package dev.sunnyday.arch.mvi.factory

import dev.sunnyday.arch.mvi.*
import dev.sunnyday.arch.mvi.sideeffect_handler.EmptySideEffectHandler
import dev.sunnyday.arch.mvi.SideEffectHandler
import dev.sunnyday.arch.mvi.factory.instance.StateMachineInstanceFactory
import dev.sunnyday.arch.mvi.primitive.OnReadyCallback

interface MviKitFeatureFactory : MviFactoryCallContext.Element {

    override val key: MviFactoryCallContext.Key<*>
        get() = Key

    fun <State : Any, InputEvent : Any, Event : Any, SideEffect : Any> createFeature(
        initialState: State,
        eventHandler: EventHandler<InputEvent, Event>,
        sideEffectHandler: SideEffectHandler<SideEffect, Event> = EmptySideEffectHandler(),
        stateMachineFactory: StateMachineInstanceFactory<State, Event, SideEffect>,
        onReadyCallback: OnReadyCallback? = null,
    ): MviFeature<State, InputEvent>

    companion object Key : MviFactoryCallContext.Key<MviKitFeatureFactory>
}