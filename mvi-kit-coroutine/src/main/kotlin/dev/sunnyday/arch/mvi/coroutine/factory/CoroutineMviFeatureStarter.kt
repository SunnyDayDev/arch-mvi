package dev.sunnyday.arch.mvi.coroutine.factory

import dev.sunnyday.arch.mvi.MviFeature
import dev.sunnyday.arch.mvi.MviFeatureStarter
import dev.sunnyday.arch.mvi.coroutine.factory.instance.CoroutineMviFeatureInstanceFactoryScope
import dev.sunnyday.arch.mvi.factory.MviFactoryCallContext
import dev.sunnyday.arch.mvi.factory.instance.MviFeatureInstanceFactory
import dev.sunnyday.arch.mvi.factory.instance.StateMachineInstanceFactory
import dev.sunnyday.arch.mvi.starter.InitialEventsProvider
import dev.sunnyday.arch.mvi.starter.InitialSideEffectsProvider
import dev.sunnyday.arch.mvi.starter.InitialStateProvider

internal class CoroutineMviFeatureStarter<State : Any, InputEvent : Any, Event : Any, SideEffect : Any>(
    private val factoryCallContext: MviFactoryCallContext,
    private val initialStateProvider: InitialStateProvider<State>,
    private val initialEventsProvider: InitialEventsProvider<State, Event>?,
    private val initialInputEventsProvider: InitialEventsProvider<State, InputEvent>?,
    private val initialSideEffectsProvider: InitialSideEffectsProvider<State, SideEffect>?,
    private val stateMachineInstanceFactory: StateMachineInstanceFactory<State, Event, SideEffect>,
    private val featureInstanceFactory: MviFeatureInstanceFactory<State, InputEvent, Event, SideEffect>,
) : MviFeatureStarter<State, InputEvent> {
    override fun start(): MviFeature<State, InputEvent> {
        return factoryCallContext.runWithFactoryContext {
            startFeature(
                initialStateProvider = initialStateProvider,
                initialEventsProvider = initialEventsProvider,
                initialInputEventsProvider = initialInputEventsProvider,
                initialSideEffectsProvider = initialSideEffectsProvider,
                stateMachineInstanceFactory = stateMachineInstanceFactory,
                featureInstanceFactory = featureInstanceFactory,
            )
        }
    }


    private fun <State : Any, InputEvent : Any, Event : Any, SideEffect : Any> startFeature(
        initialStateProvider: InitialStateProvider<State>,
        initialEventsProvider: InitialEventsProvider<State, Event>?,
        initialInputEventsProvider: InitialEventsProvider<State, InputEvent>?,
        initialSideEffectsProvider: InitialSideEffectsProvider<State, SideEffect>?,
        stateMachineInstanceFactory: StateMachineInstanceFactory<State, Event, SideEffect>,
        featureInstanceFactory: MviFeatureInstanceFactory<State, InputEvent, Event, SideEffect>,
    ): MviFeature<State, InputEvent> {
        val initialState = initialStateProvider.provideInitialState()

        val featureFactoryScope: MviFeatureInstanceFactory.FactoryScope<State, InputEvent, Event, SideEffect> =
            CoroutineMviFeatureInstanceFactoryScope(
                initialState = initialState,
                initialEventsProvider = initialEventsProvider,
                initialInputEventsProvider = initialInputEventsProvider,
                initialSideEffectsProvider = initialSideEffectsProvider,
                stateMachineInstanceFactory = stateMachineInstanceFactory,
            )

        return featureInstanceFactory.run { featureFactoryScope.createFeature() }
    }
}