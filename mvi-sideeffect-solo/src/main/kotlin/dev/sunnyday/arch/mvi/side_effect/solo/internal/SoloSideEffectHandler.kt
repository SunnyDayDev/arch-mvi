package dev.sunnyday.arch.mvi.side_effect.solo.internal

import dev.sunnyday.arch.mvi.SideEffectHandler
import dev.sunnyday.arch.mvi.primitive.ObservableEvent
import dev.sunnyday.arch.mvi.coroutine.ktx.toObservable
import dev.sunnyday.arch.mvi.side_effect.solo.ExecutingSideEffect
import dev.sunnyday.arch.mvi.side_effect.solo.SoloSideEffect
import dev.sunnyday.arch.mvi.side_effect.solo.util.AtomicStore
import dev.sunnyday.arch.mvi.side_effect.solo.util.JvmAtomicReferenceStore
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

@OptIn(ExperimentalCoroutinesApi::class)
class SoloSideEffectHandler<Dependencies : Any, SideEffect : SoloSideEffect<Dependencies, SideEffect, Event>, Event : Any>(
    private val dependencies: Dependencies,
    private val coroutineScope: CoroutineScope,
    private val sideEffectDispatcher: CoroutineDispatcher = Dispatchers.Default,
) : SideEffectHandler<SideEffect, Event> {

    private val sideEffectsFlow = MutableSharedFlow<SideEffect>()
    private val signalFlow = MutableSharedFlow<Any>()
    private val startedExecutingSideEffectsFlow = MutableSharedFlow<ExecutingSideEffect<SideEffect>>()

    private val executingSideEffectsStore: AtomicStore<Array<ExecutingSideEffect<SideEffect>>> =
        createSideEffectsStore()

    private val outputEventsFlow = sideEffectsFlow
        .flatMapMerge(transform = ::flatMapSideEffect)
        .shareIn(coroutineScope, SharingStarted.WhileSubscribed(), 0)

    override fun onSideEffect(sideEffect: SideEffect) {
        coroutineScope.launch(sideEffectDispatcher) {
            sideEffectsFlow.emit(sideEffect)
        }
    }

    override val outputEvents: ObservableEvent<Event>
        get() = outputEventsFlow
            .toObservable(coroutineScope)

    private suspend fun flatMapSideEffect(sideEffect: SideEffect): Flow<Event> {
        val rule = SoloExecutionRuleConfigInstance<SideEffect>()
        sideEffect.executionRule.run { rule.build() }

        val executingSideEffect = addExecutingSideEffect(sideEffect, rule)

        proceedRule(executingSideEffect, rule.onEnqueueRule)
        if (executingSideEffect.executionState == ExecutingSideEffect.ExecutionState.COMPLETED) {
            return emptyFlow()
        }

        val onExecuteRule = proceedRule(executingSideEffect, rule.onExecuteRule)
        if (executingSideEffect.executionState == ExecutingSideEffect.ExecutionState.COMPLETED) {
            return emptyFlow()
        }

        return sideEffect.execute(dependencies)
            .flowOn(rule.dispatcher ?: sideEffectDispatcher)
            .mergeWith(listenWhileExecuting(onExecuteRule))
            .onStart {
                executingSideEffect.executionState = ExecutingSideEffect.ExecutionState.EXECUTING
                startedExecutingSideEffectsFlow.emit(executingSideEffect)
            }
            .onCompletion {
                transformExecutingSideEffects { executingSideEffects ->
                    executingSideEffects - executingSideEffect
                }
                executingSideEffect.executionState = ExecutingSideEffect.ExecutionState.COMPLETED
                executingSideEffect.cancelInternal()
            }
            .takeUntil(getCancelSignal(executingSideEffect, rule, onExecuteRule))
    }

    private fun addExecutingSideEffect(
        sideEffect: SideEffect,
        rule: SoloExecutionRuleConfigInstance<SideEffect>,
    ): InternalExecutingSideEffect<SideEffect> {
        val executingSideEffect = InternalExecutingSideEffect(
            id = rule.sideEffectId,
            sideEffect = sideEffect,
            coroutineScope = coroutineScope,
        )

        transformExecutingSideEffects { executingSideEffects ->
            executingSideEffects + executingSideEffect
        }

        return executingSideEffect
    }

    private inline fun transformExecutingSideEffects(
        transform: (Array<ExecutingSideEffect<SideEffect>>) -> Array<ExecutingSideEffect<SideEffect>>
    ) {
        while (true) {
            val current = executingSideEffectsStore.get()
            val transformed = transform.invoke(current)
            if (executingSideEffectsStore.compareAndSet(current, transformed)) {
                return
            }
        }
    }

    private suspend fun proceedRule(
        sideEffect: InternalExecutingSideEffect<SideEffect>,
        rule: (OnAnyRuleBuilderInstance<SideEffect>.() -> Unit)?,
    ): OnAnyRuleBuilderInstance<SideEffect> {
        val onEnqueueRule = OnAnyRuleBuilderInstance(sideEffect, executingSideEffectsStore, signalFlow)
        rule?.let { configure -> onEnqueueRule.configure() }

        onEnqueueRule.execute()

        if (onEnqueueRule.isSkipped) {
            sideEffect.executionState = ExecutingSideEffect.ExecutionState.COMPLETED
        }

        return onEnqueueRule
    }

    private fun getCancelSignal(
        sideEffect: InternalExecutingSideEffect<SideEffect>,
        rulesConfig: SoloExecutionRuleConfigInstance<SideEffect>,
        onExecuteRule: OnAnyRuleBuilderInstance<SideEffect>,
    ): Flow<Any> {
        val cancelSignalFlow = onExecuteRule.cancelOnSignalFilters
            .takeIf { it.isNotEmpty() }
            ?.toList()
            ?.let { filters ->
                signalFlow.filter { signal ->
                    filters.any { filter -> filter.accept(signal) }
                }
            }
            ?: emptyFlow()

        return merge(
            sideEffect.isActiveFlow.filterNot { isActive -> isActive },
            cancelSignalFlow,
        )
            .take(1)
            .onEach {
                val onEnqueueRule = OnAnyRuleBuilderInstance(sideEffect, executingSideEffectsStore, signalFlow)
                rulesConfig.onCancelRule?.let { configure -> onEnqueueRule.configure() }
                onEnqueueRule.execute()
            }
    }

    private fun <T> listenWhileExecuting(rule: OnAnyRuleBuilderInstance<SideEffect>): Flow<T> {
        val listenerFilters = rule.executionSideEffectListeners
            .takeIf { it.isNotEmpty() }
            ?.toList()
            ?: return emptyFlow()

        return flow {
            startedExecutingSideEffectsFlow.collect { sideEffect ->
                listenerFilters.forEach { filter ->
                    filter.invoke(sideEffect)
                }
            }
        }
    }

    private inline operator fun <reified T> Array<T>.minus(element: T): Array<T> {
        val source = this
        var isRemoved = false

        return Array(size - 1) { index ->
            if (element === source[index]) {
                isRemoved = true
            }

            source[if (isRemoved) index + 1 else index]
        }
    }

    private fun <T> Flow<T>.takeUntil(signalFlow: Flow<Any?>): Flow<T> {
        val original = this

        return signalFlow
            .take(1)
            .map { emptyFlow<T>() }
            .onStart { emit(original) }
            .flatMapLatest { flow -> flow }
    }

    private fun <T> Flow<T>.mergeWith(other: Flow<T>): Flow<T> {
        return merge(this, other)
    }

    internal companion object {

        fun <SideEffect : Any> createSideEffectsStore(): AtomicStore<Array<ExecutingSideEffect<SideEffect>>> {
            return JvmAtomicReferenceStore(emptyArray())
        }
    }
}