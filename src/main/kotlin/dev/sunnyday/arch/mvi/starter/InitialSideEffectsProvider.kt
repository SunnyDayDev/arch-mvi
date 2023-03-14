package dev.sunnyday.arch.mvi.starter

fun interface InitialSideEffectsProvider<in State, out SideEffect> {

    fun getInitialSideEffects(state: State): List<SideEffect>
}