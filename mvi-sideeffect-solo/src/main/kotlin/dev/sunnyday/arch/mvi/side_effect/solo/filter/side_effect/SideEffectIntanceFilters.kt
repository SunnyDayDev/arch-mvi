package dev.sunnyday.arch.mvi.side_effect.solo.filter.side_effect

import dev.sunnyday.arch.mvi.side_effect.solo.ExecutingSideEffect
import dev.sunnyday.arch.mvi.side_effect.solo.InstanceFilter
import dev.sunnyday.arch.mvi.side_effect.solo.SoloSideEffect

inline fun <reified T : Any> sideEffectsWithType(
    allowSupertype: Boolean = true,
): InstanceFilter<ExecutingSideEffect<*>, ExecutingSideEffect<T>> {
    return SideEffectsWithTypeInstanceFilter.of(allowSupertype)
}

fun <SS : SoloSideEffect<*, *, *>> SS.sideEffectsWithSameType(
    allowSupertype: Boolean = true,
): InstanceFilter<ExecutingSideEffect<*>, ExecutingSideEffect<SS>> {
    return SideEffectsWithTypeInstanceFilter(this::class, allowSupertype)
}