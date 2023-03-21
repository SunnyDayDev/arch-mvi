package dev.sunnyday.arch.mvi.internal.factory

import dev.sunnyday.arch.mvi.EventHandler
import dev.sunnyday.arch.mvi.MviFeature
import dev.sunnyday.arch.mvi.SideEffectHandler
import dev.sunnyday.arch.mvi.StateMachine
import dev.sunnyday.arch.mvi.factory.MviFeatureFactory
import dev.sunnyday.arch.mvi.primitive.OnReadyCallback
import dev.sunnyday.arch.mvi.starter.InitialEventsProvider
import dev.sunnyday.arch.mvi.starter.InitialSideEffectsProvider
import dev.sunnyday.arch.mvi.test.Event
import dev.sunnyday.arch.mvi.test.InputEvent
import dev.sunnyday.arch.mvi.test.SideEffect
import dev.sunnyday.arch.mvi.test.State
import io.mockk.*
import kotlinx.coroutines.CoroutineScope
import org.junit.jupiter.api.Test
import kotlin.test.assertSame

class CoroutineMviFeatureInstanceFactoryScopeTest {

    @Test
    fun `create feature by provided factory`() {
        val featureFactory = mockk<MviFeatureFactory>()
        val stateMachine = mockk<StateMachine<State, Event, SideEffect>>()
        val featureCoroutineScope = mockk<CoroutineScope>()
        val initialState = State("initial")
        val eventHandler = mockk<EventHandler<InputEvent, Event>>()
        val sideEffectHandler = mockk<SideEffectHandler<SideEffect, Event>>()
        val onReadyCallback = mockk<OnReadyCallback>()

        val feature = mockk<MviFeature<State, InputEvent>>()

        every {
            featureFactory.createFeature<State, InputEvent, Event, SideEffect>(
                initialState = any(),
                eventHandler = any(),
                sideEffectHandler = any(),
                stateMachineFactory = any(),
                onReadyCallback = any(),
            )
        } returns feature

        val actualFeature = CoroutineMviFeatureInstanceFactoryScope<State, InputEvent, Event, SideEffect>(
            featureFactory = featureFactory,
            featureCoroutineScope = featureCoroutineScope,
            initialState = initialState,
            initialInputEventsProvider = null,
            initialEventsProvider = null,
            initialSideEffectsProvider = null,
            stateMachine = stateMachine,
        )
            .createFeature(
                eventHandler = eventHandler,
                sideEffectHandler = sideEffectHandler,
                onReadyCallback = onReadyCallback,
            )

        assertSame(feature, actualFeature)
        verify {
            featureFactory.createFeature(
                initialState = initialState,
                eventHandler = eventHandler,
                sideEffectHandler = sideEffectHandler,
                stateMachineFactory = ConstantMviStateMachineInstanceFactory(stateMachine),
                onReadyCallback = onReadyCallback,
            )
        }
    }

    @Test
    fun `if factory is CoroutineMviFeatureFactory use optimized internal method`() {
        val featureFactory = mockk<CoroutineMviFeatureFactory>()
        val stateMachine = mockk<StateMachine<State, Event, SideEffect>>()
        val featureCoroutineScope = mockk<CoroutineScope>()
        val initialState = State("initial")
        val eventHandler = mockk<EventHandler<InputEvent, Event>>()
        val sideEffectHandler = mockk<SideEffectHandler<SideEffect, Event>>()
        val onReadyCallback = mockk<OnReadyCallback>()

        val feature = mockk<MviFeature<State, InputEvent>>()

        every {
            featureFactory.createFeature<State, InputEvent, Event, SideEffect>(
                coroutineScope = any(),
                eventHandler = any(),
                sideEffectHandler = any(),
                stateMachine = any(),
                onReadyCallback = any(),
            )
        } returns feature

        val actualFeature = CoroutineMviFeatureInstanceFactoryScope<State, InputEvent, Event, SideEffect>(
            featureFactory = featureFactory,
            featureCoroutineScope = featureCoroutineScope,
            initialState = initialState,
            initialInputEventsProvider = null,
            initialEventsProvider = null,
            initialSideEffectsProvider = null,
            stateMachine = stateMachine,
        )
            .createFeature(
                eventHandler = eventHandler,
                sideEffectHandler = sideEffectHandler,
                onReadyCallback = onReadyCallback,
            )

        assertSame(feature, actualFeature)
        verify {
            featureFactory.createFeature(
                coroutineScope = featureCoroutineScope,
                eventHandler = eventHandler,
                sideEffectHandler = sideEffectHandler,
                stateMachine = stateMachine,
                onReadyCallback = onReadyCallback,
            )
        }
    }

    @Test
    fun `onReadyCallback sends initial events`() {
        val featureCoroutineScope = mockk<CoroutineScope>()
        val initialState = State("initial")
        val featureFactory = mockk<CoroutineMviFeatureFactory>(relaxed = true)
        val stateMachine = mockk<StateMachine<State, Event, SideEffect>>(relaxed = true)
        val eventHandler = mockk<EventHandler<InputEvent, Event>>(relaxed = true)
        val sideEffectHandler = mockk<SideEffectHandler<SideEffect, Event>>(relaxed = true)
        val onReadyCallback = mockk<OnReadyCallback>(relaxed = true)

        val initialInputEventsProvider = mockk<InitialEventsProvider<State, InputEvent>> {
            every { getInitialEvents(any()) } returns listOf(InputEvent("initial"))
        }
        val initialEventsProvider = mockk<InitialEventsProvider<State, Event>> {
            every { getInitialEvents(any()) } returns listOf(Event("initial"))
        }
        val initialSideEffectsProvider = mockk<InitialSideEffectsProvider<State, SideEffect>> {
            every { getInitialSideEffects(any()) } returns listOf(SideEffect("initial"))
        }

        CoroutineMviFeatureInstanceFactoryScope(
            featureFactory = featureFactory,
            featureCoroutineScope = featureCoroutineScope,
            initialState = initialState,
            initialInputEventsProvider = initialInputEventsProvider,
            initialEventsProvider = initialEventsProvider,
            initialSideEffectsProvider = initialSideEffectsProvider,
            stateMachine = stateMachine,
        )
            .createFeature(
                eventHandler = eventHandler,
                sideEffectHandler = sideEffectHandler,
                onReadyCallback = onReadyCallback,
            )

        val onReadyCallbackSlot = CapturingSlot<OnReadyCallback>()
        verify {
            featureFactory.createFeature<State, InputEvent, Event, SideEffect>(
                coroutineScope = any(),
                eventHandler = any(),
                sideEffectHandler = any(),
                stateMachine = any(),
                onReadyCallback = capture(onReadyCallbackSlot),
            )
        }

        confirmVerified(
            initialInputEventsProvider,
            initialEventsProvider,
            initialSideEffectsProvider,
            eventHandler,
            stateMachine,
            sideEffectHandler,
            onReadyCallback
        )

        onReadyCallbackSlot.captured.onReady()

        verify {
            initialInputEventsProvider.getInitialEvents(initialState)
            initialEventsProvider.getInitialEvents(initialState)
            initialSideEffectsProvider.getInitialSideEffects(initialState)

            eventHandler.onEvent(InputEvent("initial"))
            stateMachine.onEvent(Event("initial"))
            sideEffectHandler.onSideEffect(SideEffect("initial"))

            onReadyCallback.onReady()
        }
    }
}