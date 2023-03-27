package dev.sunnyday.arch.mvi.coroutine.factory

import dev.sunnyday.arch.mvi.factory.MviFactoryCallContext
import org.junit.jupiter.api.Test
import kotlin.test.assertNotNull


class CoroutineFactoryContextTest {

    @Test
    fun `getParentCoroutineScope add coroutine factory call context if not exists`() {
        val factoryCallContext = MviFactoryCallContext()

        factoryCallContext.runWithFactoryContext {
            CoroutineFactoryContext.getParentCoroutineScope()
        }

        assertNotNull(factoryCallContext[CoroutineFactoryContext])
    }
}