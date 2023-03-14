package dev.sunnyday.arch.mvi.primitive

import kotlinx.coroutines.flow.Flow

interface SideEffectSource<out SideEffect : Any> {

    val sideEffects: Flow<SideEffect>
}