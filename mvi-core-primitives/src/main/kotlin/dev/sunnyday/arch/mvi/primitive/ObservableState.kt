package dev.sunnyday.arch.mvi.primitive

interface ObservableState<out T> : ObservableEvent<T> {

    val value: T
}