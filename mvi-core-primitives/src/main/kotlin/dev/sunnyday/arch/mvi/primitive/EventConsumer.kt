package dev.sunnyday.arch.mvi.primitive

fun interface EventConsumer<in Event> {

    fun onEvent(event: Event)
}