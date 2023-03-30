package dev.sunnyday.arch.mvi.side_effect.particle.test

import dev.sunnyday.arch.mvi.side_effect.particle.ExecutingSideEffect

data class TestFilterExecutingSideEffect<T : Any>(
    override val sideEffect: T,
    override val id: ExecutingSideEffect.Id = ExecutingSideEffect.Id.Custom(sideEffect),
) : ExecutingSideEffect<T> {

    override fun cancel() = Unit
}