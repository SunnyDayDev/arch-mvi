package dev.sunnyday.arch.mvi.coroutine.factory.instance

import dev.sunnyday.arch.mvi.EventHandler
import dev.sunnyday.arch.mvi.MviFeature
import dev.sunnyday.arch.mvi.SideEffectHandler
import dev.sunnyday.arch.mvi.StateMachine
import dev.sunnyday.arch.mvi.coroutine.factory.CoroutineMviKitFeatureFactory
import dev.sunnyday.arch.mvi.factory.MviFactoryCallContext
import dev.sunnyday.arch.mvi.factory.instance.StateMachineInstanceFactory
import dev.sunnyday.arch.mvi.primitive.OnReadyCallback
import dev.sunnyday.arch.mvi.test.*
import dev.sunnyday.arch.mvi.test.common.createTestFeature
import io.mockk.*
import org.junit.jupiter.api.Test
import kotlin.test.assertSame

class CoroutineMviFeatureInstanceFactoryScopeTest {

    private val mockkStore = MockkStore()

    @Test
    fun `create feature by factory in context`() {
        val feature = mockk<MviFeature<State, InputEvent>>()
        val factory = spyk<CoroutineMviKitFeatureFactory> {
            every { createTestFeature(anyProvider()) } returns feature
        }

        val eventHandler = mockk<EventHandler<InputEvent, Event>>()
        val sideEffectHandler = mockk<SideEffectHandler<SideEffect, Event>>()

        val actualFeature = MviFactoryCallContext.create(factory).runWithFactoryContext {
            CoroutineMviFeatureInstanceFactoryScope<State, InputEvent, Event, SideEffect>(
                initialState = State("initial"),
                stateMachineInstanceFactory = mockk(),
            ).createFeature(
                eventHandler = eventHandler,
                sideEffectHandler = sideEffectHandler,
            )
        }

        assertSame(feature, actualFeature)
        verify {
            factory.createTestFeature(
                mockProvider = anyProvider(),
                initialState = State("initial"),
                eventHandler = eventHandler,
                sideEffectHandler = sideEffectHandler,
            )
        }
    }

    @Test
    fun `create feature by default factory`() = mockkConstructor(CoroutineMviKitFeatureFactory::class) {
        val feature = mockk<MviFeature<State, InputEvent>>()

        every { constructedWith<CoroutineMviKitFeatureFactory>().createTestFeature(anyProvider()) } returns feature

        val stateMachineInstanceFactory = mockk<StateMachineInstanceFactory<State, Event, SideEffect>>()
        val eventHandler = mockk<EventHandler<InputEvent, Event>>()
        val sideEffectHandler = mockk<SideEffectHandler<SideEffect, Event>>()

        val factoryScope = spyk(
            CoroutineMviFeatureInstanceFactoryScope<State, InputEvent, Event, SideEffect>(
                initialState = State("initial"),
                stateMachineInstanceFactory = stateMachineInstanceFactory,
            ),
            recordPrivateCalls = true
        )

        val actualFeature = factoryScope.createFeature(
            eventHandler = eventHandler,
            sideEffectHandler = sideEffectHandler,
        )

        assertSame(feature, actualFeature)
        verify { constructedWith<CoroutineMviKitFeatureFactory>().createTestFeature(anyProvider()) }
    }

    @Test
    fun `onReady callback sends initial events proper receivers`() {
        val feature = mockk<MviFeature<State, InputEvent>>()
        val onReadySlot = CapturingSlot<OnReadyCallback>()
        val factory = spyk<CoroutineMviKitFeatureFactory> {
            every {
                createTestFeature(
                    anyProvider(),
                    onReadyCallback = capture(onReadySlot),
                )
            } answers {
                val stateMachineInstanceFactory = arg<StateMachineInstanceFactory<State, Event, SideEffect>>(3)
                val stateMachineFactoryScope =
                    mockk<StateMachineInstanceFactory.FactoryScope<State, Event, SideEffect>>()
                stateMachineInstanceFactory.run { stateMachineFactoryScope.createStateMachine() }

                feature
            }
        }

        val eventHandler = mockk<EventHandler<InputEvent, Event>>(relaxed = true)
        val stateMachine = mockk<StateMachine<State, Event, SideEffect>>(relaxed = true)
        val sideEffectHandler = mockk<SideEffectHandler<SideEffect, Event>>(relaxed = true)
        val onReadyCallback = mockk<OnReadyCallback>(relaxed = true)

        MviFactoryCallContext.create(factory).runWithFactoryContext {
            CoroutineMviFeatureInstanceFactoryScope(
                initialState = State("initial"),
                stateMachineInstanceFactory = { stateMachine },
                initialInputEventsProvider = { listOf(InputEvent()) },
                initialEventsProvider = { listOf(Event()) },
                initialSideEffectsProvider = { listOf(SideEffect()) },
            ).createFeature(
                eventHandler = eventHandler,
                sideEffectHandler = sideEffectHandler,
                onReadyCallback = onReadyCallback,
            )
        }

        onReadySlot.captured.onReady()

        verify {
            eventHandler.onEvent(InputEvent())
            stateMachine.onEvent(Event())
            sideEffectHandler.onSideEffect(SideEffect())
            onReadyCallback.onReady()
        }
    }
}