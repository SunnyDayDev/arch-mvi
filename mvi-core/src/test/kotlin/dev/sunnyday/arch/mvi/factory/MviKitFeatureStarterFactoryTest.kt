package dev.sunnyday.arch.mvi.factory

import dev.sunnyday.arch.mvi.MviFeatureStarter
import dev.sunnyday.arch.mvi.factory.instance.MviFeatureInstanceFactory
import dev.sunnyday.arch.mvi.factory.instance.StateMachineInstanceFactory
import dev.sunnyday.arch.mvi.starter.InitialEventsProvider
import dev.sunnyday.arch.mvi.starter.InitialSideEffectsProvider
import dev.sunnyday.arch.mvi.starter.InitialStateProvider
import dev.sunnyday.arch.mvi.test.Event
import dev.sunnyday.arch.mvi.test.InputEvent
import dev.sunnyday.arch.mvi.test.SideEffect
import dev.sunnyday.arch.mvi.test.State
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame

class MviKitFeatureStarterFactoryTest {

    @Test
    fun `default arguments`() {
        val factory = mockk<MviKitFeatureStarterFactory>(relaxed = true)

        val initialStateProvider = mockk<InitialStateProvider<State>>()
        val stateMachineInstanceFactory = mockk<StateMachineInstanceFactory<State, Event, SideEffect>>()
        val featureInstanceFactory = mockk<MviFeatureInstanceFactory<State, InputEvent, Event, SideEffect>>()

        factory.createFeatureStarter(
            initialStateProvider = initialStateProvider,
            stateMachineInstanceFactory = stateMachineInstanceFactory,
            featureInstanceFactory = featureInstanceFactory,
        )

        verify {
            factory.createFeatureStarter(
                initialStateProvider = refEq(initialStateProvider),
                stateMachineInstanceFactory = refEq(stateMachineInstanceFactory),
                featureInstanceFactory = refEq(featureInstanceFactory),
                initialEventsProvider = isNull(),
                initialInputEventsProvider = isNull(),
                initialSideEffectsProvider = isNull(),
            )
        }
    }

    @Test
    fun `provide factory call key`() {
        val factory: MviKitFeatureStarterFactory = TestMviKitFeatureStarterFactory()
        assertEquals(MviKitFeatureStarterFactory, factory.key)
    }

    @Test
    fun `get element by key`() {
        val factory: MviKitFeatureStarterFactory = TestMviKitFeatureStarterFactory()
        assertSame(factory, factory[MviKitFeatureStarterFactory])
    }

    private class TestMviKitFeatureStarterFactory : MviKitFeatureStarterFactory {
        override fun <State : Any, InputEvent : Any, Event : Any, SideEffect : Any> createFeatureStarter(
            initialStateProvider: InitialStateProvider<State>,
            initialEventsProvider: InitialEventsProvider<State, Event>?,
            initialInputEventsProvider: InitialEventsProvider<State, InputEvent>?,
            initialSideEffectsProvider: InitialSideEffectsProvider<State, SideEffect>?,
            stateMachineInstanceFactory: StateMachineInstanceFactory<State, Event, SideEffect>,
            featureInstanceFactory: MviFeatureInstanceFactory<State, InputEvent, Event, SideEffect>
        ): MviFeatureStarter<State, InputEvent> {
            TODO("Not yet implemented")
        }

    }
}