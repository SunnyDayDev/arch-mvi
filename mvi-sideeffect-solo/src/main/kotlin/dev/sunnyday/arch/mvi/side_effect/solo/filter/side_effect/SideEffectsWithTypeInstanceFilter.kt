package dev.sunnyday.arch.mvi.side_effect.solo.filter.side_effect

import dev.sunnyday.arch.mvi.side_effect.solo.ExecutingSideEffect
import dev.sunnyday.arch.mvi.side_effect.solo.InstanceFilter
import kotlin.reflect.KClass

@PublishedApi
internal data class SideEffectsWithTypeInstanceFilter<T : Any>(
    private val klass: KClass<T>,
    private val allowSupertype: Boolean = true,
) : InstanceFilter<ExecutingSideEffect<*>, ExecutingSideEffect<T>> {

    override fun accept(value: ExecutingSideEffect<*>): Boolean {
        return if (allowSupertype) {
            klass.isInstance(value.sideEffect)
        } else {
            value.sideEffect::class == klass
        }
    }

    override fun get(value: ExecutingSideEffect<*>): ExecutingSideEffect<T> {
        return value as ExecutingSideEffect<T>
    }

    companion object {

        inline fun <reified T : Any> of(allowSupertype: Boolean = true): SideEffectsWithTypeInstanceFilter<T> {
            return SideEffectsWithTypeInstanceFilter(
                klass = T::class,
                allowSupertype = allowSupertype,
            )
        }
    }
}