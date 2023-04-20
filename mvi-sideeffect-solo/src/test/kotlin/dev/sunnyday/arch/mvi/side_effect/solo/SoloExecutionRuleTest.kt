package dev.sunnyday.arch.mvi.side_effect.solo

import dev.sunnyday.arch.mvi.primitive.ObservableEvent
import dev.sunnyday.arch.mvi.side_effect.solo.SoloExecutionRuleConfig.*
import dev.sunnyday.arch.mvi.side_effect.solo.filter.side_effect.sideEffectsWithType
import dev.sunnyday.arch.mvi.test.SideEffect
import io.mockk.confirmVerified
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import kotlin.test.assertSame

class SoloExecutionRuleTest {

    @Test
    fun `Independent rule do nothing`() {
        val config = mockk<SoloExecutionRuleConfig<SideEffect>>()

        SoloExecutionRule.independent<SideEffect>()
            .run { config.build() }

        confirmVerified(config)
    }

    @Test
    fun `default execution rule is Independent`() {
        val sideEffectHandler = object : SoloSideEffect<Any, Any, Any> {
            override fun execute(dependency: Any): ObservableEvent<Any> = ObservableEvent.empty()
        }

        assertSame(SoloExecutionRule.independent(), sideEffectHandler.executionRule)
    }

    @Test
    fun `default OnEnqueueBuilder_awaitComplete don't fail on error and don't require complete`() {
        val builder = mockk<OnEnqueueBuilder<Any>>(relaxed = true)

        builder.awaitComplete(sideEffectsWithType<Any>())

        verify {
            builder.awaitComplete(sideEffectsWithType<Any>(), failOnError = false, requireSuccess = false)
        }
    }

    @Test
    fun `executionRule is shortcut for ParticleExecutionRule`() {
        val config = mockk<SoloExecutionRuleConfig<SideEffect>>(relaxed = true)
        val onEnqueueConfig = mockk<OnEnqueueBuilder<out SideEffect>.() -> Unit>()
        val onExecuteConfig = mockk<OnExecuteBuilder<out SideEffect>.() -> Unit>()
        val onCancelConfig = mockk<OnCancelBuilder<out SideEffect>.(ExecutingSideEffect<SideEffect>) -> Unit>()

        @Suppress("RemoveExplicitTypeArguments")
        val rule = executionRule<SideEffect> {
            onEnqueue(onEnqueueConfig)
            onExecute(onExecuteConfig)
            onCancel(onCancelConfig)
        }

        rule.run { config.build() }

        verify {
            config.onEnqueue(onEnqueueConfig)
            config.onExecute(onExecuteConfig)
            config.onCancel(onCancelConfig)
        }
    }
}