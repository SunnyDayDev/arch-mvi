package dev.sunnyday.arch.mvi.starter

fun interface InitialStateProvider<out State> {

    fun provideInitialState(): State
}