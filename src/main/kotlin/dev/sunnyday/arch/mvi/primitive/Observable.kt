package dev.sunnyday.arch.mvi.primitive

fun interface Observable<out T> {

    fun observe(observer: (T) -> Unit): Cancellable

    companion object {

        private val EMPTY = Observable<Nothing> { Cancellable.empty() }

        fun <T> empty(): Observable<T> = EMPTY
    }
}