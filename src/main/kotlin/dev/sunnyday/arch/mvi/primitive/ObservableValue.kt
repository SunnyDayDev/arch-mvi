package dev.sunnyday.arch.mvi.primitive

interface ObservableValue<out T> : Observable <T> {

    val value: T
}