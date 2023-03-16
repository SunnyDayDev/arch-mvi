package dev.sunnyday.arch.mvi.factory

import kotlinx.coroutines.CoroutineScope
import dev.sunnyday.arch.mvi.*
import dev.sunnyday.arch.mvi.internal.event_handler.TransparentEventHandler
import dev.sunnyday.arch.mvi.internal.MviProcessorImpl
import dev.sunnyday.arch.mvi.internal.sideeffect_handler.EmptySideEffectHandler
import dev.sunnyday.arch.mvi.SideEffectHandler

object MviProcessorFactory {

    fun <State : Any, Event : Any, SideEffect : Any> createProcessor(
        coroutineScope: CoroutineScope,
        initialState: State,
        reducer: Reducer<State, Event, Update<State, SideEffect>>,
        sideEffectHandler: SideEffectHandler<SideEffect, Event> = EmptySideEffectHandler(),
        stateTransitionListener: StateTransitionListener<StateTransition<State, Event, SideEffect>>? = null,
    ): MviProcessor<State, Event> {
        return createProcessor(
            coroutineScope = coroutineScope,
            initialState = initialState,
            reducer = reducer,
            sideEffectHandler = sideEffectHandler,
            eventHandler = TransparentEventHandler(),
            stateTransitionListener = stateTransitionListener,
        )
    }

    fun <State : Any, InputEvent : Any, Event : Any, SideEffect : Any> createProcessor(
        coroutineScope: CoroutineScope,
        initialState: State,
        reducer: Reducer<State, Event, Update<State, SideEffect>>,
        eventHandler: EventHandler<InputEvent, Event>,
        sideEffectHandler: SideEffectHandler<SideEffect, Event> = EmptySideEffectHandler(),
        stateTransitionListener: StateTransitionListener<StateTransition<State, Event, SideEffect>>? = null,
    ): MviProcessor<State, InputEvent> {
        val stateMachine = MviStateMachineFactory.createStateMachine(
            coroutineScope = coroutineScope,
            initialState = initialState,
            reducer = reducer,
            stateTransitionListener = stateTransitionListener,
        )

        return createProcessor(
            coroutineScope = coroutineScope,
            stateMachine = stateMachine,
            eventHandler = eventHandler,
            sideEffectHandler = sideEffectHandler,
        )
    }

    internal fun <State : Any, InputEvent : Any, Event : Any, SideEffect : Any> createProcessor(
        coroutineScope: CoroutineScope,
        stateMachine: StateMachine<State, Event, SideEffect>,
        eventHandler: EventHandler<InputEvent, Event>,
        sideEffectHandler: SideEffectHandler<SideEffect, Event> = EmptySideEffectHandler(),
        onStartHandler: (suspend () -> Unit)? = null,
    ): MviProcessor<State, InputEvent> {
        // It's safe to cast, because instance of TransparentEventHandler creates only inside factory
        @Suppress("UNCHECKED_CAST")
        (eventHandler as? TransparentEventHandler<Event>)?.receiver = stateMachine

        return MviProcessorImpl(
            coroutineScope = coroutineScope,
            eventHandler = eventHandler,
            sideEffectHandler = sideEffectHandler,
            stateMachine = stateMachine,
            onStartHandler = onStartHandler,
        )
    }
}