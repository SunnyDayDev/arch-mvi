package dev.sunnyday.arch.mvi.kit

import dev.sunnyday.arch.mvi.MviFeature
import dev.sunnyday.arch.mvi.MviFeatureStarter
import dev.sunnyday.arch.mvi.StateMachine
import dev.sunnyday.arch.mvi.factory.*
import dev.sunnyday.arch.mvi.test.*
import dev.sunnyday.arch.mvi.test.common.createTestFeature
import dev.sunnyday.arch.mvi.test.common.createTestFeatureStarter
import dev.sunnyday.arch.mvi.test.common.createTestStateMachine
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertSame

class ContextEnabledMviKitInstanceTest {

    private val stateMachineFactory = mockk<MviKitStateMachineFactory>(relaxed = true)
    private val featureFactory = mockk<MviKitFeatureFactory>(relaxed = true)
    private val starterFactory = mockk<MviKitFeatureStarterFactory>(relaxed = true)

    private val kitInstance: MviKitInstance = TestContextEnabledMviKitInstance(
        stateMachineFactory = stateMachineFactory,
        featureFactory = featureFactory,
        starterFactory = starterFactory,
    )

    private val argumentsMock = MockkStore()

    @BeforeEach
    fun `ensure factory context is null before call`() {
        assertNull(MviFactoryCallContext.getCurrentFactoryContext())
    }

    @Test
    fun `delegate create state machine method to factory`() {
        val expectedStateMachine = mockk<StateMachine<State, Event, SideEffect>>()

        var callFactoryContext: MviFactoryCallContext? = null

        every { stateMachineFactory.createTestStateMachine(anyProvider()) } answers {
            callFactoryContext = MviFactoryCallContext.getCurrentFactoryContext()
            expectedStateMachine
        }


        val actualStateMachine = kitInstance.createTestStateMachine(argumentsMock)

        assertSame(expectedStateMachine, actualStateMachine)
        assertEquals(getExpectedFactoryContext(), callFactoryContext)
        verify { stateMachineFactory.createTestStateMachine(argumentsMock) }
    }

    @Test
    fun `delegate create feature method to factory`() {
        val expectedFeature = mockk<MviFeature<State, InputEvent>>()

        var callFactoryContext: MviFactoryCallContext? = null

        every { featureFactory.createTestFeature(anyProvider()) } answers {
            callFactoryContext = MviFactoryCallContext.getCurrentFactoryContext()
            expectedFeature
        }

        val actualStateMachine = kitInstance.createTestFeature(argumentsMock)

        assertSame(expectedFeature, actualStateMachine)
        assertEquals(getExpectedFactoryContext(), callFactoryContext)
        verify { featureFactory.createTestFeature(argumentsMock) }
    }

    @Test
    fun `delegate create feature starter method to factory`() {
        val expectedStarter = mockk<MviFeatureStarter<State, InputEvent>>()

        var callFactoryContext: MviFactoryCallContext? = null

        every { starterFactory.createTestFeatureStarter(anyProvider()) } answers {
            callFactoryContext = MviFactoryCallContext.getCurrentFactoryContext()
            expectedStarter
        }

        val actualStarter = kitInstance.createTestFeatureStarter(argumentsMock)

        assertSame(expectedStarter, actualStarter)
        assertEquals(getExpectedFactoryContext(), callFactoryContext)
        verify { starterFactory.createTestFeatureStarter(argumentsMock) }
    }

    @Test
    fun `repeated call doesn't wrap with new factory call context`() {
        var stateMachineFactoryCallContext: MviFactoryCallContext? = null
        var featureFactoryCallContext: MviFactoryCallContext? = null
        var staterFactoryCallContext: MviFactoryCallContext? = null

        every { stateMachineFactory.createTestStateMachine(anyProvider()) } answers {
            stateMachineFactoryCallContext = MviFactoryCallContext.getCurrentFactoryContext()
            mockk()
        }

        every { featureFactory.createTestFeature(anyProvider()) } answers {
            featureFactoryCallContext = MviFactoryCallContext.getCurrentFactoryContext()
            kitInstance.createTestStateMachine(argumentsMock)
            mockk()
        }

        every { starterFactory.createTestFeatureStarter(anyProvider()) } answers {
            staterFactoryCallContext = MviFactoryCallContext.getCurrentFactoryContext()
            kitInstance.createTestFeature(argumentsMock)
            mockk()
        }

        kitInstance.createTestFeatureStarter(argumentsMock)

        assertSame(staterFactoryCallContext, featureFactoryCallContext)
        assertSame(featureFactoryCallContext, stateMachineFactoryCallContext)
    }

    @AfterEach
    fun `ensure factory context is null after call`() {
        assertNull(MviFactoryCallContext.getCurrentFactoryContext())
    }

    private fun getExpectedFactoryContext(): MviFactoryCallContext {
        return MviFactoryCallContext().apply {
            this[MviKitInstance] = kitInstance
            this[MviKitStateMachineFactory] = stateMachineFactory
            this[MviKitFeatureFactory] = featureFactory
            this[MviKitFeatureStarterFactory] = starterFactory
        }
    }

    private class TestContextEnabledMviKitInstance(
        override val stateMachineFactory: MviKitStateMachineFactory,
        override val featureFactory: MviKitFeatureFactory,
        override val starterFactory: MviKitFeatureStarterFactory
    ) : ContextEnabledMviKitInstance()
}