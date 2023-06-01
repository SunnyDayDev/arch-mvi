package dev.sunnyday.arch.mvi.side_effect.solo

interface InstanceFilter<in T, out R> {

    fun accept(value: T): Boolean

    fun get(value: T): R

    interface Filter<T> : InstanceFilter<T, T> {

        override fun get(value: T): T = value
    }

    companion object {

        inline fun <T> Filter(crossinline filter: (T) -> Boolean) = object : Filter<T> {
            override fun accept(value: T): Boolean {
                return filter.invoke(value)
            }
        }
    }
}

