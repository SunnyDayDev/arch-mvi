package dev.sunnyday.arch.mvi.internal

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import dev.sunnyday.arch.mvi.EventHandler
import dev.sunnyday.arch.mvi.MviProcessor
import dev.sunnyday.arch.mvi.SideEffectHandler
import dev.sunnyday.arch.mvi.StateMachine

internal class MviProcessorImpl<State : Any, InputEvent : Any, Event : Any, SideEffect : Any>(
    coroutineScope: CoroutineScope,
    private val eventHandler: EventHandler<InputEvent, Event>,
    private val sideEffectHandler: SideEffectHandler<SideEffect, Event>,
    private val stateMachine: StateMachine<State, Event, SideEffect>,
    onStartHandler: (() -> Unit)? = null,
) : MviProcessor<State, InputEvent> {

    init {
        var onStarHandlerInstance = onStartHandler

        coroutineScope.launch {
            val mergedEventsSource = merge(eventHandler.outputEvents, sideEffectHandler.outputEvents)

            val eventsSource = if (onStarHandlerInstance == null) {
                mergedEventsSource
            } else {
                mergedEventsSource
                    .shareIn(coroutineScope, SharingStarted.Lazily)
                    .onSubscription {
                        onStarHandlerInstance?.invoke()
                        onStarHandlerInstance = null
                    }
            }

            eventsSource.collect(stateMachine::onEvent)
        }

        coroutineScope.launch {
            stateMachine.sideEffects
                .collect(sideEffectHandler::onSideEffect)
        }
    }

    override val state: StateFlow<State>
        get() = stateMachine.state

    override fun onEvent(event: InputEvent) = eventHandler.onEvent(event)
}
