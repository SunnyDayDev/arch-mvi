package dev.sunnyday.arch.mvi.coroutine.factory

import dev.sunnyday.arch.mvi.factory.MviFactoryCallContext
import dev.sunnyday.arch.mvi.coroutine.CoroutineScopes
import kotlinx.coroutines.CoroutineScope

internal data class CoroutineFactoryContext(
    var parentCoroutineScope: CoroutineScope? = null,
) : MviFactoryCallContext.Element {

    override val key: MviFactoryCallContext.Key<*> = Companion

    companion object : MviFactoryCallContext.Key<CoroutineFactoryContext> {

        fun getParentCoroutineScope(): CoroutineScope? {
            val context = MviFactoryCallContext.getCurrentFactoryContext() ?: return null

            val coroutineFactoryContext = context[CoroutineFactoryContext]
            return if (coroutineFactoryContext != null) {
                coroutineFactoryContext.parentCoroutineScope
            } else {
                val coroutineScope = CoroutineScopes.MviCoroutineScope()
                context.add(CoroutineFactoryContext(coroutineScope))
                coroutineScope
            }
        }
    }
}