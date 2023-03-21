package dev.sunnyday.arch.mvi.internal.factory

import dev.sunnyday.arch.mvi.factory.*
import dev.sunnyday.arch.mvi.internal.coroutine.MviCoroutineScope
import dev.sunnyday.arch.mvi.starter.InitialEventsProvider
import dev.sunnyday.arch.mvi.starter.InitialSideEffectsProvider
import dev.sunnyday.arch.mvi.starter.InitialStateProvider
import dev.sunnyday.arch.mvi.starter.MviFeatureStarter

internal class CoroutineMviFeatureStarterFactory : MviFeatureStarterFactory {

    override fun <State : Any, InputEvent : Any, Event : Any, SideEffect : Any> createFeatureStarter(
        initialStateProvider: InitialStateProvider<State>,
        initialEventsProvider: InitialEventsProvider<State, Event>?,
        initialInputEventsProvider: InitialEventsProvider<State, InputEvent>?,
        initialSideEffectsProvider: InitialSideEffectsProvider<State, SideEffect>?,
        stateMachineInstanceFactory: MviStateMachineInstanceFactory<State, Event, SideEffect>,
        featureInstanceFactory: MviFeatureInstanceFactory<State, InputEvent, Event, SideEffect>,
    ): MviFeatureStarter<State, InputEvent> {
        return MviFeatureStarter {
            val initialState = initialStateProvider.provideInitialState()
            val featureCoroutineScope = MviCoroutineScope()

            val stateMachineFactoryScope = CoroutineStateMachineInstanceFactoryScope<State, Event, SideEffect>(
                stateMachineFactory = MviKit.stateMachineFactory,
                coroutineScope = featureCoroutineScope,
                initialState = initialState,
            )

            val stateMachine = stateMachineInstanceFactory.run { stateMachineFactoryScope.createStateMachine() }

            val featureFactoryScope = CoroutineMviFeatureInstanceFactoryScope(
                featureFactory = MviKit.featureFactory,
                featureCoroutineScope = featureCoroutineScope,
                initialState = initialState,
                initialEventsProvider = initialEventsProvider,
                initialInputEventsProvider = initialInputEventsProvider,
                initialSideEffectsProvider = initialSideEffectsProvider,
                stateMachine = stateMachine,
            )

            featureInstanceFactory.run { featureFactoryScope.createFeature() }
        }
    }
}