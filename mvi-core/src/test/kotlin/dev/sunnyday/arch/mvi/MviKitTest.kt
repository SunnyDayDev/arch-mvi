package dev.sunnyday.arch.mvi

import dev.sunnyday.arch.mvi.factory.*
import dev.sunnyday.arch.mvi.kit.ContextEnabledMviKitInstance
import dev.sunnyday.arch.mvi.test.*
import dev.sunnyday.arch.mvi.test.common.createTestFeature
import dev.sunnyday.arch.mvi.test.common.createTestFeatureStarter
import dev.sunnyday.arch.mvi.test.common.createTestStateMachine
import io.mockk.Ordering
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.*

class MviKitTest {

    private val mockStore = MockkStore()

    @Test
    fun `require to be initialized`() {
        val expectedErrorMessage = "Before you can use MviKit it must be configured with MviKit.setup(...)"

        runCatching { MviKit.createTestStateMachine(mockStore) }
            .exceptionOrNull()
            .let { assertEquals(expectedErrorMessage, it?.message) }

        runCatching { MviKit.createTestFeature(mockStore) }
            .exceptionOrNull()
            .let { assertEquals(expectedErrorMessage, it?.message) }

        runCatching { MviKit.createTestFeatureStarter(mockStore) }
            .exceptionOrNull()
            .let { assertEquals(expectedErrorMessage, it?.message) }
    }

    @Test
    fun `setup with factories`() {
        val stateMachineFactory = mockk<MviKitStateMachineFactory>(relaxed = true)
        val featureFactory = mockk<MviKitFeatureFactory>(relaxed = true)
        val starterFactory = mockk<MviKitFeatureStarterFactory>(relaxed = true)

        MviKit.setup(
            stateMachineFactory = stateMachineFactory,
            featureFactory = featureFactory,
            starterFactory = starterFactory,
        )

        assertTrue(MviKit.isReady)
    }

    @Test
    fun `delegate calls to factories`() {
        val stateMachineFactory = mockk<MviKitStateMachineFactory>(relaxed = true)
        val featureFactory = mockk<MviKitFeatureFactory>(relaxed = true)
        val starterFactory = mockk<MviKitFeatureStarterFactory>(relaxed = true)

        MviKit.setup(
            stateMachineFactory = stateMachineFactory,
            featureFactory = featureFactory,
            starterFactory = starterFactory,
        )

        MviKit.createTestStateMachine(mockStore)
        MviKit.createTestFeature(mockStore)
        MviKit.createTestFeatureStarter(mockStore)

        assertTrue(MviKit.isReady)
        verify(Ordering.ORDERED) {
            stateMachineFactory.createTestStateMachine(mockStore)
            featureFactory.createTestFeature(mockStore)
            starterFactory.createTestFeatureStarter(mockStore)
        }
    }

    @Test
    fun `setup with instance`() {
        val kitInstance = mockk<ContextEnabledMviKitInstance>(relaxed = true)

        MviKit.setup(kitInstance)

        assertTrue(MviKit.isReady)
    }

    @Test
    fun `delegate calls to instance`() {
        val kitInstance = mockk<ContextEnabledMviKitInstance>(relaxed = true)

        MviKit.setup(kitInstance)

        MviKit.createTestStateMachine(mockStore)
        MviKit.createTestFeature(mockStore)
        MviKit.createTestFeatureStarter(mockStore)

        assertTrue(MviKit.isReady)
        verify(ordering = Ordering.ORDERED) {
            kitInstance.createTestStateMachine(mockStore)
            kitInstance.createTestFeature(mockStore)
            kitInstance.createTestFeatureStarter(mockStore)
        }
    }

    @Test
    fun `reset clear factories and increment key`() {
        val stateMachineFactory = mockk<MviKitStateMachineFactory>()
        val featureFactory = mockk<MviKitFeatureFactory>()
        val starterFactory = mockk<MviKitFeatureStarterFactory>()

        MviKit.setup(
            stateMachineFactory = stateMachineFactory,
            featureFactory = featureFactory,
            starterFactory = starterFactory,
        )

        MviKit.reset()

        assertFalse(MviKit.isReady)
    }

    @BeforeEach
    fun resetMviKit() {
        MviKit.reset()
    }
}