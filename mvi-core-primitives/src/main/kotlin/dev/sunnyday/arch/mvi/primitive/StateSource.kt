package dev.sunnyday.arch.mvi.primitive

interface StateSource<out State> {

    val state: ObservableState<State>
}