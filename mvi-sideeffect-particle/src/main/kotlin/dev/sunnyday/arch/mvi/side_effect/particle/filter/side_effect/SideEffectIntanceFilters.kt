package dev.sunnyday.arch.mvi.side_effect.particle.filter.side_effect


import dev.sunnyday.arch.mvi.side_effect.particle.ExecutingSideEffect
import dev.sunnyday.arch.mvi.side_effect.particle.InstanceFilter

inline fun <reified T : Any> sideEffectsWithType(): InstanceFilter<ExecutingSideEffect<*>, ExecutingSideEffect<T>> {
    return SideEffectsWithTypeInstanceFilter.of()
}