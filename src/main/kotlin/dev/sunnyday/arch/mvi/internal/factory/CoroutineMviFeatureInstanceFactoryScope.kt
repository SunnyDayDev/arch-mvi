package dev.sunnyday.arch.mvi.internal.factory

import dev.sunnyday.arch.mvi.EventHandler
import dev.sunnyday.arch.mvi.MviFeature
import dev.sunnyday.arch.mvi.SideEffectHandler
import dev.sunnyday.arch.mvi.StateMachine
import dev.sunnyday.arch.mvi.factory.MviFeatureFactory
import dev.sunnyday.arch.mvi.factory.MviFeatureInstanceFactory
import dev.sunnyday.arch.mvi.primitive.OnReadyCallback
import dev.sunnyday.arch.mvi.starter.InitialEventsProvider
import dev.sunnyday.arch.mvi.starter.InitialSideEffectsProvider
import kotlinx.coroutines.CoroutineScope

internal class CoroutineMviFeatureInstanceFactoryScope<State : Any, InputEvent : Any, Event : Any, SideEffect : Any>(
    private val featureFactory: MviFeatureFactory,
    private val featureCoroutineScope: CoroutineScope,
    private val initialState: State,
    private val initialInputEventsProvider: InitialEventsProvider<State, InputEvent>?,
    private val initialEventsProvider: InitialEventsProvider<State, Event>?,
    private val initialSideEffectsProvider: InitialSideEffectsProvider<State, SideEffect>?,
    private val stateMachine: StateMachine<State, Event, SideEffect>,
) : MviFeatureInstanceFactory.FactoryScope<State, InputEvent, Event, SideEffect> {

    override fun createFeature(
        eventHandler: EventHandler<InputEvent, Event>,
        sideEffectHandler: SideEffectHandler<SideEffect, Event>,
        onReadyCallback: OnReadyCallback?,
    ): MviFeature<State, InputEvent> {
        val starterOnReadyCallback = getOnReadyCallback(eventHandler, sideEffectHandler, onReadyCallback)

        return if (featureFactory is CoroutineMviFeatureFactory) {
            featureFactory.createFeature(
                coroutineScope = featureCoroutineScope,
                eventHandler = eventHandler,
                sideEffectHandler = sideEffectHandler,
                stateMachine = stateMachine,
                onReadyCallback = starterOnReadyCallback,
            )
        } else {
            featureFactory.createFeature(
                initialState = initialState,
                eventHandler = eventHandler,
                sideEffectHandler = sideEffectHandler,
                stateMachineFactory = ConstantMviStateMachineInstanceFactory(stateMachine),
                onReadyCallback = starterOnReadyCallback,
            )
        }
    }

    private fun getOnReadyCallback(
        eventHandler: EventHandler<InputEvent, Event>,
        sideEffectHandler: SideEffectHandler<SideEffect, Event>,
        onReadyCallback: OnReadyCallback?,
    ): OnReadyCallback? {
        return if (
            initialEventsProvider == null &&
            initialInputEventsProvider == null &&
            initialSideEffectsProvider == null
        ) {
            onReadyCallback
        } else {
            OnReadyCallback {
                initialEventsProvider?.getInitialEvents(initialState)
                    ?.forEach(stateMachine::onEvent)
                initialInputEventsProvider?.getInitialEvents(initialState)
                    ?.forEach(eventHandler::onEvent)
                initialSideEffectsProvider?.getInitialSideEffects(initialState)
                    ?.forEach(sideEffectHandler::onSideEffect)

                onReadyCallback?.onReady()
            }
        }
    }
}