package dev.sunnyday.arch.mvi.internal.factory

import dev.sunnyday.arch.mvi.*
import dev.sunnyday.arch.mvi.factory.MviFeatureStarterFactory
import dev.sunnyday.arch.mvi.factory.MviKit
import dev.sunnyday.arch.mvi.factory.createFeature
import dev.sunnyday.arch.mvi.internal.coroutine.MviCoroutineScope
import dev.sunnyday.arch.mvi.internal.event_handler.TransparentEventHandler
import dev.sunnyday.arch.mvi.primitive.OnReadyCallback
import dev.sunnyday.arch.mvi.starter.InitialEventsProvider
import dev.sunnyday.arch.mvi.starter.InitialSideEffectsProvider
import dev.sunnyday.arch.mvi.starter.InitialStateProvider
import dev.sunnyday.arch.mvi.test.*
import io.mockk.*
import io.mockk.mockk
import org.junit.jupiter.api.Test
import kotlin.test.assertSame

class CoroutineMviFeatureStarterFactoryTest {

    @Test
    fun `create feature starter`() {
        val coroutineScope = MviCoroutineScope()

        mockkStatic(::MviCoroutineScope)
        mockkConstructor(
            CoroutineStateMachineInstanceFactoryScope::class,
            CoroutineMviFeatureInstanceFactoryScope::class,
        )

        every { MviCoroutineScope(any()) } returns coroutineScope

        val initialState = State("initial")
        val initialStateProvider = createStub<InitialStateProvider<State>>(
            delegate = mockk {
                every { provideInitialState() } returns initialState
            },
            delegateCalls = {
                provideInitialState()
            }
        )
        val initialEventsProvider = createStub<InitialEventsProvider<State, Event>>()
        val initialInputEventsProvider = createStub<InitialEventsProvider<State, InputEvent>>()
        val initialSideEffectsProvider = createStub<InitialSideEffectsProvider<State, SideEffect>>()

        val reducer = mockk<Reducer<State, Event, Update<State, SideEffect>>>()
        val stateTransitionListener = mockk<StateTransitionListener<StateTransition<State, Event, SideEffect>>>()
        val eventHandler = mockk<EventHandler<InputEvent, Event>>()
        val sideEffectHandler = mockk<SideEffectHandler<SideEffect, Event>>()
        val onReadyCallback = mockk<OnReadyCallback>()

        val expectedStateMachine = createStub<StateMachine<State, Event, SideEffect>>()
        val expectedFeature = mockk<MviFeature<State, InputEvent>>()


        fun MockKMatcherScope.expectedStateMachineFactoryScope(): CoroutineStateMachineInstanceFactoryScope<State, Event, SideEffect> {
            return constructedWith(
                EqMatcher(MviKit.stateMachineFactory, ref = true),
                EqMatcher(coroutineScope, ref = true),
                EqMatcher(initialState, ref = true),
            )
        }

        every {
            expectedStateMachineFactoryScope().createStateMachine(
                reducer = any(),
                stateTransitionListener = any(),
            )
        } returns expectedStateMachine

        fun MockKMatcherScope.expectedFeatureFactoryScope(): CoroutineMviFeatureInstanceFactoryScope<State, InputEvent, Event, SideEffect> {
            return constructedWith(
                EqMatcher(MviKit.featureFactory, ref = true),
                EqMatcher(coroutineScope, ref = true),
                EqMatcher(initialState, ref = true),
                EqMatcher(initialInputEventsProvider, ref = true),
                EqMatcher(initialEventsProvider, ref = true),
                EqMatcher(initialSideEffectsProvider, ref = true),
                EqMatcher(expectedStateMachine, ref = true),
            )
        }

        every {
            expectedFeatureFactoryScope().createFeature(
                eventHandler = any(),
                sideEffectHandler = any(),
                onReadyCallback = any(),
            )
        } returns expectedFeature

        val factory: MviFeatureStarterFactory = CoroutineMviFeatureStarterFactory()
        val starter = factory.createFeatureStarter(
            initialStateProvider = initialStateProvider,
            initialEventsProvider = initialEventsProvider,
            initialInputEventsProvider = initialInputEventsProvider,
            initialSideEffectsProvider = initialSideEffectsProvider,
            stateMachineInstanceFactory = {
                createStateMachine(
                    reducer = reducer,
                    stateTransitionListener = stateTransitionListener,
                )
            },
            featureInstanceFactory = {
                createFeature(
                    eventHandler = eventHandler,
                    sideEffectHandler = sideEffectHandler,
                    onReadyCallback = onReadyCallback,
                )
            },
        )

        confirmVerified(
            reducer,
            stateTransitionListener,
            eventHandler,
            sideEffectHandler,
            onReadyCallback
        )

        val actualFeature = starter.start()

        assertSame(expectedFeature, actualFeature)

        verify {
            expectedStateMachineFactoryScope().createStateMachine(
                reducer = refEq(reducer),
                stateTransitionListener = refEq(stateTransitionListener),
            )
            expectedFeatureFactoryScope().createFeature(
                eventHandler = refEq(eventHandler),
                sideEffectHandler = refEq(sideEffectHandler),
                onReadyCallback = refEq(onReadyCallback),
            )
        }

        unmockkAll()
    }

    @Test
    fun `create transparent feature starter`() {
        val coroutineScope = MviCoroutineScope()

        mockkStatic(::MviCoroutineScope)
        mockkConstructor(
            CoroutineStateMachineInstanceFactoryScope::class,
            CoroutineMviFeatureInstanceFactoryScope::class,
        )

        every { MviCoroutineScope(any()) } returns coroutineScope

        val initialState = State("initial")
        val initialStateProvider = createStub<InitialStateProvider<State>>(
            delegate = mockk {
                every { provideInitialState() } returns initialState
            },
            delegateCalls = {
                provideInitialState()
            }
        )
        val initialEventsProvider = createStub<InitialEventsProvider<State, Event>>()
        val initialInputEventsProvider = createStub<InitialEventsProvider<State, Event>>()
        val initialSideEffectsProvider = createStub<InitialSideEffectsProvider<State, SideEffect>>()

        val reducer = mockk<Reducer<State, Event, Update<State, SideEffect>>>()
        val stateTransitionListener = mockk<StateTransitionListener<StateTransition<State, Event, SideEffect>>>()
        val sideEffectHandler = mockk<SideEffectHandler<SideEffect, Event>>()
        val onReadyCallback = mockk<OnReadyCallback>()

        val expectedStateMachine = createStub<StateMachine<State, Event, SideEffect>>()
        val expectedFeature = mockk<MviFeature<State, Event>>()


        fun MockKMatcherScope.expectedStateMachineFactoryScope(): CoroutineStateMachineInstanceFactoryScope<State, Event, SideEffect> {
            return constructedWith(
                EqMatcher(MviKit.stateMachineFactory, ref = true),
                EqMatcher(coroutineScope, ref = true),
                EqMatcher(initialState, ref = true),
            )
        }

        every {
            expectedStateMachineFactoryScope().createStateMachine(
                reducer = any(),
                stateTransitionListener = any(),
            )
        } returns expectedStateMachine

        fun MockKMatcherScope.expectedFeatureFactoryScope(): CoroutineMviFeatureInstanceFactoryScope<State, Event, Event, SideEffect> {
            return constructedWith(
                EqMatcher(MviKit.featureFactory, ref = true),
                EqMatcher(coroutineScope, ref = true),
                EqMatcher(initialState, ref = true),
                EqMatcher(initialInputEventsProvider, ref = true),
                EqMatcher(initialEventsProvider, ref = true),
                EqMatcher(initialSideEffectsProvider, ref = true),
                EqMatcher(expectedStateMachine, ref = true),
            )
        }

        every {
            expectedFeatureFactoryScope().createFeature(
                eventHandler = any(),
                sideEffectHandler = any(),
                onReadyCallback = any(),
            )
        } returns expectedFeature

        val factory: MviFeatureStarterFactory = CoroutineMviFeatureStarterFactory()
        val starter = factory.createFeatureStarter(
            initialStateProvider = initialStateProvider,
            initialEventsProvider = initialEventsProvider,
            initialInputEventsProvider = initialInputEventsProvider,
            initialSideEffectsProvider = initialSideEffectsProvider,
            stateMachineInstanceFactory = {
                createStateMachine(
                    reducer = reducer,
                    stateTransitionListener = stateTransitionListener,
                )
            },
            featureInstanceFactory = {
                createFeature(
                    sideEffectHandler = sideEffectHandler,
                    onReadyCallback = onReadyCallback,
                )
            },
        )

        confirmVerified(
            reducer,
            stateTransitionListener,
            sideEffectHandler,
            onReadyCallback
        )

        val actualFeature = starter.start()

        assertSame(expectedFeature, actualFeature)

        verify {
            expectedStateMachineFactoryScope().createStateMachine(
                reducer = refEq(reducer),
                stateTransitionListener = refEq(stateTransitionListener),
            )
            expectedFeatureFactoryScope().createFeature(
                eventHandler = ofType<TransparentEventHandler<Event>>(),
                sideEffectHandler = refEq(sideEffectHandler),
                onReadyCallback = refEq(onReadyCallback),
            )
        }

        unmockkAll()
    }
}