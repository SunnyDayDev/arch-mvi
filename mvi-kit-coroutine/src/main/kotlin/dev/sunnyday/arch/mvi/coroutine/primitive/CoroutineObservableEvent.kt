package dev.sunnyday.arch.mvi.coroutine.primitive

import dev.sunnyday.arch.mvi.coroutine.CoroutineScopes
import dev.sunnyday.arch.mvi.primitive.Cancellable
import dev.sunnyday.arch.mvi.primitive.EventConsumer
import dev.sunnyday.arch.mvi.primitive.ObservableEvent
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow

internal open class CoroutineObservableEvent<T>(
    val flow: Flow<T>,
    coroutineScope: CoroutineScope? = null,
) : ObservableEvent<T> {

    protected val coroutineScope = coroutineScope ?: CoroutineScopes.ObservableCoroutineScope()

    override fun observe(observer: EventConsumer<T>): Cancellable {
        if (!coroutineScope.isActive) {
            return Cancellable.empty()
        }

        val job = coroutineScope.launch(SupervisorJob()) {
            flow.collect { value ->
                observer.onEvent(value)
            }
        }

        return Cancellable(job::cancel)
    }
}