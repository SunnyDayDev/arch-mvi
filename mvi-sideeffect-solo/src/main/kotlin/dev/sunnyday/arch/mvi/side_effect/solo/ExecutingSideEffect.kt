package dev.sunnyday.arch.mvi.side_effect.solo

import dev.sunnyday.arch.mvi.primitive.Cancellable

interface ExecutingSideEffect<out SideEffect : Any> : Cancellable {

    val id: Id

    val sideEffect: SideEffect

    val executionState: ExecutionState

    sealed interface Id {

        object Undefined : Id

        @Suppress("CanSealedSubClassBeObject")
        class Unique : Id

        data class Custom(val id: Any) : Id {

            override fun toString(): String = id.toString()
        }
    }

    enum class ExecutionState {
        ENQUEUED,
        EXECUTING,
        COMPLETED,
    }
}