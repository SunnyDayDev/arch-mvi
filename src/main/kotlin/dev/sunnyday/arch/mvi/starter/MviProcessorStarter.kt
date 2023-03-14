package dev.sunnyday.arch.mvi.starter

import dev.sunnyday.arch.mvi.MviProcessor

fun interface MviProcessorStarter<State: Any, in Event: Any> {

    fun start(initialState: State): MviProcessor<State, Event>
}