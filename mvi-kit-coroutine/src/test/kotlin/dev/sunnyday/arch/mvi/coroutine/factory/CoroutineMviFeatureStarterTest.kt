package dev.sunnyday.arch.mvi.coroutine.factory

import dev.sunnyday.arch.mvi.MviFeature
import dev.sunnyday.arch.mvi.coroutine.factory.instance.CoroutineMviFeatureInstanceFactoryScope
import dev.sunnyday.arch.mvi.factory.MviFactoryCallContext
import dev.sunnyday.arch.mvi.factory.instance.MviFeatureInstanceFactory
import dev.sunnyday.arch.mvi.factory.instance.StateMachineInstanceFactory
import dev.sunnyday.arch.mvi.starter.InitialEventsProvider
import dev.sunnyday.arch.mvi.starter.InitialSideEffectsProvider
import dev.sunnyday.arch.mvi.starter.InitialStateProvider
import dev.sunnyday.arch.mvi.test.*
import io.mockk.*
import org.junit.jupiter.api.Test
import kotlin.test.assertSame


class CoroutineMviFeatureStarterTest {

    @Test
    fun `start new feature`() = mockkConstructor(CoroutineMviFeatureInstanceFactoryScope::class) {
        val expectedFeature = mockk<MviFeature<State, InputEvent>>()
        val factoryCallContext = MviFactoryCallContext()
        val initialStateProvider = stub<InitialStateProvider<State>> {
            every { provideInitialState() } returns State("initial")
        }
        val initialEventsProvider = stub<InitialEventsProvider<State, Event>>()
        val initialInputEventsProvider = stub<InitialEventsProvider<State, InputEvent>>()
        val initialSideEffectsProvider = stub<InitialSideEffectsProvider<State, SideEffect>>()
        val stateMachineInstanceFactory = stub<StateMachineInstanceFactory<State, Event, SideEffect>>()
        val featureFactoryScopeSlot = slot<MviFeatureInstanceFactory.FactoryScope<State, InputEvent, Event, SideEffect>>()
        val featureInstanceFactory = stub<MviFeatureInstanceFactory<State, InputEvent, Event, SideEffect>> {
            every {
                run { capture(featureFactoryScopeSlot).createFeature() }
            } returns expectedFeature
        }

        val scopeConstructorRule =
            ConstructorRule.create<CoroutineMviFeatureInstanceFactoryScope<State, InputEvent, Event, SideEffect>>(
                EqMatcher(State("initial")),
                EqMatcher(stateMachineInstanceFactory, ref = true),
                EqMatcher(initialInputEventsProvider, ref = true),
                EqMatcher(initialEventsProvider, ref = true),
                EqMatcher(initialSideEffectsProvider, ref = true),
            )

        val starter = CoroutineMviFeatureStarter(
            factoryCallContext = factoryCallContext,
            initialStateProvider = initialStateProvider,
            initialEventsProvider = initialEventsProvider,
            initialInputEventsProvider = initialInputEventsProvider,
            initialSideEffectsProvider = initialSideEffectsProvider,
            stateMachineInstanceFactory = stateMachineInstanceFactory,
            featureInstanceFactory = featureInstanceFactory,
        )

        val feature = starter.start()

        assertSame(expectedFeature, feature)
        scopeConstructorRule.verifyConstructorCalled(featureFactoryScopeSlot.captured)
    }
}