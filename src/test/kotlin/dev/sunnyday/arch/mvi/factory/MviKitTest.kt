package dev.sunnyday.arch.mvi.factory

import dev.sunnyday.arch.mvi.*
import dev.sunnyday.arch.mvi.primitive.OnReadyCallback
import dev.sunnyday.arch.mvi.starter.InitialEventsProvider
import dev.sunnyday.arch.mvi.starter.InitialSideEffectsProvider
import dev.sunnyday.arch.mvi.starter.InitialStateProvider
import dev.sunnyday.arch.mvi.starter.MviFeatureStarter
import dev.sunnyday.arch.mvi.test.Event
import dev.sunnyday.arch.mvi.test.InputEvent
import dev.sunnyday.arch.mvi.test.SideEffect
import dev.sunnyday.arch.mvi.test.State
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import kotlin.test.assertSame

class MviKitTest {

    @Test
    fun `delegate create state machine method to factory`() {
        val previousStateMachineFactory = MviKit.stateMachineFactory

        val stateMachineFactory = mockk<MviStateMachineFactory>()
        MviKit.stateMachineFactory = stateMachineFactory

        val initialState = mockk<State>()
        val reducer = mockk<Reducer<State, Event, Update<State, SideEffect>>>()
        val stateTransitionListener = mockk<StateTransitionListener<StateTransition<State, Event, SideEffect>>>()

        val expectedStateMachine = mockk<StateMachine<State, Event, SideEffect>>()

        every {
            stateMachineFactory.createStateMachine<State, Event, SideEffect>(
                initialState = any(),
                reducer = any(),
                stateTransitionListener = any(),
            )
        } returns expectedStateMachine

        val actualStateMachine = MviKit.createStateMachine(
            initialState = initialState,
            reducer = reducer,
            stateTransitionListener = stateTransitionListener,
        )

        assertSame(expectedStateMachine, actualStateMachine)
        verify {
            stateMachineFactory.createStateMachine(
                initialState = refEq(initialState),
                reducer = refEq(reducer),
                stateTransitionListener = refEq(stateTransitionListener),
            )
        }

        MviKit.stateMachineFactory = previousStateMachineFactory
    }

    @Test
    fun `delegate create feature method to factory`() {
        val previousFeatureFactory = MviKit.featureFactory

        val featureFactory = mockk<MviFeatureFactory>()
        MviKit.featureFactory = featureFactory

        val initialState = mockk<State>()
        val eventHandler = mockk<EventHandler<InputEvent, Event>>()
        val sideEffectHandler = mockk<SideEffectHandler<SideEffect, Event>>()
        val stateMachineFactory = mockk<MviStateMachineInstanceFactory<State, Event, SideEffect>>()
        val onReadyCallback = mockk<OnReadyCallback>()

        val expectedFeature = mockk<MviFeature<State, InputEvent>>()

        every {
            featureFactory.createFeature<State, InputEvent, Event, SideEffect>(
                initialState = any(),
                eventHandler = any(),
                sideEffectHandler = any(),
                stateMachineFactory = any(),
                onReadyCallback = any(),
            )
        } returns expectedFeature

        val actualStateMachine = MviKit.createFeature(
            initialState = initialState,
            eventHandler = eventHandler,
            sideEffectHandler = sideEffectHandler,
            stateMachineFactory = stateMachineFactory,
            onReadyCallback = onReadyCallback,
        )

        assertSame(expectedFeature, actualStateMachine)
        verify {
            featureFactory.createFeature(
                initialState = refEq(initialState),
                eventHandler = refEq(eventHandler),
                sideEffectHandler = refEq(sideEffectHandler),
                stateMachineFactory = refEq(stateMachineFactory),
                onReadyCallback = refEq(onReadyCallback),
            )
        }

        MviKit.featureFactory = previousFeatureFactory
    }

    @Test
    fun `delegate create feature starter method to factory`() {
        val previousStarterFactory = MviKit.starterFactory
        val starterFactory = mockk<MviFeatureStarterFactory>()
        MviKit.starterFactory = starterFactory

        val initialStateProvider = mockk<InitialStateProvider<State>>()
        val initialEventsProvider = mockk<InitialEventsProvider<State, Event>>()
        val initialInputEventsProvider = mockk<InitialEventsProvider<State, InputEvent>>()
        val initialSideEffectsProvider = mockk<InitialSideEffectsProvider<State, SideEffect>>()
        val stateMachineInstanceFactory = mockk<MviStateMachineInstanceFactory<State, Event, SideEffect>>()
        val featureInstanceFactory = mockk<MviFeatureInstanceFactory<State, InputEvent, Event, SideEffect>>()

        val expectedStarter = mockk<MviFeatureStarter<State, InputEvent>>()

        every {
            starterFactory.createFeatureStarter<State, InputEvent, Event, SideEffect>(
                initialStateProvider = any(),
                initialEventsProvider = any(),
                initialInputEventsProvider = any(),
                initialSideEffectsProvider = any(),
                stateMachineInstanceFactory = any(),
                featureInstanceFactory = any(),
            )
        } returns expectedStarter

        val actualStarter = MviKit.createFeatureStarter(
            initialStateProvider = initialStateProvider,
            initialEventsProvider = initialEventsProvider,
            initialInputEventsProvider = initialInputEventsProvider,
            initialSideEffectsProvider = initialSideEffectsProvider,
            stateMachineInstanceFactory = stateMachineInstanceFactory,
            featureInstanceFactory = featureInstanceFactory,
        )

        assertSame(expectedStarter, actualStarter)
        verify {
            starterFactory.createFeatureStarter(
                initialStateProvider = refEq(initialStateProvider),
                initialEventsProvider = refEq(initialEventsProvider),
                initialInputEventsProvider = refEq(initialInputEventsProvider),
                initialSideEffectsProvider = refEq(initialSideEffectsProvider),
                stateMachineInstanceFactory = refEq(stateMachineInstanceFactory),
                featureInstanceFactory = refEq(featureInstanceFactory),
            )
        }

        MviKit.starterFactory = previousStarterFactory
    }
}