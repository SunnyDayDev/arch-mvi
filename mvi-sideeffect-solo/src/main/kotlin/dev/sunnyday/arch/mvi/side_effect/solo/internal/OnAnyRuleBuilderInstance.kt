package dev.sunnyday.arch.mvi.side_effect.solo.internal

import dev.sunnyday.arch.mvi.side_effect.solo.ExecutingSideEffect
import dev.sunnyday.arch.mvi.side_effect.solo.InstanceFilter
import dev.sunnyday.arch.mvi.side_effect.solo.SoloExecutionRuleConfig
import dev.sunnyday.arch.mvi.side_effect.solo.util.AtomicStore
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.filterNot
import kotlinx.coroutines.flow.firstOrNull
import kotlin.time.Duration

internal class OnAnyRuleBuilderInstance<SideEffect : Any>(
    private val executionSideEffect: ExecutingSideEffect<SideEffect>,
    private val executingSideEffectsStore: AtomicStore<Array<ExecutingSideEffect<SideEffect>>>,
    private val signalFlow: MutableSharedFlow<Any>,
) : SoloExecutionRuleConfig.OnEnqueueBuilder<SideEffect>,
    SoloExecutionRuleConfig.OnExecuteBuilder<SideEffect>,
    SoloExecutionRuleConfig.OnCancelBuilder<SideEffect> {

    val cancelOnSignalFilters = mutableListOf<InstanceFilter<Any, *>>()
    val executionSideEffectListeners = mutableListOf<(ExecutingSideEffect<SideEffect>) -> Unit>()

    var isSkipped = false
        private set

    private val ruleActions = mutableListOf<suspend () -> Unit>()

    override fun skipIfAlreadyExecuting(filter: InstanceFilter<ExecutingSideEffect<SideEffect>, *>) {
        ruleActions.add {
            isSkipped = executingSideEffectsStore.get().any(filter::accept)

            if (isSkipped) {
                cancelOnSignalFilters.clear()
                executionSideEffectListeners.clear()
            }
        }
    }

    override fun delay(duration: Duration) {
        ruleActions.add {
            kotlinx.coroutines.delay(duration.inWholeMilliseconds)
        }
    }

    override fun awaitComplete(
        filter: InstanceFilter<ExecutingSideEffect<SideEffect>, *>,
        failOnError: Boolean,
        requireSuccess: Boolean,
    ) {
        ruleActions.add {
            executingSideEffectsStore.get().filter(filter::accept).forEach { sideEffect ->
                (sideEffect as InternalExecutingSideEffect)
                    .isActiveFlow
                    .filterNot { it }
                    .firstOrNull()
            }
        }
    }

    override fun registerCancelOnSignal(signalFilter: InstanceFilter<Any, *>) {
        ruleActions.add {
            cancelOnSignalFilters.add(signalFilter)
        }
    }

    override fun sendSignal(signal: Any) {
        ruleActions.add {
            signalFlow.emit(signal)
        }
    }

    override fun cancelOther(sideEffectsFilter: InstanceFilter<ExecutingSideEffect<SideEffect>, *>) {
        ruleActions.add {
            executingSideEffectsStore.get().filter(sideEffectsFilter::accept).forEach { sideEffect ->
                (sideEffect as InternalExecutingSideEffect)
                    .cancelInternal()
            }
        }
    }

    override fun <S : SideEffect> registerListener(
        filter: InstanceFilter<ExecutingSideEffect<SideEffect>, ExecutingSideEffect<S>>,
        listener: (ExecutingSideEffect<S>) -> Unit
    ) {
        ruleActions.add {
            executionSideEffectListeners.add { sideEffect ->
                if (filter.accept(sideEffect)) {
                    listener.invoke(filter.get(sideEffect))
                }
            }
        }
    }

    override fun <S : SideEffect> getExecutingSideEffects(
        filter: InstanceFilter<ExecutingSideEffect<SideEffect>, ExecutingSideEffect<S>>,
        handler: (List<ExecutingSideEffect<S>>) -> Unit,
    ) {
        ruleActions.add {
            while (true) {
                val source = executingSideEffectsStore.get()
                val filteredResult = source.mapNotNull { executingSideEffect ->
                    executingSideEffect.takeIf { it !== executionSideEffect && filter.accept(it) }
                        ?.let(filter::get)
                }

                if (source === executingSideEffectsStore.get()) {
                    handler.invoke(filteredResult)
                    break
                }
            }
        }
    }

    suspend fun execute() {
        ruleActions.asSequence()
            .takeWhile { !isSkipped }
            .forEach { action -> action.invoke() }
    }
}