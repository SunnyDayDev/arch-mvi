package dev.sunnyday.arch.mvi.internal

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.actor
import kotlinx.coroutines.flow.*
import dev.sunnyday.arch.mvi.*
import dev.sunnyday.arch.mvi.Reducer

@OptIn(FlowPreview::class)
internal class StateMachineImpl<out State : Any, in Event : Any, out SideEffect : Any>(
    initialState: State,
    coroutineScope: CoroutineScope,
    private val reducer: Reducer<State, Event, Update<State, SideEffect>>,
    private val stateTransitionListener: StateTransitionListener<StateTransition<State, Event, SideEffect>>? = null,
) : StateMachine<State, Event, SideEffect> {

    override val state: StateFlow<State> get() = _state.asStateFlow()
    private val _state = MutableStateFlow(initialState)

    override val sideEffects: Flow<SideEffect>
        get() = _sideEffects.flatMapMerge { sideEffects -> sideEffects.asFlow() }
    private val _sideEffects = MutableSharedFlow<List<SideEffect>>()

    @Suppress("RemoveExplicitTypeArguments") // compiler bug
    @OptIn(ObsoleteCoroutinesApi::class)
    private val eventActor = coroutineScope.actor<Event>(capacity = ACTOR_EVENT_CAPACITY) {
        for (event in channel) {
            val currentState = _state.value
            val (state, sideEffects) = reducer.reduce(currentState, event)

            if (state != null) {
                _state.value = state
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
        _sideEffects.subscriptionCount.first { it > 0 }
        _sideEffects.emit(sideEffects)
    }

    override fun onEvent(event: Event) {
        var result = eventActor.trySend(event)

        // TODO: https://github.com/SunnyDayDev/arch-mvi/issues/13
        //  implement better suspending onEvent
        while (result.isFailure && !result.isClosed) {
            Thread.sleep(1)
            if (Thread.currentThread().isInterrupted) return
            result = eventActor.trySend(event)
        }
    }

    private companion object {

        const val ACTOR_EVENT_CAPACITY = 16
    }
}
