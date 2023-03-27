package dev.sunnyday.arch.mvi.factory.instance

import dev.sunnyday.arch.mvi.EventHandler
import dev.sunnyday.arch.mvi.SideEffectHandler
import dev.sunnyday.arch.mvi.event_handler.TransparentEventHandler
import dev.sunnyday.arch.mvi.primitive.OnReadyCallback
import dev.sunnyday.arch.mvi.test.*
import io.mockk.Ordering
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test

class MviFeatureInstanceFactoryScopeTest {

    @Test
    fun `create instance with factory`() {
        val eventHandler = mockk<EventHandler<InputEvent, Event>>()
        val sideEffectHandler = mockk<SideEffectHandler<SideEffect, Event>>()
        val onReadyCallback = mockk<OnReadyCallback>()

        val factoryScope =
            mockk<MviFeatureInstanceFactory.FactoryScope<State, InputEvent, Event, SideEffect>>(relaxed = true)

        MviFeatureInstanceFactory<State, InputEvent, Event, SideEffect> {
            createFeature(
                eventHandler = eventHandler,
                sideEffectHandler = sideEffectHandler,
                onReadyCallback = onReadyCallback,
            )
        }.run { factoryScope.createFeature() }

        MviFeatureInstanceFactory<State, InputEvent, Event, SideEffect> {
            createFeature(
                eventHandler = eventHandler,
                sideEffectHandler = sideEffectHandler,
            )
        }.run { factoryScope.createFeature() }

        verify(ordering = Ordering.ORDERED) {
            factoryScope.createFeature(
                eventHandler = refEq(eventHandler),
                sideEffectHandler = refEq(sideEffectHandler),
                onReadyCallback = refEq(onReadyCallback),
            )
            factoryScope.createFeature(
                eventHandler = refEq(eventHandler),
                sideEffectHandler = refEq(sideEffectHandler),
                onReadyCallback = isNull(),
            )
        }
    }

    @Test
    fun `short scope createFeature sets TransparentEventHandler`() {
        val factoryScope =
            mockk<MviFeatureInstanceFactory.FactoryScope<State, Event, Event, SideEffect>>(relaxed = true)
        factoryScope.createFeature(
            sideEffectHandler = mockk(),
            onReadyCallback = mockk(),
        )

        verify {
            factoryScope.createFeature(
                eventHandler = ofType<TransparentEventHandler<Event>>(),
                sideEffectHandler = any(),
                onReadyCallback = any(),
            )
        }
    }

    @Test
    fun `shortest scope createFeature sets TransparentEventHandler`() {
        val factoryScope =
            mockk<MviFeatureInstanceFactory.FactoryScope<State, Event, Event, SideEffect>>(relaxed = true)
        factoryScope.createFeature(
            sideEffectHandler = mockk(),
        )

        verify {
            factoryScope.createFeature(
                eventHandler = ofType<TransparentEventHandler<Event>>(),
                sideEffectHandler = any(),
                onReadyCallback = isNull(),
            )
        }
    }
}