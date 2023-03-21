package dev.sunnyday.arch.mvi.factory

import dev.sunnyday.arch.mvi.*
import dev.sunnyday.arch.mvi.internal.factory.CoroutineMviFeatureFactory
import dev.sunnyday.arch.mvi.internal.factory.CoroutineMviFeatureStarterFactory
import dev.sunnyday.arch.mvi.internal.factory.CoroutineStateMachineFactory
import dev.sunnyday.arch.mvi.primitive.OnReadyCallback
import dev.sunnyday.arch.mvi.starter.InitialEventsProvider
import dev.sunnyday.arch.mvi.starter.InitialSideEffectsProvider
import dev.sunnyday.arch.mvi.starter.InitialStateProvider
import dev.sunnyday.arch.mvi.starter.MviFeatureStarter

object MviKit :
    MviStateMachineFactory,
    MviFeatureFactory,
    MviFeatureStarterFactory {

    internal var stateMachineFactory: MviStateMachineFactory = CoroutineStateMachineFactory()

    internal var featureFactory: MviFeatureFactory = CoroutineMviFeatureFactory()

    internal var starterFactory: MviFeatureStarterFactory = CoroutineMviFeatureStarterFactory()

    override fun <State : Any, InputEvent : Any, Event : Any, SideEffect : Any> createFeature(
        initialState: State,
        eventHandler: EventHandler<InputEvent, Event>,
        sideEffectHandler: SideEffectHandler<SideEffect, Event>,
        stateMachineFactory: MviStateMachineInstanceFactory<State, Event, SideEffect>,
        onReadyCallback: OnReadyCallback?,
    ): MviFeature<State, InputEvent> {
        return featureFactory.createFeature(
            initialState = initialState,
            eventHandler = eventHandler,
            sideEffectHandler = sideEffectHandler,
            stateMachineFactory = stateMachineFactory,
            onReadyCallback = onReadyCallback,
        )
    }

    override fun <State : Any, InputEvent : Any, Event : Any, SideEffect : Any> createFeatureStarter(
        initialStateProvider: InitialStateProvider<State>,
        initialEventsProvider: InitialEventsProvider<State, Event>?,
        initialInputEventsProvider: InitialEventsProvider<State, InputEvent>?,
        initialSideEffectsProvider: InitialSideEffectsProvider<State, SideEffect>?,
        stateMachineInstanceFactory: MviStateMachineInstanceFactory<State, Event, SideEffect>,
        featureInstanceFactory: MviFeatureInstanceFactory<State, InputEvent, Event, SideEffect>
    ): MviFeatureStarter<State, InputEvent> {
        return starterFactory.createFeatureStarter(
            initialStateProvider = initialStateProvider,
            initialEventsProvider = initialEventsProvider,
            initialInputEventsProvider = initialInputEventsProvider,
            initialSideEffectsProvider = initialSideEffectsProvider,
            stateMachineInstanceFactory = stateMachineInstanceFactory,
            featureInstanceFactory = featureInstanceFactory,
        )
    }

    override fun <State : Any, Event : Any, SideEffect : Any> createStateMachine(
        initialState: State,
        reducer: Reducer<State, Event, Update<State, SideEffect>>,
        stateTransitionListener: StateTransitionListener<StateTransition<State, Event, SideEffect>>?
    ): StateMachine<State, Event, SideEffect> {
        return stateMachineFactory.createStateMachine(
            initialState = initialState,
            reducer = reducer,
            stateTransitionListener = stateTransitionListener,
        )
    }
}