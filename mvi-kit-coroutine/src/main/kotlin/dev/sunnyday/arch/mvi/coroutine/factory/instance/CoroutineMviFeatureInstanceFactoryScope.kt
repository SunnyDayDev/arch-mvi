package dev.sunnyday.arch.mvi.coroutine.factory.instance

import dev.sunnyday.arch.mvi.*
import dev.sunnyday.arch.mvi.coroutine.factory.CoroutineMviKitFeatureFactory
import dev.sunnyday.arch.mvi.factory.*
import dev.sunnyday.arch.mvi.factory.instance.StateMachineInstanceFactory.FactoryScope
import dev.sunnyday.arch.mvi.factory.instance.MviFeatureInstanceFactory
import dev.sunnyday.arch.mvi.factory.instance.StateMachineInstanceFactory
import dev.sunnyday.arch.mvi.primitive.OnReadyCallback
import dev.sunnyday.arch.mvi.starter.InitialEventsProvider
import dev.sunnyday.arch.mvi.starter.InitialSideEffectsProvider

internal class CoroutineMviFeatureInstanceFactoryScope<State : Any, InputEvent : Any, Event : Any, SideEffect : Any>(
    private val initialState: State,
    private val stateMachineInstanceFactory: StateMachineInstanceFactory<State, Event, SideEffect>,
    private val initialInputEventsProvider: InitialEventsProvider<State, InputEvent>? = null,
    private val initialEventsProvider: InitialEventsProvider<State, Event>? = null,
    private val initialSideEffectsProvider: InitialSideEffectsProvider<State, SideEffect>? = null,
) : MviFeatureInstanceFactory.FactoryScope<State, InputEvent, Event, SideEffect> {

    override fun createFeature(
        eventHandler: EventHandler<InputEvent, Event>,
        sideEffectHandler: SideEffectHandler<SideEffect, Event>,
        onReadyCallback: OnReadyCallback?,
    ): MviFeature<State, InputEvent> {
        val featureFactory = getFeatureFactory()

        val stateMachineProvider = ObservingStateMachineInstanceFactory(stateMachineInstanceFactory)
        val starterOnReadyCallback = getOnReadyCallback(
            stateMachineProvider,
            eventHandler,
            sideEffectHandler,
            onReadyCallback,
        )

        return featureFactory.createFeature(
            initialState = initialState,
            eventHandler = eventHandler,
            sideEffectHandler = sideEffectHandler,
            stateMachineFactory = stateMachineProvider,
            onReadyCallback = starterOnReadyCallback,
        )
    }

    private fun getFeatureFactory(): MviKitFeatureFactory {
        return MviFactoryCallContext.getCurrentFactoryContext()?.get(MviKitFeatureFactory)
            ?: getDefaultFeatureFactory()
    }

    private fun getDefaultFeatureFactory(): MviKitFeatureFactory {
        return CoroutineMviKitFeatureFactory()
    }

    private fun getOnReadyCallback(
        stateMachineProvider: ObservingStateMachineInstanceFactory<State, Event, SideEffect>,
        eventHandler: EventHandler<InputEvent, Event>,
        sideEffectHandler: SideEffectHandler<SideEffect, Event>,
        onReadyCallback: OnReadyCallback?,
    ): OnReadyCallback {
        return OnReadyCallback {
            initialEventsProvider?.getInitialEvents(initialState)
                ?.forEach(stateMachineProvider.stateMachine::onEvent)
            initialInputEventsProvider?.getInitialEvents(initialState)
                ?.forEach(eventHandler::onEvent)
            initialSideEffectsProvider?.getInitialSideEffects(initialState)
                ?.forEach(sideEffectHandler::onSideEffect)

            onReadyCallback?.onReady()
        }
    }

    private class ObservingStateMachineInstanceFactory<State : Any, Event : Any, SideEffect : Any>(
        private val stateMachineInstanceFactory: StateMachineInstanceFactory<State, Event, SideEffect>,
    ) : StateMachineInstanceFactory<State, Event, SideEffect> {

        lateinit var stateMachine: StateMachine<State, Event, SideEffect>

        override fun FactoryScope<State, Event, SideEffect>.createStateMachine(): StateMachine<State, Event, SideEffect> {
            return stateMachineInstanceFactory.run { createStateMachine() }
                .also { stateMachine = it }
        }
    }
}