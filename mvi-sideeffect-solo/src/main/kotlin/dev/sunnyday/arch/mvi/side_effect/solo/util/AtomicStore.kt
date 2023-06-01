package dev.sunnyday.arch.mvi.side_effect.solo.util

internal interface AtomicStore<T> {

    fun get(): T

    fun compareAndSet(expected: T, new: T): Boolean
}