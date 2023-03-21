package dev.sunnyday.arch.mvi.internal

import kotlinx.coroutines.flow.*
import dev.sunnyday.arch.mvi.EventHandler
import dev.sunnyday.arch.mvi.MviFeature
import dev.sunnyday.arch.mvi.SideEffectHandler
import dev.sunnyday.arch.mvi.StateMachine
import dev.sunnyday.arch.mvi.coroutines.toFlow
import dev.sunnyday.arch.mvi.coroutines.takeUntil
import dev.sunnyday.arch.mvi.primitive.ObservableValue
import dev.sunnyday.arch.mvi.primitive.OnReadyCallback
import kotlinx.coroutines.*
import java.util.concurrent.atomic.AtomicReference

internal class MviFeatureImpl<State : Any, InputEvent : Any, Event : Any, SideEffect : Any>(
    coroutineScope: CoroutineScope,
    private val eventHandler: EventHandler<InputEvent, Event>,
    private val sideEffectHandler: SideEffectHandler<SideEffect, Event>,
    private val stateMachine: StateMachine<State, Event, SideEffect>,
    onReadyCallback: OnReadyCallback?,
) : MviFeature<State, InputEvent> {

    private val isCancelled = MutableStateFlow(false)

    private val cancelSignal: Flow<Any>
        get() = isCancelled.filter { it }

    init {
        val onReadyCallbackWrapper = AtomicReference(onReadyCallback)

        coroutineScope.launch(SupervisorJob()) {
            launch { collectEvents(this, onReadyCallbackWrapper) }
            launch { collectSideEffects() }
        }
    }

    override val state: ObservableValue<State>
        get() = stateMachine.state

    private suspend fun collectEvents(
        coroutineScope: CoroutineScope,
        onSubscription: AtomicReference<OnReadyCallback?>,
    ) {
        val mergedEventsSource = merge(
            eventHandler.outputEvents.toFlow(),
            sideEffectHandler.outputEvents.toFlow(),
        )

        val eventsSource = if (onSubscription.get() == null) {
            mergedEventsSource
        } else {
            mergedEventsSource
                .shareIn(coroutineScope, SharingStarted.Lazily)
                .onSubscription { onSubscription.getAndSet(null)?.onReady() }
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
