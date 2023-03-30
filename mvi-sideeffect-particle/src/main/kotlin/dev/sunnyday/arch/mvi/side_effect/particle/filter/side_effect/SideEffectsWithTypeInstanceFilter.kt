package dev.sunnyday.arch.mvi.side_effect.particle.filter.side_effect

import dev.sunnyday.arch.mvi.side_effect.particle.ExecutingSideEffect
import dev.sunnyday.arch.mvi.side_effect.particle.InstanceFilter
import kotlin.reflect.KClass

@PublishedApi
internal data class SideEffectsWithTypeInstanceFilter<T : Any>(
    private val klass: KClass<T>
) : InstanceFilter<ExecutingSideEffect<*>, ExecutingSideEffect<T>> {

    override fun accept(value: ExecutingSideEffect<*>): Boolean {
        return klass.isInstance(value.sideEffect)
    }

    override fun get(value: ExecutingSideEffect<*>): ExecutingSideEffect<T> {
        return value as ExecutingSideEffect<T>
    }

    companion object {

        inline fun <reified T : Any> of(): SideEffectsWithTypeInstanceFilter<T> {
            return SideEffectsWithTypeInstanceFilter(T::class)
        }
    }
}