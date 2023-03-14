package dev.sunnyday.arch.mvi.internal.event_handler

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import dev.sunnyday.arch.mvi.primitive.EventConsumer
import dev.sunnyday.arch.mvi.EventHandler

internal class TransparentEventHandler<Event : Any> : EventHandler<Event, Event> {

    var receiver: EventConsumer<Event>? = null

    override val outputEvents: Flow<Event> = emptyFlow()

    override fun onEvent(event: Event) {
        receiver?.onEvent(event)
    }
}