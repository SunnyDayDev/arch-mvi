package dev.sunnyday.arch.mvi

import dev.sunnyday.arch.mvi.primitive.EventConsumer
import dev.sunnyday.arch.mvi.primitive.SideEffectSource
import dev.sunnyday.arch.mvi.primitive.StateSource

interface StateMachine<out State: Any, in Event: Any, out SideEffect: Any> :
    EventConsumer<Event>,
    StateSource<State>,
    SideEffectSource<SideEffect>