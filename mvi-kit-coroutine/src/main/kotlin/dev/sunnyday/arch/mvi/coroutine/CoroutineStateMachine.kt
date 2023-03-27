package dev.sunnyday.arch.mvi.coroutine

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.actor
import kotlinx.coroutines.flow.*
import dev.sunnyday.arch.mvi.*
import dev.sunnyday.arch.mvi.Reducer
import dev.sunnyday.arch.mvi.coroutine.ktx.toObservable
import dev.sunnyday.arch.mvi.primitive.ObservableEvent
import dev.sunnyday.arch.mvi.primitive.ObservableState
import kotlinx.coroutines.channels.Channel

@OptIn(FlowPreview::class)
internal class CoroutineStateMachine<out State : Any, in Event : Any, out SideEffect : Any>(
    initialState: State,
    private val coroutineScope: CoroutineScope,
    private val reducer: Reducer<State, Event, Update<State, SideEffect>>,
    private val stateTransitionListener: StateTransitionListener<StateTransition<State, Event, SideEffect>>? = null,
) : StateMachine<State, Event, SideEffect> {

    private val stateFlow = MutableStateFlow(initialState)
    private val sideEffectsFlow = MutableSharedFlow<List<SideEffect>>()

    override val state: ObservableState<State> = stateFlow.toObservable(coroutineScope)

    override val sideEffects: ObservableEvent<SideEffect> = sideEffectsFlow
        .flatMapConcat { it.asFlow() }
        .toObservable(coroutineScope)

    @Suppress("RemoveExplicitTypeArguments") // compiler bug
    @OptIn(ObsoleteCoroutinesApi::class)
    private val eventActor = coroutineScope.actor<Event>(capacity = ACTOR_EVENT_CAPACITY) {
        proceedEvents(coroutineScope, channel)
    }

    private suspend fun proceedEvents(coroutineScope: CoroutineScope, channel: Channel<Event>) {
        for (event in channel) {
            val currentState = stateFlow.value
            val (state, sideEffects) = reducer.reduce(currentState, event)

            if (state != null) {
                stateFlow.value = state
            }

            if (sideEffects.isNotEmpty()) {
                coroutineScope.launch { sendSideEffects(sideEffects) }
            }

            if (stateTransitionListener != null) {
                val stateTransition = StateTransition(
                    previousState = currentState,
                    newState = state ?: currentState,
                    triggerEvent = event,
                    sideEffects = sideEffects,
                )

                coroutineScope.launch { stateTransitionListener.onStateTransition(stateTransition) }
            }
        }
    }

    private suspend fun sendSideEffects(sideEffects: List<SideEffect>) {
        sideEffectsFlow.subscriptionCount.first { it > 0 }
        sideEffectsFlow.emit(sideEffects)
    }

    override fun onEvent(event: Event) {
        var isCompleted = trySendEvent(event)

        // TODO: https://github.com/SunnyDayDev/arch-mvi/issues/13
        //  implement better suspending onEvent
        while (!isCompleted) {
            Thread.sleep(1)
            if (Thread.currentThread().isInterrupted) return
            isCompleted = trySendEvent(event)
        }
    }

    private fun trySendEvent(event: Event): Boolean {
        val result = eventActor.trySend(event)
        return result.isSuccess || result.isClosed
    }

    override fun cancel() {
        eventActor.close()
    }

    private companion object {

        const val ACTOR_EVENT_CAPACITY = 16
    }
}
