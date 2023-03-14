package dev.sunnyday.arch.mvi

fun interface StateTransitionListener<in ST: StateTransition<*, *, *>> {

    fun onStateTransition(transition: ST)
}