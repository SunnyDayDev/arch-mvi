package dev.sunnyday.arch.mvi.kit

import dev.sunnyday.arch.mvi.*
import dev.sunnyday.arch.mvi.primitive.OnReadyCallback
import dev.sunnyday.arch.mvi.starter.InitialEventsProvider
import dev.sunnyday.arch.mvi.starter.InitialSideEffectsProvider
import dev.sunnyday.arch.mvi.starter.InitialStateProvider
import dev.sunnyday.arch.mvi.factory.*
import dev.sunnyday.arch.mvi.factory.instance.MviFeatureInstanceFactory
import dev.sunnyday.arch.mvi.factory.instance.StateMachineInstanceFactory

abstract class ContextEnabledMviKitInstance : MviKitInstance {

    protected abstract val stateMachineFactory: MviKitStateMachineFactory
    protected abstract val featureFactory: MviKitFeatureFactory
    protected abstract val starterFactory: MviKitFeatureStarterFactory

    override fun <State : Any, InputEvent : Any, Event : Any, SideEffect : Any> createFeature(
        initialState: State,
        eventHandler: EventHandler<InputEvent, Event>,
        sideEffectHandler: SideEffectHandler<SideEffect, Event>,
        stateMachineFactory: StateMachineInstanceFactory<State, Event, SideEffect>,
        onReadyCallback: OnReadyCallback?,
    ): MviFeature<State, InputEvent> {
        return runWithFactoryContext(::createCallFactoryContext) {
            featureFactory.createFeature(
                initialState = initialState,
                eventHandler = eventHandler,
                sideEffectHandler = sideEffectHandler,
                stateMachineFactory = stateMachineFactory,
                onReadyCallback = onReadyCallback,
            )
        }
    }

    override fun <State : Any, InputEvent : Any, Event : Any, SideEffect : Any> createFeatureStarter(
        initialStateProvider: InitialStateProvider<State>,
        initialEventsProvider: InitialEventsProvider<State, Event>?,
        initialInputEventsProvider: InitialEventsProvider<State, InputEvent>?,
        initialSideEffectsProvider: InitialSideEffectsProvider<State, SideEffect>?,
        stateMachineInstanceFactory: StateMachineInstanceFactory<State, Event, SideEffect>,
        featureInstanceFactory: MviFeatureInstanceFactory<State, InputEvent, Event, SideEffect>
    ): MviFeatureStarter<State, InputEvent> {
        return runWithFactoryContext(::createCallFactoryContext) {
            starterFactory.createFeatureStarter(
                initialStateProvider = initialStateProvider,
                initialEventsProvider = initialEventsProvider,
                initialInputEventsProvider = initialInputEventsProvider,
                initialSideEffectsProvider = initialSideEffectsProvider,
                stateMachineInstanceFactory = stateMachineInstanceFactory,
                featureInstanceFactory = featureInstanceFactory,
            )
        }
    }

    override fun <State : Any, Event : Any, SideEffect : Any> createStateMachine(
        initialState: State,
        reducer: Reducer<State, Event, Update<State, SideEffect>>,
        stateTransitionListener: StateTransitionListener<StateTransition<State, Event, SideEffect>>?
    ): StateMachine<State, Event, SideEffect> {
        return runWithFactoryContext(::createCallFactoryContext) {
            stateMachineFactory.createStateMachine(
                initialState = initialState,
                reducer = reducer,
                stateTransitionListener = stateTransitionListener,
            )
        }
    }

    private inline fun <T> runWithFactoryContext(
        contextProvider: () -> MviFactoryCallContext,
        crossinline action: () -> T,
    ): T {
        val currentFactoryCallContext = MviFactoryCallContext.getCurrentFactoryContext()
        val currentContextInstance = currentFactoryCallContext?.get(MviKitInstance)
        if (currentContextInstance === this) {
            return action.invoke()
        }

        val factoryContext = contextProvider.invoke()

        return factoryContext.runWithFactoryContext {
            action.invoke()
        }
    }

    protected open fun createCallFactoryContext(): MviFactoryCallContext {
        return createCallFactoryContextFrom(this)
    }

    protected fun createCallFactoryContextFrom(instance: ContextEnabledMviKitInstance): MviFactoryCallContext {
        return MviFactoryCallContext.create().apply {
            this[MviKitInstance] = instance
            this[MviKitStateMachineFactory] = stateMachineFactory
            this[MviKitFeatureFactory] = featureFactory
            this[MviKitFeatureStarterFactory] = starterFactory
        }
    }
    // expected: dev.sunnyday.arch.mvi.factory.MviFactoryCallContext@e829a6a<dev.sunnyday.arch.mvi.kit.ContextEnabledMviKitInstanceTest$TestContextEnabledMviKitInstance@66a8dbe1, MviKitStateMachineFactory(#24), MviKitFeatureFactory(#25), MviKitFeatureStarterFactory(#26)>
    // but was: dev.sunnyday.arch.mvi.factory.MviFactoryCallContext@335047db<dev.sunnyday.arch.mvi.kit.ContextEnabledMviKitInstanceTest$TestContextEnabledMviKitInstance@66a8dbe1, MviKitStateMachineFactory(#24), MviKitFeatureFactory(#25), MviKitFeatureStarterFactory(#26)>
}