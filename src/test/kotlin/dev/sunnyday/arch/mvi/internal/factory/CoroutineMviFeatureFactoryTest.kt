package dev.sunnyday.arch.mvi.internal.factory

import dev.sunnyday.arch.mvi.*
import dev.sunnyday.arch.mvi.factory.MviKit
import dev.sunnyday.arch.mvi.factory.MviStateMachineInstanceFactory
import dev.sunnyday.arch.mvi.internal.MviFeatureImpl
import dev.sunnyday.arch.mvi.internal.coroutine.MviCoroutineScope
import dev.sunnyday.arch.mvi.internal.event_handler.TransparentEventHandler
import dev.sunnyday.arch.mvi.internal.sideeffect_handler.EmptySideEffectHandler
import dev.sunnyday.arch.mvi.primitive.Observable
import dev.sunnyday.arch.mvi.primitive.ObservableValue
import dev.sunnyday.arch.mvi.primitive.OnReadyCallback
import dev.sunnyday.arch.mvi.test.*
import io.mockk.*
import kotlinx.coroutines.CoroutineScope
import org.junit.jupiter.api.Test
import kotlin.test.assertSame

class CoroutineMviFeatureFactoryTest {

    @Test
    fun `common create MviFeature`() {
        val featureCoroutineScope = MviCoroutineScope()

        mockkStatic(::MviCoroutineScope)
        mockkConstructor(CoroutineStateMachineInstanceFactoryScope::class, MviFeatureImpl::class)

        val initialState = State("initial")
        val eventHandler = createStub<EventHandler<InputEvent, Event>>()
        val sideEffectHandler = createStub<SideEffectHandler<SideEffect, Event>>()
        val onReadyCallback = createStub<OnReadyCallback>()
        val reducer = mockk<Reducer<State, Event, Update<State, SideEffect>>>()
        val stateTransitionListener = mockk<StateTransitionListener<StateTransition<State, Event, SideEffect>>>()

        val stateMachine = createStub<StateMachine<State, Event, SideEffect>>()

        every { MviCoroutineScope(any()) } returns featureCoroutineScope

        val featureConstructorRule = ConstructorRule.create<MviFeatureImpl<State, InputEvent, Event, SideEffect>>(
            EqMatcher(featureCoroutineScope, ref = true),
            EqMatcher(eventHandler, ref = true),
            EqMatcher(sideEffectHandler, ref = true),
            EqMatcher(stateMachine, ref = true),
            EqMatcher(onReadyCallback, ref = true),
        )

        fun MockKMatcherScope.expectedStateMachineFactoryScope(): CoroutineStateMachineInstanceFactoryScope<State, Event, SideEffect> {
            return constructedWith(
                EqMatcher(MviKit.stateMachineFactory, ref = true),
                EqMatcher(featureCoroutineScope, ref = true),
                EqMatcher(initialState, ref = true),
            )
        }

        every {
            expectedStateMachineFactoryScope().createStateMachine(any(), any())
        } returns stateMachine

        val feature = CoroutineMviFeatureFactory()
            .createFeature(
                initialState = initialState,
                eventHandler = eventHandler,
                sideEffectHandler = sideEffectHandler,
                stateMachineFactory = {
                    createStateMachine(
                        reducer = reducer,
                        stateTransitionListener = stateTransitionListener,
                    )
                },
                onReadyCallback = onReadyCallback,
            )

        featureConstructorRule.verifyConstructorCalled(feature)

        verify {
            expectedStateMachineFactoryScope().createStateMachine(refEq(reducer), refEq(stateTransitionListener))
        }

        unmockkAll()
    }

    @Test
    fun `if event handler is transparent set state machine as receiver`() = mockkConstructor(MviFeatureImpl::class) {

        val initialState = State("initial")
        val eventHandler = TransparentEventHandler<Event>()
        val sideEffectHandler = createStub<SideEffectHandler<SideEffect, Event>>()
        val onReadyCallback = createStub<OnReadyCallback>()
        val stateMachineInstanceFactory = mockk<MviStateMachineInstanceFactory<State, Event, SideEffect>>()

        val stateMachine = createStub<StateMachine<State, Event, SideEffect>>()

        ConstructorRule.create<MviFeatureImpl<State, InputEvent, Event, SideEffect>>(
            ConstantMatcher<CoroutineScope>(true),
            ConstantMatcher<EventHandler<InputEvent, Event>>(true),
            ConstantMatcher<SideEffectHandler<SideEffect, Event>>(true),
            ConstantMatcher<StateMachine<State, Event, SideEffect>>(true),
            ConstantMatcher<OnReadyCallback>(true),
        )

        every {
            stateMachineInstanceFactory.run {
                any<MviStateMachineInstanceFactory.FactoryScope<State, Event, SideEffect>>()
                    .createStateMachine()
            }
        } returns stateMachine

        CoroutineMviFeatureFactory()
            .createFeature(
                initialState = initialState,
                eventHandler = eventHandler,
                sideEffectHandler = sideEffectHandler,
                stateMachineFactory = stateMachineInstanceFactory,
                onReadyCallback = onReadyCallback,
            )

        assertSame(stateMachine, eventHandler.receiver)
    }
}