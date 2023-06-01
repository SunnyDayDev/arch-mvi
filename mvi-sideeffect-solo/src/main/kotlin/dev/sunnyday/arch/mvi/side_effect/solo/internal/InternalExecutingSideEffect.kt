package dev.sunnyday.arch.mvi.side_effect.solo.internal

import dev.sunnyday.arch.mvi.side_effect.solo.ExecutingSideEffect
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

internal class InternalExecutingSideEffect<SideEffect : Any>(
    override val id: ExecutingSideEffect.Id,
    override val sideEffect: SideEffect,
    private val coroutineScope: CoroutineScope,
) : ExecutingSideEffect<SideEffect> {

    override var executionState: ExecutingSideEffect.ExecutionState = ExecutingSideEffect.ExecutionState.ENQUEUED

    val isActiveFlow: StateFlow<Boolean>
        get() = _isActiveFlow
    private val _isActiveFlow: MutableStateFlow<Boolean> = MutableStateFlow(true)

    override fun cancel() {
        coroutineScope.launch {
            cancelInternal()
        }
    }

    suspend fun cancelInternal() {
        _isActiveFlow.emit(false)
    }

    override fun toString(): String {
        return "ExecutingSideEffect($id)"
    }
}