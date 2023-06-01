package dev.sunnyday.arch.mvi.side_effect.solo

import kotlinx.coroutines.flow.Flow

interface SoloSideEffect<Dependency : Any, SideEffects: Any, OutputEvent : Any> {

    val executionRule: SoloExecutionRule<SideEffects>
        get() = SoloExecutionRule.independent()

    fun execute(dependency: Dependency): Flow<OutputEvent>
}