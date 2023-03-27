package dev.sunnyday.arch.mvi

import dev.sunnyday.arch.mvi.primitive.EventConsumer
import dev.sunnyday.arch.mvi.primitive.EventSource

interface EventHandler<in InputEvent : Any, out Event : Any> :
    EventConsumer<InputEvent>,
    EventSource<Event>