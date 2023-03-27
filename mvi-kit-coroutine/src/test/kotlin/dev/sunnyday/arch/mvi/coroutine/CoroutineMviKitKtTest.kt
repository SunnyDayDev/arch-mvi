package dev.sunnyday.arch.mvi.coroutine

import dev.sunnyday.arch.mvi.MviKit
import dev.sunnyday.arch.mvi.coroutine.factory.CoroutineFactoryContext
import dev.sunnyday.arch.mvi.coroutine.factory.CoroutineMviKitFeatureFactory
import dev.sunnyday.arch.mvi.coroutine.factory.CoroutineMviKitFeatureStarterFactory
import dev.sunnyday.arch.mvi.coroutine.factory.CoroutineMviKitStateMachineFactory
import dev.sunnyday.arch.mvi.factory.MviKitFeatureFactory
import dev.sunnyday.arch.mvi.factory.MviKitFeatureStarterFactory
import dev.sunnyday.arch.mvi.factory.MviKitStateMachineFactory
import dev.sunnyday.arch.mvi.kit.ContextEnabledMviKitInstance
import dev.sunnyday.arch.mvi.kit.MviKitInstance
import dev.sunnyday.arch.mvi.test.MockkStore
import dev.sunnyday.arch.mvi.test.anyProvider
import dev.sunnyday.arch.mvi.test.common.createTestStateMachine
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.verify
import kotlinx.coroutines.CoroutineScope
import org.junit.jupiter.api.Test
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.test.assertSame


class CoroutineMviKitKtTest {

    @Test
    fun `setup coroutine MviKit`() = mockkObject(MviKit) {
        MviKit.setupFactories()

        verify {
            MviKit.setup(
                stateMachineFactory = ofType<CoroutineMviKitStateMachineFactory>(),
                featureFactory = ofType<CoroutineMviKitFeatureFactory>(),
                starterFactory = ofType<CoroutineMviKitFeatureStarterFactory>(),
            )
        }
    }

    @Test
    fun `MviKit can be extended with coroutine scope`() {
        val coroutineScope = mockk<CoroutineScope>()
        var actualCoroutineScope: CoroutineScope? = null

        val kit = object : ContextEnabledMviKitInstance() {
            override val stateMachineFactory: MviKitStateMachineFactory = mockk(relaxed = true) {
                every { createTestStateMachine(anyProvider()) } answers {
                    actualCoroutineScope = CoroutineFactoryContext.getParentCoroutineScope()
                    mockk()
                }
            }
            override val featureFactory: MviKitFeatureFactory = mockk(relaxed = true)
            override val starterFactory: MviKitFeatureStarterFactory = mockk(relaxed = true)

        }

        kit.withParentCoroutine(mockk())
            .withParentCoroutine(coroutineScope)
            .createTestStateMachine(MockkStore())

        assertSame(coroutineScope, actualCoroutineScope)
    }
}