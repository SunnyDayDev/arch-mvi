package dev.sunnyday.arch.mvi.primitive

interface EventSource<out Event : Any> {

    val outputEvents: Observable<Event>
}