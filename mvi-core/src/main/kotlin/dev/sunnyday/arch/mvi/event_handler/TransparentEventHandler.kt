package dev.sunnyday.arch.mvi.event_handler

import dev.sunnyday.arch.mvi.EventHandler
import dev.sunnyday.arch.mvi.primitive.ObservableEvent
import dev.sunnyday.arch.mvi.primitive.SharedObservableEvent

class TransparentEventHandler<Event : Any> : EventHandler<Event, Event> {

    private val sharedEvents = SharedObservableEvent<Event>()

    override val outputEvents: ObservableEvent<Event>
        get() = ObservableEvent(sharedEvents::observe)

    override fun onEvent(event: Event) = sharedEvents.onEvent(event)
}