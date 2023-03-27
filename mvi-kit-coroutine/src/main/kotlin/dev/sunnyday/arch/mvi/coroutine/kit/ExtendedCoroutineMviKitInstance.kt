package dev.sunnyday.arch.mvi.coroutine.kit

import dev.sunnyday.arch.mvi.coroutine.factory.CoroutineFactoryContext
import dev.sunnyday.arch.mvi.factory.*
import dev.sunnyday.arch.mvi.kit.ContextEnabledMviKitInstance
import dev.sunnyday.arch.mvi.kit.MviKitInstance
import kotlinx.coroutines.CoroutineScope

internal class ExtendedCoroutineMviKitInstance(
    private val original: MviKitInstance,
) : ContextEnabledMviKitInstance() {

    override val stateMachineFactory: MviKitStateMachineFactory = original
    override val featureFactory: MviKitFeatureFactory =  original
    override val starterFactory: MviKitFeatureStarterFactory =  original

    var parentCoroutineScope: CoroutineScope? = null

    override fun createCallFactoryContext(): MviFactoryCallContext {
        val originalKit = original
        val originalContext = if (originalKit is ContextEnabledMviKitInstance) {
            createCallFactoryContextFrom(originalKit)
        } else {
            super.createCallFactoryContext()
                .apply { add(originalKit) }
        }

        return originalContext.apply {
            val coroutineFactoryContext = instantiateCoroutineFactoryContext()
            coroutineFactoryContext.parentCoroutineScope = parentCoroutineScope
        }
    }

    private fun MviFactoryCallContext.instantiateCoroutineFactoryContext(): CoroutineFactoryContext {
        return this[CoroutineFactoryContext] ?: CoroutineFactoryContext().also(::add)
    }
}