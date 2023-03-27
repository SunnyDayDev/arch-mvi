package dev.sunnyday.arch.mvi.primitive

import java.util.concurrent.atomic.AtomicReference

class SharedObservableEvent<T> internal constructor(
    private val consumers: EventConsumersStore<T>,
) : ObservableEvent<T>, EventConsumer<T> {

    constructor() : this(AtomicEventConsumersStore())

    override fun observe(observer: EventConsumer<T>): Cancellable {
        updateSubscribers { it + observer }
        return Cancellable { updateSubscribers { it - observer } }
    }

    override fun onEvent(event: T) {
        consumers.get().forEach { subscriber ->
            subscriber.onEvent(event)
        }
    }

    private fun updateSubscribers(transform: (Array<EventConsumer<T>>) -> Array<EventConsumer<T>>) {
        while (true) {
            val currentSubscribers = consumers.get()
            val newSubscribers = transform.invoke(currentSubscribers)
            if (compareAndSetSubscribers(currentSubscribers, newSubscribers)) {
                break
            }
        }
    }

    private fun compareAndSetSubscribers(
        currentSubscribers: Array<EventConsumer<T>>,
        newSubscribers: Array<EventConsumer<T>>,
    ): Boolean {
        return consumers.compareAndSet(currentSubscribers, newSubscribers)
    }

    private operator fun Array<EventConsumer<T>>.minus(element: EventConsumer<T>): Array<EventConsumer<T>> {
        var isRemoved = false
        val original = this

        return Array(size - 1) { index ->
            if (original[index] === element) {
                isRemoved = true
            }

            if (isRemoved) original[index + 1] else original[index]
        }
    }

    internal interface EventConsumersStore<T> {
        fun get(): Array<EventConsumer<T>>

        fun compareAndSet(expected: Array<EventConsumer<T>>, new: Array<EventConsumer<T>>): Boolean
    }

    private class AtomicEventConsumersStore<T> : EventConsumersStore<T> {

        private val consumers = AtomicReference<Array<EventConsumer<T>>>(emptyArray())

        override fun get(): Array<EventConsumer<T>> = consumers.get()

        override fun compareAndSet(expected: Array<EventConsumer<T>>, new: Array<EventConsumer<T>>): Boolean {
            return consumers.compareAndSet(expected, new)
        }
    }
}