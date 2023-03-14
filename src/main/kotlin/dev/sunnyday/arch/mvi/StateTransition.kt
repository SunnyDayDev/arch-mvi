package dev.sunnyday.arch.mvi

data class StateTransition<out State: Any, out Event: Any, out SideEffect: Any>(
    val previousState: State,
    val newState: State,
    val triggerEvent: Event,
    val sideEffects: List<SideEffect>,
)