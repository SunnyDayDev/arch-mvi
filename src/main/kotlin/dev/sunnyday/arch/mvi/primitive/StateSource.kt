package dev.sunnyday.arch.mvi.primitive

import kotlinx.coroutines.flow.StateFlow

interface StateSource<out State> {

    val state: StateFlow<State>
}