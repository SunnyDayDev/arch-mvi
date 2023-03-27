package dev.sunnyday.arch.mvi

fun interface MviFeatureStarter<State: Any, in Event: Any> {

    fun start(): MviFeature<State, Event>
}