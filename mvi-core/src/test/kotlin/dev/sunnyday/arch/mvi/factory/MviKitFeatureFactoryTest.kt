package dev.sunnyday.arch.mvi.factory

import dev.sunnyday.arch.mvi.EventHandler
import dev.sunnyday.arch.mvi.MviFeature
import dev.sunnyday.arch.mvi.SideEffectHandler
import dev.sunnyday.arch.mvi.factory.instance.StateMachineInstanceFactory
import dev.sunnyday.arch.mvi.primitive.OnReadyCallback
import dev.sunnyday.arch.mvi.sideeffect_handler.EmptySideEffectHandler
import dev.sunnyday.arch.mvi.test.Event
import dev.sunnyday.arch.mvi.test.InputEvent
import dev.sunnyday.arch.mvi.test.SideEffect
import dev.sunnyday.arch.mvi.test.State
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame

class MviKitFeatureFactoryTest {

    @Test
    fun `default values`() {
        val factory = mockk<MviKitFeatureFactory>(relaxed = true)

        val initialState = mockk<State>()
        val eventHandler = mockk<EventHandler<InputEvent, Event>>()
        val stateMachineFactory = mockk<StateMachineInstanceFactory<State, Event, SideEffect>>()

        factory.createFeature(
            initialState = initialState,
            eventHandler = eventHandler,
            stateMachineFactory = stateMachineFactory,
        )

        verify {
            factory.createFeature(
                initialState = refEq(initialState),
                eventHandler = refEq(eventHandler),
                sideEffectHandler = ofType<EmptySideEffectHandler>(),
                stateMachineFactory = refEq(stateMachineFactory),
                onReadyCallback = isNull(),
            )
        }
    }

    @Test
    fun `provide factory call key`() {
        val factory: MviKitFeatureFactory = TestMviKitFeatureFactory()
        assertEquals(MviKitFeatureFactory.Key, factory.key)
    }

    @Test
    fun `get element by key`() {
        val factory: MviKitFeatureFactory = TestMviKitFeatureFactory()
        assertSame(factory, factory[MviKitFeatureFactory.Key])
    }

    private class TestMviKitFeatureFactory : MviKitFeatureFactory {

        override fun <State : Any, InputEvent : Any, Event : Any, SideEffect : Any> createFeature(
            initialState: State,
            eventHandler: EventHandler<InputEvent, Event>,
            sideEffectHandler: SideEffectHandler<SideEffect, Event>,
            stateMachineFactory: StateMachineInstanceFactory<State, Event, SideEffect>,
            onReadyCallback: OnReadyCallback?
        ): MviFeature<State, InputEvent> {
            TODO("Not yet implemented")
        }
    }
}