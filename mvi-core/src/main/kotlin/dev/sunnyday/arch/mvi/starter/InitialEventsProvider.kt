package dev.sunnyday.arch.mvi.starter

fun interface InitialEventsProvider<in State, out Event> {

    fun getInitialEvents(state: State): List<Event>
}