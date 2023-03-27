package dev.sunnyday.arch.mvi

import dev.sunnyday.arch.mvi.primitive.EventSource
import dev.sunnyday.arch.mvi.primitive.SideEffectConsumer

interface SideEffectHandler<in SideEffect : Any, out Event : Any> :
    SideEffectConsumer<SideEffect>,
    EventSource<Event>