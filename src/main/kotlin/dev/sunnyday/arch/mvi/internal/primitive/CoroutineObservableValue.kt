package dev.sunnyday.arch.mvi.internal.primitive

import dev.sunnyday.arch.mvi.primitive.Cancellable
import dev.sunnyday.arch.mvi.primitive.ObservableValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive

internal class CoroutineObservableValue<T>(
    val stateFlow: StateFlow<T>,
    coroutineScope: CoroutineScope? = null,
) : CoroutineObservable<T>(stateFlow, coroutineScope), ObservableValue<T> {

    override val value: T get() = stateFlow.value

    override fun observe(observer: (T) -> Unit, coroutineScope: CoroutineScope): Cancellable {
        return if (!coroutineScope.isActive) {
            observer.invoke(value)
            Cancellable.empty()
        } else {
            super.observe(observer, coroutineScope)
        }
    }
}