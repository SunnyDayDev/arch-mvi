package dev.sunnyday.arch.mvi.primitive

fun interface ObservableEvent<out T> {

    fun observe(observer: EventConsumer<T>): Cancellable

    companion object {

        private val EMPTY = ObservableEvent<Nothing> { Cancellable.empty() }

        fun <T> empty(): ObservableEvent<T> = EMPTY
    }
}