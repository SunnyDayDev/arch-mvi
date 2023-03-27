package dev.sunnyday.arch.mvi.coroutine.kit

import dev.sunnyday.arch.mvi.coroutine.factory.CoroutineFactoryContext
import dev.sunnyday.arch.mvi.factory.MviFactoryCallContext
import dev.sunnyday.arch.mvi.kit.MviKitInstance
import dev.sunnyday.arch.mvi.test.MockkStore
import dev.sunnyday.arch.mvi.test.anyProvider
import dev.sunnyday.arch.mvi.test.common.createTestStateMachine
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import kotlin.test.assertNotNull


class ExtendedCoroutineMviKitInstanceTest {

    @Test
    fun `can wrap non context enabled MviKitInstances`() {
        var factoryCallContext: MviFactoryCallContext? = null

        val kit = mockk<MviKitInstance> {
            every { key } returns MviKitInstance
            every { get(MviKitInstance) } returns this
            every { createTestStateMachine(anyProvider()) } answers {
                factoryCallContext = MviFactoryCallContext.getCurrentFactoryContext()?.clone()
                mockk()
            }
        }

        ExtendedCoroutineMviKitInstance(kit)
            .createTestStateMachine(MockkStore())

        assertNotNull(factoryCallContext?.get(CoroutineFactoryContext))
    }
}