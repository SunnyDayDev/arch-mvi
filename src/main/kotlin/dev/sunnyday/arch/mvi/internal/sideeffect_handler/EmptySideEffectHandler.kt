package dev.sunnyday.arch.mvi.internal.sideeffect_handler

import dev.sunnyday.arch.mvi.SideEffectHandler
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
class EmptySideEffectHandler : SideEffectHandler<Any, Nothing> {

    override val outputEvents: Flow<Nothing> = emptyFlow()

    override fun onSideEffect(sideEffect: Any) = Unit
}