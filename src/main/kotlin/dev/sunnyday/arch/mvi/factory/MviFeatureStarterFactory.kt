package dev.sunnyday.arch.mvi.factory

import dev.sunnyday.arch.mvi.starter.InitialEventsProvider
import dev.sunnyday.arch.mvi.starter.InitialSideEffectsProvider
import dev.sunnyday.arch.mvi.starter.InitialStateProvider
import dev.sunnyday.arch.mvi.starter.MviFeatureStarter

interface MviFeatureStarterFactory {

    fun <State : Any, InputEvent : Any, Event : Any, SideEffect : Any> createFeatureStarter(
        initialStateProvider: InitialStateProvider<State>,
        initialEventsProvider: InitialEventsProvider<State, Event>? = null,
        initialInputEventsProvider: InitialEventsProvider<State, InputEvent>? = null,
        initialSideEffectsProvider: InitialSideEffectsProvider<State, SideEffect>? = null,
        stateMachineInstanceFactory: MviStateMachineInstanceFactory<State, Event, SideEffect>,
        featureInstanceFactory: MviFeatureInstanceFactory<State, InputEvent, Event, SideEffect>,
    ): MviFeatureStarter<State, InputEvent>
}