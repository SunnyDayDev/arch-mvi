package dev.sunnyday.arch.mvi.side_effect.particle

import dev.sunnyday.arch.mvi.primitive.ObservableEvent

interface ParticleSideEffectHandler<Dependency : Any, SideEffects: Any, OutputEvent : Any> {

    val executionRule: ParticleExecutionRule<out SideEffects>
        get() = ParticleExecutionRule.independent()

    fun execute(dependency: Dependency): ObservableEvent<OutputEvent>
}