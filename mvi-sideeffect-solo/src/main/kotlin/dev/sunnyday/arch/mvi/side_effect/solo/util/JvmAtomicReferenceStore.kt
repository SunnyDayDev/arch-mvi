package dev.sunnyday.arch.mvi.side_effect.solo.util

import java.util.concurrent.atomic.AtomicReference

internal class JvmAtomicReferenceStore<T : Any>(initialValue: T) : AtomicStore<T> {

    private val atomicReference = AtomicReference(initialValue)

    override fun get(): T {
        return atomicReference.get()
    }

    override fun compareAndSet(expected: T, new: T): Boolean {
        return atomicReference.compareAndSet(expected, new)
    }
}