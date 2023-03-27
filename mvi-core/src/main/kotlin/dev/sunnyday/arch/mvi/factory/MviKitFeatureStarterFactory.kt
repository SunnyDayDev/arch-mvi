package dev.sunnyday.arch.mvi.factory

import dev.sunnyday.arch.mvi.starter.InitialEventsProvider
import dev.sunnyday.arch.mvi.starter.InitialSideEffectsProvider
import dev.sunnyday.arch.mvi.starter.InitialStateProvider
import dev.sunnyday.arch.mvi.MviFeatureStarter
import dev.sunnyday.arch.mvi.factory.instance.MviFeatureInstanceFactory
import dev.sunnyday.arch.mvi.factory.instance.StateMachineInstanceFactory

interface MviKitFeatureStarterFactory : MviFactoryCallContext.Element {

    override val key: MviFactoryCallContext.Key<*>
        get() = Key

    fun <State : Any, InputEvent : Any, Event : Any, SideEffect : Any> createFeatureStarter(
        initialStateProvider: InitialStateProvider<State>,
        initialEventsProvider: InitialEventsProvider<State, Event>? = null,
        initialInputEventsProvider: InitialEventsProvider<State, InputEvent>? = null,
        initialSideEffectsProvider: InitialSideEffectsProvider<State, SideEffect>? = null,
        stateMachineInstanceFactory: StateMachineInstanceFactory<State, Event, SideEffect>,
        featureInstanceFactory: MviFeatureInstanceFactory<State, InputEvent, Event, SideEffect>,
    ): MviFeatureStarter<State, InputEvent>

    companion object Key : MviFactoryCallContext.Key<MviKitFeatureStarterFactory>
}