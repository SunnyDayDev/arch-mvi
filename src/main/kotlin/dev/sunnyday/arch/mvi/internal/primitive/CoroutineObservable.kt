package dev.sunnyday.arch.mvi.internal.primitive

import dev.sunnyday.arch.mvi.internal.coroutine.MviCoroutineScope
import dev.sunnyday.arch.mvi.primitive.Cancellable
import dev.sunnyday.arch.mvi.primitive.Observable
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow

internal open class CoroutineObservable<T>(
    val flow: Flow<T>,
    private val coroutineScope: CoroutineScope? = null,
) : Observable<T> {

    override fun observe(observer: (T) -> Unit): Cancellable {
        val checkedCoroutineScope = coroutineScope ?: MviCoroutineScope()
        return observe(observer, checkedCoroutineScope)
    }

    internal open fun observe(observer: (T) -> Unit, coroutineScope: CoroutineScope): Cancellable {
        if (!coroutineScope.isActive) {
            return Cancellable.empty()
        }

        val job = coroutineScope.launch(SupervisorJob()) {
            flow.collect { value ->
                observer.invoke(value)
            }
        }

        return Cancellable(job::cancel)
    }
}