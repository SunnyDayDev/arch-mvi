package dev.sunnyday.arch.mvi.internal.factory

import kotlinx.coroutines.CoroutineScope
import dev.sunnyday.arch.mvi.*
import dev.sunnyday.arch.mvi.internal.event_handler.TransparentEventHandler
import dev.sunnyday.arch.mvi.internal.MviFeatureImpl
import dev.sunnyday.arch.mvi.SideEffectHandler
import dev.sunnyday.arch.mvi.factory.MviFeatureFactory
import dev.sunnyday.arch.mvi.factory.MviKit
import dev.sunnyday.arch.mvi.factory.MviStateMachineInstanceFactory
import dev.sunnyday.arch.mvi.internal.coroutine.MviCoroutineScope
import dev.sunnyday.arch.mvi.primitive.OnReadyCallback

internal class CoroutineMviFeatureFactory : MviFeatureFactory {

    override fun <State : Any, InputEvent : Any, Event : Any, SideEffect : Any> createFeature(
        initialState: State,
        eventHandler: EventHandler<InputEvent, Event>,
        sideEffectHandler: SideEffectHandler<SideEffect, Event>,
        stateMachineFactory: MviStateMachineInstanceFactory<State, Event, SideEffect>,
        onReadyCallback: OnReadyCallback?,
    ): MviFeature<State, InputEvent> {
        val featureCoroutineScope = MviCoroutineScope()

        val stateMachineFactoryScope: MviStateMachineInstanceFactory.FactoryScope<State, Event, SideEffect> =
            CoroutineStateMachineInstanceFactoryScope(
                stateMachineFactory = MviKit.stateMachineFactory,
                coroutineScope = featureCoroutineScope,
                initialState = initialState,
            )

        val stateMachine = stateMachineFactory.run { stateMachineFactoryScope.createStateMachine() }

        return createFeature(
            coroutineScope = featureCoroutineScope,
            eventHandler = eventHandler,
            sideEffectHandler = sideEffectHandler,
            stateMachine = stateMachine,
            onReadyCallback = onReadyCallback,
        )
    }

    fun <State : Any, InputEvent : Any, Event : Any, SideEffect : Any> createFeature(
        coroutineScope: CoroutineScope,
        eventHandler: EventHandler<InputEvent, Event>,
        sideEffectHandler: SideEffectHandler<SideEffect, Event>,
        stateMachine: StateMachine<State, Event, SideEffect>,
        onReadyCallback: OnReadyCallback?,
    ): MviFeature<State, InputEvent> {

        // It's safe to cast, because instance of TransparentEventHandler creates only inside factory
        @Suppress("UNCHECKED_CAST")
        (eventHandler as? TransparentEventHandler<Event>)?.receiver = stateMachine

        return MviFeatureImpl(
            coroutineScope = coroutineScope,
            eventHandler = eventHandler,
            sideEffectHandler = sideEffectHandler,
            stateMachine = stateMachine,
            onReadyCallback = onReadyCallback,
        )
    }
}