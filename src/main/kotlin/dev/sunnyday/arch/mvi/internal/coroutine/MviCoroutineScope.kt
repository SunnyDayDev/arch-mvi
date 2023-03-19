package dev.sunnyday.arch.mvi.internal.coroutine

import kotlinx.coroutines.*
import kotlin.coroutines.EmptyCoroutineContext

@Suppress("FunctionName")
internal fun MviCoroutineScope(parent: CoroutineScope? = null): CoroutineScope {
    val parentJob = parent?.coroutineContext?.job
    val parentContext = parent?.coroutineContext ?: EmptyCoroutineContext
    return CoroutineScope(parentContext + Dispatchers.Default + SupervisorJob(parentJob) + MviCoroutineMarker)
}

internal val CoroutineScope.isMviCoroutineScope: Boolean
    get() = coroutineContext.isMviCoroutine