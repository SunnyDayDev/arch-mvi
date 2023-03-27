package dev.sunnyday.arch.mvi.test


data class State(val name: String = "state")

data class InputEvent(val name: String = "inputEvent")

data class Event(val name: String = "event")

data class SideEffect(val name: String = "sideEffect")