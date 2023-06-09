package dev.sunnyday.arch.mvi.primitive

interface SideEffectSource<out SideEffect : Any> {

    val sideEffects: ObservableEvent<SideEffect>
}