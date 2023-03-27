package dev.sunnyday.arch.mvi.sideeffect_handler

import dev.sunnyday.arch.mvi.SideEffectHandler
import dev.sunnyday.arch.mvi.primitive.ObservableEvent

class EmptySideEffectHandler : SideEffectHandler<Any, Nothing> {

    override val outputEvents: ObservableEvent<Nothing> = ObservableEvent.empty()

    override fun onSideEffect(sideEffect: Any) = Unit
}