package dev.sunnyday.arch.mvi.factory.instance

import dev.sunnyday.arch.mvi.*
import dev.sunnyday.arch.mvi.event_handler.TransparentEventHandler
import dev.sunnyday.arch.mvi.primitive.OnReadyCallback

fun interface MviFeatureInstanceFactory<State : Any, InputEvent : Any, Event : Any, SideEffect : Any> {

    fun FactoryScope<State, InputEvent, Event, SideEffect>.createFeature(): MviFeature<State, InputEvent>

    interface FactoryScope<State : Any, InputEvent : Any, Event : Any, SideEffect : Any> {

        fun createFeature(
            eventHandler: EventHandler<InputEvent, Event>,
            sideEffectHandler: SideEffectHandler<SideEffect, Event>,
            onReadyCallback: OnReadyCallback? = null,
        ): MviFeature<State, InputEvent>
    }
}

fun <S : Any, E : Any, SE : Any> MviFeatureInstanceFactory.FactoryScope<S, E, E, SE>.createFeature(
    sideEffectHandler: SideEffectHandler<SE, E>,
    onReadyCallback: OnReadyCallback? = null,
): MviFeature<S, E> {
    return createFeature(
        sideEffectHandler = sideEffectHandler,
        onReadyCallback = onReadyCallback,
        eventHandler = TransparentEventHandler(),
    )
}