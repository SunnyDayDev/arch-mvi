package dev.sunnyday.arch.mvi.coroutine.factory

import dev.sunnyday.arch.mvi.factory.MviFactoryCallContext
import dev.sunnyday.arch.mvi.MviFeatureStarter
import dev.sunnyday.arch.mvi.factory.*
import dev.sunnyday.arch.mvi.factory.instance.MviFeatureInstanceFactory
import dev.sunnyday.arch.mvi.factory.instance.StateMachineInstanceFactory
import dev.sunnyday.arch.mvi.starter.InitialEventsProvider
import dev.sunnyday.arch.mvi.starter.InitialSideEffectsProvider
import dev.sunnyday.arch.mvi.starter.InitialStateProvider

class CoroutineMviKitFeatureStarterFactory : MviKitFeatureStarterFactory {

    override fun <State : Any, InputEvent : Any, Event : Any, SideEffect : Any> createFeatureStarter(
        initialStateProvider: InitialStateProvider<State>,
        initialEventsProvider: InitialEventsProvider<State, Event>?,
        initialInputEventsProvider: InitialEventsProvider<State, InputEvent>?,
        initialSideEffectsProvider: InitialSideEffectsProvider<State, SideEffect>?,
        stateMachineInstanceFactory: StateMachineInstanceFactory<State, Event, SideEffect>,
        featureInstanceFactory: MviFeatureInstanceFactory<State, InputEvent, Event, SideEffect>,
    ): MviFeatureStarter<State, InputEvent> {
        val factoryCallContext = getFactoryCallContext()

        return CoroutineMviFeatureStarter(
            factoryCallContext = factoryCallContext,
            initialStateProvider = initialStateProvider,
            initialEventsProvider = initialEventsProvider,
            initialInputEventsProvider = initialInputEventsProvider,
            initialSideEffectsProvider = initialSideEffectsProvider,
            stateMachineInstanceFactory = stateMachineInstanceFactory,
            featureInstanceFactory = featureInstanceFactory,
        )
    }

    private fun getFactoryCallContext(): MviFactoryCallContext {
        return MviFactoryCallContext.getCurrentFactoryContext() ?: MviFactoryCallContext.create().apply {
            this[MviKitStateMachineFactory] = CoroutineMviKitStateMachineFactory()
            this[MviKitFeatureFactory] = CoroutineMviKitFeatureFactory()
            this[MviKitFeatureStarterFactory] = this@CoroutineMviKitFeatureStarterFactory
        }
    }
}