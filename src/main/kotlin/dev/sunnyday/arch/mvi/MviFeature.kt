package dev.sunnyday.arch.mvi

import dev.sunnyday.arch.mvi.primitive.Cancellable
import dev.sunnyday.arch.mvi.primitive.EventConsumer
import dev.sunnyday.arch.mvi.primitive.StateSource

interface MviFeature<out State : Any, in Event : Any> :
    EventConsumer<Event>,
    StateSource<State>,
    Cancellable