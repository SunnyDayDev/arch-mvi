package dev.sunnyday.arch.mvi.side_effect.particle

import dev.sunnyday.arch.mvi.primitive.Cancellable

interface ExecutingSideEffect<out SideEffect : Any> : Cancellable {

    val id: Id

    val sideEffect: SideEffect

    sealed interface Id {

        object Undefined : Id

        @Suppress("CanSealedSubClassBeObject")
        class Unique : Id

        data class Custom(val id: Any) : Id
    }
}