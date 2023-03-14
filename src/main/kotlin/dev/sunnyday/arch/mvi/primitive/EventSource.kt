package dev.sunnyday.arch.mvi.primitive

import kotlinx.coroutines.flow.Flow

interface EventSource<out Event : Any> {

    val outputEvents: Flow<Event>
}