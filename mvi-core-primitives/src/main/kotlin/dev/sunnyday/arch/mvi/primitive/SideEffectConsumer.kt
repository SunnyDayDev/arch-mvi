package dev.sunnyday.arch.mvi.primitive

fun interface SideEffectConsumer<in SideEffect : Any> {

    fun onSideEffect(sideEffect: SideEffect)
}