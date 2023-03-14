package dev.sunnyday.arch.mvi.primitive

fun interface EventConsumer<in Event: Any> {

    fun onEvent(event: Event)
}