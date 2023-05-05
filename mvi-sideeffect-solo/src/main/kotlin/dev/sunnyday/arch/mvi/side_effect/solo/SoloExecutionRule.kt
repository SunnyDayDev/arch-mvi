package dev.sunnyday.arch.mvi.side_effect.solo

import kotlin.time.Duration

fun interface SoloExecutionRule<SideEffect : Any> {

    fun SoloExecutionRuleConfig<SideEffect>.build()

    companion object {

        @Suppress("UNCHECKED_CAST")
        fun <SideEffect: Any> independent(): SoloExecutionRule<SideEffect> {
            return IndependentRuleInstance as SoloExecutionRule<SideEffect>
        }
    }

    private object IndependentRuleInstance : SoloExecutionRule<Nothing> {

        override fun SoloExecutionRuleConfig<Nothing>.build() = Unit
    }
}

interface SoloExecutionRuleConfig<SideEffect : Any> {

    fun setId(id: ExecutingSideEffect.Id): SoloExecutionRuleConfig<SideEffect>

    fun onEnqueue(config: OnEnqueueBuilder<SideEffect>.() -> Unit): SoloExecutionRuleConfig<SideEffect>

    fun onExecute(config: OnExecuteBuilder<SideEffect>.() -> Unit): SoloExecutionRuleConfig<SideEffect>

    fun onCancel(config: OnCancelBuilder<SideEffect>.() -> Unit): SoloExecutionRuleConfig<SideEffect>

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

        fun <S : SideEffect> registerListener(
            filter: InstanceFilter<ExecutingSideEffect<SideEffect>, ExecutingSideEffect<S>>,
            listener: (ExecutingSideEffect<S>) -> Unit
        )
    }

    interface OnCancelBuilder<SideEffect : Any> : Builder<SideEffect> {

        fun sendSignal(signal: Any)

        fun cancelOther(sideEffectsFilter: InstanceFilter<ExecutingSideEffect<SideEffect>, *>)
    }

    interface Builder<SideEffect : Any> {

        fun <S : SideEffect> getExecutingSideEffects(
            filter: InstanceFilter<ExecutingSideEffect<SideEffect>, ExecutingSideEffect<S>>,
        ): List<ExecutingSideEffect<S>>
    }
}

inline fun <T : Any> executionRule(
    crossinline configure: SoloExecutionRuleConfig<T>.() -> Unit,
): SoloExecutionRule<T> {
    return SoloExecutionRule {
        configure.invoke(this)
    }
}

fun <SE : Any> SoloExecutionRuleConfig.Builder<SE>.getExecutingSideEffects(): List<ExecutingSideEffect<SE>> {
    return getExecutingSideEffects(InstanceFilter.Filter { true })
}