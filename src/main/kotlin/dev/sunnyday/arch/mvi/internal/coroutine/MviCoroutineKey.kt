package dev.sunnyday.arch.mvi.internal.coroutine

import kotlin.coroutines.CoroutineContext


internal object MviCoroutineMarker : CoroutineContext.Element {

    override val key: CoroutineContext.Key<*>
        get() = Key

    object Key : CoroutineContext.Key<MviCoroutineMarker>
}

internal val CoroutineContext.isMviCoroutine: Boolean
    get() = this[MviCoroutineMarker.Key] != null
