package dev.sunnyday.arch.mvi.kit

import dev.sunnyday.arch.mvi.*
import dev.sunnyday.arch.mvi.factory.instance.MviFeatureInstanceFactory
import dev.sunnyday.arch.mvi.factory.instance.StateMachineInstanceFactory
import dev.sunnyday.arch.mvi.primitive.OnReadyCallback
import dev.sunnyday.arch.mvi.starter.InitialEventsProvider
import dev.sunnyday.arch.mvi.starter.InitialSideEffectsProvider
import dev.sunnyday.arch.mvi.starter.InitialStateProvider
import org.junit.jupiter.api.Test
import kotlin.test.assertSame

interface MviKitInstanceTest {

    @Test
    fun `key is companion`() {
        val instance = object : MviKitInstance {
            override fun <State : Any, Event : Any, SideEffect : Any> createStateMachine(
                initialState: State,
                reducer: Reducer<State, Event, Update<State, SideEffect>>,
                stateTransitionListener: StateTransitionListener<StateTransition<State, Event, SideEffect>>?
            ): StateMachine<State, Event, SideEffect> {
                TODO("Not yet implemented")
            }

            override fun <State : Any, InputEvent : Any, Event : Any, SideEffect : Any> createFeature(
                initialState: State,
                eventHandler: EventHandler<InputEvent, Event>,
                sideEffectHandler: SideEffectHandler<SideEffect, Event>,
                stateMachineFactory: StateMachineInstanceFactory<State, Event, SideEffect>,
                onReadyCallback: OnReadyCallback?
            ): MviFeature<State, InputEvent> {
                TODO("Not yet implemented")
            }

            override fun <State : Any, InputEvent : Any, Event : Any, SideEffect : Any> createFeatureStarter(
                initialStateProvider: InitialStateProvider<State>,
                initialEventsProvider: InitialEventsProvider<State, Event>?,
                initialInputEventsProvider: InitialEventsProvider<State, InputEvent>?,
                initialSideEffectsProvider: InitialSideEffectsProvider<State, SideEffect>?,
                stateMachineInstanceFactory: StateMachineInstanceFactory<State, Event, SideEffect>,
                featureInstanceFactory: MviFeatureInstanceFactory<State, InputEvent, Event, SideEffect>
            ): MviFeatureStarter<State, InputEvent> {
                TODO("Not yet implemented")
            }
        }

        assertSame(MviKitInstance.Key, instance.key)
    }
}
