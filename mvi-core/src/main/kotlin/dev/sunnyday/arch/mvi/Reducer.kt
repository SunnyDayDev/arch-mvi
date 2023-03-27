package dev.sunnyday.arch.mvi

fun interface Reducer<in State: Any, in Event: Any, out Output> {

    fun reduce(state: State, event: Event): Output
}