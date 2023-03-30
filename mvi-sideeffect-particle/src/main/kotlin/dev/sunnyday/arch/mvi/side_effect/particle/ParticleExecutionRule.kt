package dev.sunnyday.arch.mvi.side_effect.particle

import kotlin.time.Duration

fun interface ParticleExecutionRule<SideEffect : Any> {

    fun ParticleExecutionRuleConfig<SideEffect>.build()

    companion object {

        @Suppress("UNCHECKED_CAST")
        fun <SideEffect: Any> independent(): ParticleExecutionRule<SideEffect> {
            return IndependentRuleInstance as ParticleExecutionRule<SideEffect>
        }
    }

    private object IndependentRuleInstance : ParticleExecutionRule<Nothing> {

        override fun ParticleExecutionRuleConfig<Nothing>.build() = Unit
    }
}

interface ParticleExecutionRuleConfig<SideEffect : Any> {

    fun setId(id: ExecutingSideEffect.Id): ParticleExecutionRuleConfig<SideEffect>

    fun onEnqueue(config: OnEnqueueBuilder<SideEffect>.() -> Unit): ParticleExecutionRuleConfig<SideEffect>

    fun onExecute(config: OnExecuteBuilder<SideEffect>.() -> Unit): ParticleExecutionRuleConfig<SideEffect>

    fun onCancel(
        config: OnCancelBuilder<SideEffect>.(trigger: ExecutingSideEffect<SideEffect>) -> Unit,
    ): ParticleExecutionRuleConfig<SideEffect>

    interface OnEnqueueBuilder<SideEffect : Any> : OnCancelBuilder<SideEffect> {

        fun skipIfAlreadyExecuting(filter: InstanceFilter<ExecutingSideEffect<SideEffect>, *>)

        fun delay(duration: Duration)

        fun awaitComplete(
            filter: InstanceFilter<ExecutingSideEffect<SideEffect>, *>,
            failOnError: Boolean = false,
            requireSuccess: Boolean = false,
        )
    }

    interface OnExecuteBuilder<SideEffect : Any> : OnCancelBuilder<SideEffect> {

        fun skipIfAlreadyExecuting(filter: InstanceFilter<ExecutingSideEffect<SideEffect>, *>)

        fun registerCancelOnSignal(signalFilter: InstanceFilter<Any, *>)

        fun <S : SideEffect> getExecutingSideEffects(
            filter: InstanceFilter<ExecutingSideEffect<SideEffect>, ExecutingSideEffect<S>>,
        ): List<ExecutingSideEffect<S>>

        fun <S : SideEffect> registerListener(
            filter: InstanceFilter<ExecutingSideEffect<SideEffect>, ExecutingSideEffect<S>>,
            listener: (ExecutingSideEffect<S>) -> Unit
        )
    }

    interface OnCancelBuilder<SideEffect : Any> : ParticleRuleBuilder<SideEffect> {

        fun sendSignal(signal: Any)

        fun cancelOther(sideEffectsFilter: InstanceFilter<ExecutingSideEffect<SideEffect>, *>)
    }

    interface ParticleRuleBuilder<SideEffect : Any>
}

inline fun <T : Any> executionRule(
    crossinline configure: ParticleExecutionRuleConfig<out T>.() -> Unit,
): ParticleExecutionRule<T> {
    return ParticleExecutionRule { configure() }
}