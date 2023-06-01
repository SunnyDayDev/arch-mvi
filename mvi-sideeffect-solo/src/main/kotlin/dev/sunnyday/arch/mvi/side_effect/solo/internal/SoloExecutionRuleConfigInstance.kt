package dev.sunnyday.arch.mvi.side_effect.solo.internal

import dev.sunnyday.arch.mvi.side_effect.solo.ExecutingSideEffect
import dev.sunnyday.arch.mvi.side_effect.solo.SoloExecutionRuleConfig
import kotlinx.coroutines.CoroutineDispatcher

internal class SoloExecutionRuleConfigInstance<SideEffect : Any> : SoloExecutionRuleConfig<SideEffect> {

    var sideEffectId: ExecutingSideEffect.Id = ExecutingSideEffect.Id.Unique()
    var dispatcher: CoroutineDispatcher? = null

    var onEnqueueRule: (SoloExecutionRuleConfig.OnEnqueueBuilder<SideEffect>.() -> Unit)? = null
    var onExecuteRule: (SoloExecutionRuleConfig.OnExecuteBuilder<SideEffect>.() -> Unit)? = null
    var onCancelRule: (SoloExecutionRuleConfig.OnCancelBuilder<SideEffect>.() -> Unit)? = null

    override fun setId(id: ExecutingSideEffect.Id): SoloExecutionRuleConfig<SideEffect> = apply {
        sideEffectId = id
    }

    override fun setDispatcher(dispatcher: CoroutineDispatcher): SoloExecutionRuleConfig<SideEffect> = apply {
        this.dispatcher = dispatcher
    }

    override fun onEnqueue(
        config: SoloExecutionRuleConfig.OnEnqueueBuilder<SideEffect>.() -> Unit,
    ) = apply {
        onEnqueueRule = config
    }

    override fun onExecute(
        config: SoloExecutionRuleConfig.OnExecuteBuilder<SideEffect>.() -> Unit,
    ): SoloExecutionRuleConfig<SideEffect> = apply {
        onExecuteRule = config
    }

    override fun onCancel(
        config: SoloExecutionRuleConfig.OnCancelBuilder<SideEffect>.() -> Unit,
    ): SoloExecutionRuleConfig<SideEffect> = apply {
        onCancelRule = config
    }
}