package dev.sunnyday.arch.mvi.coroutine.primitive

import dev.sunnyday.arch.mvi.primitive.Cancellable
import dev.sunnyday.arch.mvi.primitive.EventConsumer
import dev.sunnyday.arch.mvi.primitive.ObservableState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive

internal class CoroutineObservableState<T>(
    val stateFlow: StateFlow<T>,
    coroutineScope: CoroutineScope? = null,
) : CoroutineObservableEvent<T>(stateFlow, coroutineScope), ObservableState<T> {

    override val value: T get() = stateFlow.value

    override fun observe(observer: EventConsumer<T>): Cancellable {
        return if (!coroutineScope.isActive) {
            observer.onEvent(value)
            Cancellable.empty()
        } else {
            super.observe(observer)
        }
    }
}