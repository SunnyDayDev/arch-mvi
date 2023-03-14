package dev.sunnyday.arch.mvi

import dev.sunnyday.arch.mvi.primitive.EventConsumer
import dev.sunnyday.arch.mvi.primitive.StateSource

interface MviProcessor<out State: Any, in Event: Any> :
    EventConsumer<Event>,
    StateSource<State>