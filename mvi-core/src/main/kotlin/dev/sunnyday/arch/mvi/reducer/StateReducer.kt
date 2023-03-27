package dev.sunnyday.arch.mvi.reducer

import dev.sunnyday.arch.mvi.Reducer
import dev.sunnyday.arch.mvi.Update

abstract class StateReducer<State : Any, in Event : Any> : Reducer<State, Event, Update<State, Nothing>> {

    override fun reduce(state: State, event: Event): Update<State, Nothing> {
        return Update.state(reduceState(state, event))
    }

    abstract fun reduceState(state: State, event: Event): State

    companion object {

        fun <State : Any, Event : Any> from(
            reducer: Reducer<State, Event, State>,
        ): Reducer<State, Event, Update<State, Nothing>> {
            return object : StateReducer<State, Event>() {
                override fun reduceState(state: State, event: Event): State = reducer.reduce(state, event)
            }
        }
    }
}