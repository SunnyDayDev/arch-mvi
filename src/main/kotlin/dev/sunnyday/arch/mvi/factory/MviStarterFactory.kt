package dev.sunnyday.arch.mvi.factory

import kotlinx.coroutines.CoroutineScope
import dev.sunnyday.arch.mvi.*
import dev.sunnyday.arch.mvi.internal.event_handler.TransparentEventHandler
import dev.sunnyday.arch.mvi.internal.sideeffect_handler.EmptySideEffectHandler
import dev.sunnyday.arch.mvi.SideEffectHandler
import dev.sunnyday.arch.mvi.starter.InitialEventsProvider
import dev.sunnyday.arch.mvi.starter.InitialSideEffectsProvider
import dev.sunnyday.arch.mvi.starter.MviProcessorStarter

object MviStarterFactory {

    fun <State : Any, Event : Any, SideEffect : Any> createStarter(
        coroutineScope: CoroutineScope,
        reducer: Reducer<State, Event, Update<State, SideEffect>>,
        initialEventsProvider: InitialEventsProvider<State, Event>? = null,
        initialSideEffectsProvider: InitialSideEffectsProvider<State, SideEffect>? = null,
        sideEffectHandler: SideEffectHandler<SideEffect, Event> = EmptySideEffectHandler(),
        stateTransitionListener: StateTransitionListener<StateTransition<State, Event, SideEffect>>? = null,
    ): MviProcessorStarter<State, Event> {
        return createStarter(
            coroutineScope = coroutineScope,
            reducer = reducer,
            eventHandler = TransparentEventHandler(),
            initialEventsProvider = initialEventsProvider,
            initialSideEffectsProvider = initialSideEffectsProvider,
            sideEffectHandler = sideEffectHandler,
            stateTransitionListener = stateTransitionListener,
        )
    }

    fun <State : Any, InputEvent : Any, Event : Any, SideEffect : Any> createStarter(
        coroutineScope: CoroutineScope,
        reducer: Reducer<State, Event, Update<State, SideEffect>>,
        eventHandler: EventHandler<InputEvent, Event>,
        initialEventsProvider: InitialEventsProvider<State, Event>? = null,
        initialInputEventsProvider: InitialEventsProvider<State, InputEvent>? = null,
        initialSideEffectsProvider: InitialSideEffectsProvider<State, SideEffect>? = null,
        sideEffectHandler: SideEffectHandler<SideEffect, Event> = EmptySideEffectHandler(),
        stateTransitionListener: StateTransitionListener<StateTransition<State, Event, SideEffect>>? = null,
    ): MviProcessorStarter<State, InputEvent> {
        return MviProcessorStarter { initialState ->
            val stateMachine = MviStateMachineFactory.createStateMachine(
                coroutineScope = coroutineScope,
                initialState = initialState,
                reducer = reducer,
                stateTransitionListener = stateTransitionListener,
            )

            val processor = MviProcessorFactory.createProcessor(
                coroutineScope = coroutineScope,
                stateMachine = stateMachine,
                eventHandler = eventHandler,
                sideEffectHandler = sideEffectHandler,
                onStartHandler = {
                    initialEventsProvider?.getInitialEvents(initialState)
                        ?.forEach(stateMachine::onEvent)

                    initialInputEventsProvider?.getInitialEvents(initialState)
                        ?.forEach(eventHandler::onEvent)

                    initialSideEffectsProvider?.getInitialSideEffects(initialState)
                        ?.forEach(sideEffectHandler::onSideEffect)
                },
            )

            processor
        }
    }
}