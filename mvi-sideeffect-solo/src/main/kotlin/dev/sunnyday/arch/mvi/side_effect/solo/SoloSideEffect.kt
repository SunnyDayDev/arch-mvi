package dev.sunnyday.arch.mvi.side_effect.solo

import dev.sunnyday.arch.mvi.primitive.ObservableEvent

interface SoloSideEffect<Dependency : Any, SideEffects: Any, OutputEvent : Any> {

    val executionRule: SoloExecutionRule<SideEffects>
        get() = SoloExecutionRule.independent()

    fun execute(dependency: Dependency): ObservableEvent<OutputEvent>
}