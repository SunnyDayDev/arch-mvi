package dev.sunnyday.arch.mvi.test.common

import dev.sunnyday.arch.mvi.*
import dev.sunnyday.arch.mvi.factory.MviKitFeatureFactory
import dev.sunnyday.arch.mvi.factory.MviKitFeatureStarterFactory
import dev.sunnyday.arch.mvi.factory.MviKitStateMachineFactory
import dev.sunnyday.arch.mvi.factory.instance.MviFeatureInstanceFactory
import dev.sunnyday.arch.mvi.factory.instance.StateMachineInstanceFactory
import dev.sunnyday.arch.mvi.primitive.OnReadyCallback
import dev.sunnyday.arch.mvi.starter.InitialEventsProvider
import dev.sunnyday.arch.mvi.starter.InitialSideEffectsProvider
import dev.sunnyday.arch.mvi.starter.InitialStateProvider
import dev.sunnyday.arch.mvi.test.*


fun MviKitStateMachineFactory.createTestStateMachine(
    mockProvider: MockProvider,
    initialState: State? = null,
    reducer: Reducer<State, Event, Update<State, SideEffect>>? = null,
    stateTransitionListener: StateTransitionListener<StateTransition<State, Event, SideEffect>>? = null,
): StateMachine<State, Event, SideEffect> {
    return createStateMachine(
        initialState = initialState ?: mockProvider.getMock("createStateMachine:initialState"),
        reducer = reducer ?: mockProvider.getMock("createStateMachine:reducer"),
        stateTransitionListener = stateTransitionListener
            ?: mockProvider.getMock("createStateMachine:stateTransitionListener"),
    )
}

fun MviKitFeatureFactory.createTestFeature(
    mockProvider: MockProvider,
    initialState: State? = null,
    eventHandler: EventHandler<InputEvent, Event>? = null,
    sideEffectHandler: SideEffectHandler<SideEffect, Event>? = null,
    stateMachineFactory: StateMachineInstanceFactory<State, Event, SideEffect>? = null,
    onReadyCallback: OnReadyCallback? = null,
): MviFeature<State, InputEvent> {
    return createFeature(
        initialState = initialState ?: mockProvider.getMock("createFeature:initialState"),
        eventHandler = eventHandler ?: mockProvider.getMock("createFeature:eventHandler"),
        sideEffectHandler = sideEffectHandler ?: mockProvider.getMock("createFeature:sideEffectHandler"),
        stateMachineFactory = stateMachineFactory ?: mockProvider.getMock("createFeature:stateMachineFactory"),
        onReadyCallback = onReadyCallback ?: mockProvider.getMock("createFeature:onReadyCallback"),
    )
}

fun MviKitFeatureStarterFactory.createTestFeatureStarter(
    mockProvider: MockProvider,
    initialStateProvider: InitialStateProvider<State>? = null,
    initialEventsProvider: InitialEventsProvider<State, Event>? = null,
    initialInputEventsProvider: InitialEventsProvider<State, InputEvent>? = null,
    initialSideEffectsProvider: InitialSideEffectsProvider<State, SideEffect>? = null,
    stateMachineInstanceFactory: StateMachineInstanceFactory<State, Event, SideEffect>? = null,
    featureInstanceFactory: MviFeatureInstanceFactory<State, InputEvent, Event, SideEffect>? = null,
): MviFeatureStarter<State, InputEvent> {
    return createFeatureStarter(
        initialStateProvider = initialStateProvider
            ?: mockProvider.getMock("createFeatureStarter:initialStateProvider"),
        initialEventsProvider = initialEventsProvider
            ?: mockProvider.getMock("createFeatureStarter:initialEventsProvider"),
        initialInputEventsProvider = initialInputEventsProvider
            ?: mockProvider.getMock("createFeatureStarter:initialInputEventsProvider"),
        initialSideEffectsProvider = initialSideEffectsProvider
            ?: mockProvider.getMock("createFeatureStarter:initialSideEffectsProvider"),
        stateMachineInstanceFactory = stateMachineInstanceFactory
            ?: mockProvider.getMock("createFeatureStarter:stateMachineInstanceFactory"),
        featureInstanceFactory = featureInstanceFactory
            ?: mockProvider.getMock("createFeatureStarter:featureInstanceFactory"),
    )
}