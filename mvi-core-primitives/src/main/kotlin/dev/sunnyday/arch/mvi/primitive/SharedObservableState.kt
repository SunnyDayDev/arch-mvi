package dev.sunnyday.arch.mvi.primitive

import kotlin.properties.Delegates

// TODO: https://github.com/SunnyDayDev/arch-mvi/issues/19
//  improve concurrency
class SharedObservableState<T>(initialValue: T) : ObservableState<T>, EventConsumer<T> {

    private val observable = SharedObservableEvent<T>()

    override var value by Delegates.observable(initialValue) { _, _, newValue ->
        observable.onEvent(newValue)
    }

    override fun observe(observer: EventConsumer<T>): Cancellable {
        observer.onEvent(value)
        return observable.observe(observer)
    }

    override fun onEvent(event: T) {
        value = event
    }
}