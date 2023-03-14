package dev.sunnyday.arch.mvi.reducer

import dev.sunnyday.arch.mvi.Reducer
import dev.sunnyday.arch.mvi.Update

abstract class StateReducer<State : Any, in Event : Any> : Reducer<State, Event, Update<State, Nothing>> {

    override fun reduce(state: State, event: Event): Update<State, Nothing> {
        return Update.state(reduceState(state, event))
    }

    abstract fun reduceState(state: State, event: Event): State
}