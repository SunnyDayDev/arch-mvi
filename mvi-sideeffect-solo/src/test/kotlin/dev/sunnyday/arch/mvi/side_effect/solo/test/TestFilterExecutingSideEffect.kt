package dev.sunnyday.arch.mvi.side_effect.solo.test

import dev.sunnyday.arch.mvi.side_effect.solo.ExecutingSideEffect

data class TestFilterExecutingSideEffect<T : Any>(
    override val sideEffect: T,
    override val id: ExecutingSideEffect.Id = ExecutingSideEffect.Id.Custom(sideEffect),
) : ExecutingSideEffect<T> {

    override val executionState: ExecutingSideEffect.ExecutionState
        get() = TODO("Not yet implemented")

    override fun cancel() = Unit
}