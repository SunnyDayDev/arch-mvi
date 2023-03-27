package dev.sunnyday.arch.mvi.coroutine.factory

import dev.sunnyday.arch.mvi.*
import dev.sunnyday.arch.mvi.coroutine.CoroutineScopes
import dev.sunnyday.arch.mvi.coroutine.factory.instance.CoroutineMviFeatureInstanceFactoryScope
import dev.sunnyday.arch.mvi.coroutine.factory.instance.CoroutineStateMachineInstanceFactoryScope
import dev.sunnyday.arch.mvi.event_handler.TransparentEventHandler
import dev.sunnyday.arch.mvi.factory.MviFactoryCallContext
import dev.sunnyday.arch.mvi.factory.MviKitFeatureFactory
import dev.sunnyday.arch.mvi.factory.MviKitFeatureStarterFactory
import dev.sunnyday.arch.mvi.factory.MviKitStateMachineFactory
import dev.sunnyday.arch.mvi.factory.instance.MviFeatureInstanceFactory
import dev.sunnyday.arch.mvi.factory.instance.StateMachineInstanceFactory
import dev.sunnyday.arch.mvi.factory.instance.createFeature
import dev.sunnyday.arch.mvi.primitive.OnReadyCallback
import dev.sunnyday.arch.mvi.starter.InitialEventsProvider
import dev.sunnyday.arch.mvi.starter.InitialSideEffectsProvider
import dev.sunnyday.arch.mvi.starter.InitialStateProvider
import dev.sunnyday.arch.mvi.test.*
import dev.sunnyday.arch.mvi.test.common.createTestFeatureStarter
import io.mockk.*
import io.mockk.mockk
import org.junit.jupiter.api.Test
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertSame

class CoroutineMviKitFeatureStarterFactoryTest {

    @Test
    fun `create feature starter`() = mockkConstructor(CoroutineMviFeatureStarter::class) {
        val factoryCallContext = MviFactoryCallContext()
        val initialStateProvider = stub<InitialStateProvider<State>>()
        val initialEventsProvider = stub<InitialEventsProvider<State, Event>>()
        val initialInputEventsProvider = stub<InitialEventsProvider<State, InputEvent>>()
        val initialSideEffectsProvider = stub<InitialSideEffectsProvider<State, SideEffect>>()
        val stateMachineInstanceFactory = stub<StateMachineInstanceFactory<State, Event, SideEffect>>()
        val featureInstanceFactory = stub<MviFeatureInstanceFactory<State, InputEvent, Event, SideEffect>>()

        val starterConstructorRule =
            ConstructorRule.create<CoroutineMviFeatureStarter<State, InputEvent, Event, SideEffect>>(
                EqMatcher(factoryCallContext, ref = true),
                EqMatcher(initialStateProvider, ref = true),
                EqMatcher(initialEventsProvider, ref = true),
                EqMatcher(initialInputEventsProvider, ref = true),
                EqMatcher(initialSideEffectsProvider, ref = true),
                EqMatcher(stateMachineInstanceFactory, ref = true),
                EqMatcher(featureInstanceFactory, ref = true),
            )

        val starter = factoryCallContext.runWithFactoryContext {
            CoroutineMviKitFeatureStarterFactory()
                .createFeatureStarter(
                    initialStateProvider = initialStateProvider,
                    initialEventsProvider = initialEventsProvider,
                    initialInputEventsProvider = initialInputEventsProvider,
                    initialSideEffectsProvider = initialSideEffectsProvider,
                    stateMachineInstanceFactory = stateMachineInstanceFactory,
                    featureInstanceFactory = featureInstanceFactory,
                )
        }

        starterConstructorRule.verifyConstructorCalled(starter)
    }


    @Test
    fun `if no in factory call context, provide coroutine factory call context`() =
        mockkConstructor(CoroutineMviFeatureStarter::class) {
            var factoryCallContext: MviFactoryCallContext? = null

            val starterConstructorRule =
                ConstructorRule.create<CoroutineMviFeatureStarter<State, InputEvent, Event, SideEffect>>(
                    FunctionMatcher<MviFactoryCallContext>(
                        { factoryCallContext = it; true },
                        MviFactoryCallContext::class
                    ),
                    ConstantMatcher<InitialStateProvider<State>>(true),
                    ConstantMatcher<InitialEventsProvider<State, Event>>(true),
                    ConstantMatcher<InitialEventsProvider<State, InputEvent>>(true),
                    ConstantMatcher<InitialSideEffectsProvider<State, SideEffect>>(true),
                    ConstantMatcher<StateMachineInstanceFactory<State, Event, SideEffect>>(true),
                    ConstantMatcher<MviFeatureInstanceFactory<State, InputEvent, Event, SideEffect>>(true),
                )

            CoroutineMviKitFeatureStarterFactory().createTestFeatureStarter(StubProvider())
                .let(starterConstructorRule::verifyConstructorCalled)

            val context = factoryCallContext
            assertNotNull(context)
            assertIs<CoroutineMviKitStateMachineFactory>(context[MviKitStateMachineFactory])
            assertIs<CoroutineMviKitFeatureFactory>(context[MviKitFeatureFactory])
            assertIs<CoroutineMviKitFeatureStarterFactory>(context[MviKitFeatureStarterFactory])
        }
}