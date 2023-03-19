package dev.sunnyday.arch.mvi.primitive

fun interface Cancellable {

    fun cancel()

    companion object {

        private val EMPTY = Cancellable { }

        fun empty(): Cancellable = EMPTY
    }
}