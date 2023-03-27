package dev.sunnyday.arch.mvi.coroutine

import kotlinx.coroutines.*
import kotlin.coroutines.EmptyCoroutineContext

internal object CoroutineScopes {

    @Suppress("FunctionName")
    fun MviCoroutineScope(parent: CoroutineScope? = null): CoroutineScope {
        val parentJob = parent?.coroutineContext?.job
        val parentContext = parent?.coroutineContext ?: EmptyCoroutineContext
        return CoroutineScope(parentContext + Dispatchers.Default + SupervisorJob(parentJob))
    }

    @Suppress("FunctionName")
    fun ObservableCoroutineScope(parent: CoroutineScope? = null): CoroutineScope {
        val parentJob = parent?.coroutineContext?.job
        val parentContext = parent?.coroutineContext ?: EmptyCoroutineContext
        return CoroutineScope(parentContext + SupervisorJob(parentJob))
    }
}