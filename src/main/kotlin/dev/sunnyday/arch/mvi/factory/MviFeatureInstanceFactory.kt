package dev.sunnyday.arch.mvi.factory

import dev.sunnyday.arch.mvi.*
import dev.sunnyday.arch.mvi.internal.event_handler.TransparentEventHandler
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

fun <State : Any, Event : Any, SideEffect : Any> MviFeatureInstanceFactory.FactoryScope<State, Event, Event, SideEffect>.createFeature(
    sideEffectHandler: SideEffectHandler<SideEffect, Event>,
    onReadyCallback: OnReadyCallback? = null,
): MviFeature<State, Event> = createFeature(
    sideEffectHandler = sideEffectHandler,
    onReadyCallback = onReadyCallback,
    eventHandler = TransparentEventHandler(),
)