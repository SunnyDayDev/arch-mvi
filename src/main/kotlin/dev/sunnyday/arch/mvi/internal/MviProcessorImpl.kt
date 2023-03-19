package dev.sunnyday.arch.mvi.internal

import kotlinx.coroutines.flow.*
import dev.sunnyday.arch.mvi.EventHandler
import dev.sunnyday.arch.mvi.MviProcessor
import dev.sunnyday.arch.mvi.SideEffectHandler
import dev.sunnyday.arch.mvi.StateMachine
import dev.sunnyday.arch.mvi.coroutines.toFlow
import dev.sunnyday.arch.mvi.coroutines.takeUntil
import dev.sunnyday.arch.mvi.primitive.ObservableValue
import kotlinx.coroutines.*

internal class MviProcessorImpl<State : Any, InputEvent : Any, Event : Any, SideEffect : Any>(
    coroutineScope: CoroutineScope,
    private val eventHandler: EventHandler<InputEvent, Event>,
    private val sideEffectHandler: SideEffectHandler<SideEffect, Event>,
    private val stateMachine: StateMachine<State, Event, SideEffect>,
    onStartHandler: (suspend () -> Unit)? = null,
) : MviProcessor<State, InputEvent> {

    private val isCancelled = MutableStateFlow(false)

    private val cancelSignal: Flow<Any>
        get() = isCancelled.filter { it }

    init {
        val onSubscription = getOneShotOnStartHandler(onStartHandler)

        coroutineScope.launch(SupervisorJob()) {
            launch { collectEvents(this, onSubscription) }
            launch { collectSideEffects() }
        }
    }

    override val state: ObservableValue<State>
        get() = stateMachine.state

    private fun getOneShotOnStartHandler(onStartHandler: (suspend () -> Unit)?): (suspend () -> Unit)? {
        onStartHandler ?: return null
        var onStarHandlerInstance = onStartHandler

        return {
            onStarHandlerInstance?.invoke()
            onStarHandlerInstance = null
        }
    }

    private suspend fun collectEvents(coroutineScope: CoroutineScope, onSubscription: (suspend () -> Unit)?) {
        val mergedEventsSource = merge(
            eventHandler.outputEvents.toFlow(),
            sideEffectHandler.outputEvents.toFlow(),
        )

        val eventsSource = if (onSubscription == null) {
            mergedEventsSource
        } else {
            mergedEventsSource
                .shareIn(coroutineScope, SharingStarted.Lazily)
                .onSubscription { onSubscription.invoke() }
        }

        eventsSource
            .takeUntil(cancelSignal)
            .collect(stateMachine::onEvent)
    }

    private suspend fun collectSideEffects() {
        stateMachine.sideEffects.toFlow()
            .takeUntil(cancelSignal)
            .collect(sideEffectHandler::onSideEffect)
    }

    override fun cancel() {
        stateMachine.cancel()
        isCancelled.tryEmit(true)
    }

    override fun onEvent(event: InputEvent) {
        if (isCancelled.value) return
        eventHandler.onEvent(event)
    }
}
