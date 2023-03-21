package dev.sunnyday.arch.mvi.starter

import dev.sunnyday.arch.mvi.MviFeature

fun interface MviFeatureStarter<State: Any, in Event: Any> {

    fun start(): MviFeature<State, Event>
}