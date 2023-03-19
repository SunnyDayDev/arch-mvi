package dev.sunnyday.arch.mvi.internal.sideeffect_handler

import dev.sunnyday.arch.mvi.SideEffectHandler
import dev.sunnyday.arch.mvi.primitive.Observable

class EmptySideEffectHandler : SideEffectHandler<Any, Nothing> {

    override val outputEvents: Observable<Nothing> = Observable.empty()

    override fun onSideEffect(sideEffect: Any) = Unit
}