package dev.sunnyday.arch.mvi.internal.event_handler

import dev.sunnyday.arch.mvi.primitive.EventConsumer
import dev.sunnyday.arch.mvi.EventHandler
import dev.sunnyday.arch.mvi.primitive.Observable

internal class TransparentEventHandler<Event : Any> : EventHandler<Event, Event> {

    var receiver: EventConsumer<Event>? = null

    override val outputEvents: Observable<Event> = Observable.empty()

    override fun onEvent(event: Event) {
        receiver?.onEvent(event)
    }
}