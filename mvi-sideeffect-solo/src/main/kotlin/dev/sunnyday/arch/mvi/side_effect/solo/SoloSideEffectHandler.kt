package dev.sunnyday.arch.mvi.side_effect.solo

import dev.sunnyday.arch.mvi.SideEffectHandler
import dev.sunnyday.arch.mvi.primitive.ObservableEvent
import dev.sunnyday.arch.mvi.coroutine.ktx.toObservable
import dev.sunnyday.arch.mvi.primitive.Cancellable
import dev.sunnyday.arch.mvi.side_effect.solo.SoloExecutionRuleConfig.*
import dev.sunnyday.arch.mvi.side_effect.solo.util.AtomicStore
import dev.sunnyday.arch.mvi.side_effect.solo.util.JvmAtomicReferenceStore
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlin.time.Duration

@OptIn(ExperimentalCoroutinesApi::class)
class SoloSideEffectHandler<Dependencies : Any, SideEffect : SoloSideEffect<Dependencies, SideEffect, Event>, Event : Any>(
    private val dependencies: Dependencies,
    private val coroutineScope: CoroutineScope,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default,
) : SideEffectHandler<SideEffect, Event> {

    private val sideEffectsFlow = MutableSharedFlow<SideEffect>()
    private val signalFlow = MutableSharedFlow<Any>()
    private val executingSideEffectsFlow = MutableSharedFlow<ExecutingSideEffect<SideEffect>>()

    private val executingSideEffectsStore: AtomicStore<Array<ExecutingSideEffect<SideEffect>>> =
        createSideEffectsStore()

    private val outputEventsFlow = sideEffectsFlow
        .flatMapMerge(transform = ::flatMapSideEffect)
        .shareIn(coroutineScope, SharingStarted.WhileSubscribed(), 0)

    override fun onSideEffect(sideEffect: SideEffect) {
        coroutineScope.launch {
            sideEffectsFlow.emit(sideEffect)
        }
    }

    override val outputEvents: ObservableEvent<Event>
        get() = outputEventsFlow
            .toObservable(coroutineScope)

    private suspend fun flatMapSideEffect(sideEffect: SideEffect): Flow<Event> {
        val rule = SideEffectRule()
        sideEffect.executionRule.run { rule.build() }

        val isActiveState = MutableStateFlow(true)

        val executingSideEffect = addExecutingSideEffect(sideEffect, rule, isActiveState)

        proceedRule(executingSideEffect, rule.onEnqueueRule)
        if (executingSideEffect.executionState == ExecutingSideEffect.ExecutionState.COMPLETED) {
            return emptyFlow()
        }

        val onExecuteRule = proceedRule(executingSideEffect, rule.onExecuteRule)
        if (executingSideEffect.executionState == ExecutingSideEffect.ExecutionState.COMPLETED) {
            return emptyFlow()
        }

        return sideEffect.execute(dependencies)
            .mergeWith(listenWhileExecuting(onExecuteRule))
            .onStart {
                executingSideEffect.executionState = ExecutingSideEffect.ExecutionState.EXECUTING
                executingSideEffectsFlow.emit(executingSideEffect)
            }
            .onCompletion {
                transformExecutingSideEffects { executingSideEffects ->
                    executingSideEffects - executingSideEffect
                }
                executingSideEffect.executionState = ExecutingSideEffect.ExecutionState.COMPLETED
                executingSideEffect.isActiveState.emit(false)
            }
            .takeUntil(getCancelSignal(executingSideEffect, rule, onExecuteRule))
    }

    private fun addExecutingSideEffect(
        sideEffect: SideEffect,
        rule: SideEffectRule,
        isActiveState: MutableStateFlow<Boolean>,
    ): ExecutingSideEffectImpl {
        val executingSideEffect = ExecutingSideEffectImpl(
            id = rule.sideEffectId,
            sideEffect = sideEffect,
            isActiveState = isActiveState,
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
        sideEffect: ExecutingSideEffectImpl,
        rule: (OnEnqueueRule.() -> Unit)?,
    ): OnEnqueueRule {
        val onEnqueueRule = OnEnqueueRule(sideEffect)
        rule?.let { execute -> onEnqueueRule.execute() }

        onEnqueueRule.actions.forEach { action ->
            action.invoke()
        }

        if (onEnqueueRule.isSkipped) {
            sideEffect.executionState = ExecutingSideEffect.ExecutionState.COMPLETED
        }

        return onEnqueueRule
    }

    private fun getCancelSignal(
        sideEffect: ExecutingSideEffectImpl,
        rulesConfig: SideEffectRule,
        onExecuteRule: OnEnqueueRule,
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
            sideEffect.isActiveState.filterNot { isActive -> isActive },
            cancelSignalFlow,
        )
            .take(1)
            .onEach {
                val onEnqueueRule = OnEnqueueRule(sideEffect)
                rulesConfig.onCancelRule?.let { execute -> onEnqueueRule.execute() }
                onEnqueueRule.actions.forEach { action -> action.invoke() }
            }
    }

    private fun <T> listenWhileExecuting(rule: OnEnqueueRule): Flow<T> {
        val listenerFilters = rule.listenerFilters
            .takeIf { it.isNotEmpty() }
            ?.toList()
            ?: return emptyFlow()

        return flow {
            executingSideEffectsFlow.collect { sideEffect ->
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

    private inner class ExecutingSideEffectImpl(
        override val id: ExecutingSideEffect.Id,
        override val sideEffect: SideEffect,
        val isActiveState: MutableStateFlow<Boolean>,
    ) : ExecutingSideEffect<SideEffect> {

        override var executionState: ExecutingSideEffect.ExecutionState = ExecutingSideEffect.ExecutionState.ENQUEUED

        override fun cancel() {
            coroutineScope.launch {
                cancelInternal()
            }
        }

        suspend fun cancelInternal() {
            isActiveState.emit(false)
        }

        override fun toString(): String {
            return "ExecutingSideEffect($id)"
        }
    }

    private inner class SideEffectRule : SoloExecutionRuleConfig<SideEffect> {

        var sideEffectId: ExecutingSideEffect.Id = ExecutingSideEffect.Id.Unique()

        var onEnqueueRule: (OnEnqueueBuilder<SideEffect>.() -> Unit)? = null
        var onExecuteRule: (OnExecuteBuilder<SideEffect>.() -> Unit)? = null
        var onCancelRule: (OnCancelBuilder<SideEffect>.() -> Unit)? = null

        override fun setId(id: ExecutingSideEffect.Id): SoloExecutionRuleConfig<SideEffect> = apply {
            sideEffectId = id
        }

        override fun onEnqueue(
            config: OnEnqueueBuilder<SideEffect>.() -> Unit,
        ) = apply {
            onEnqueueRule = config
        }

        override fun onExecute(
            config: OnExecuteBuilder<SideEffect>.() -> Unit,
        ): SoloExecutionRuleConfig<SideEffect> = apply {
            onExecuteRule = config
        }

        override fun onCancel(
            config: OnCancelBuilder<SideEffect>.() -> Unit,
        ): SoloExecutionRuleConfig<SideEffect> = apply {
            onCancelRule = config
        }
    }

    private inner class OnEnqueueRule(
        sideEffect: ExecutingSideEffect<SideEffect>,
    ) : BuilderRule(sideEffect),
        OnEnqueueBuilder<SideEffect>,
        OnExecuteBuilder<SideEffect>,
        OnCancelBuilder<SideEffect> {

        val actions = mutableListOf<suspend () -> Unit>()

        var isSkipped = false

        val cancelOnSignalFilters = mutableListOf<InstanceFilter<Any, *>>()
        val listenerFilters = mutableListOf<(ExecutingSideEffect<SideEffect>) -> Unit>()

        override fun skipIfAlreadyExecuting(filter: InstanceFilter<ExecutingSideEffect<SideEffect>, *>) {
            if (isSkipped) return
            isSkipped = executingSideEffectsStore.get().any(filter::accept)
        }

        override fun delay(duration: Duration) {
            actions.add {
                delay(duration.inWholeMilliseconds)
            }
        }

        override fun awaitComplete(
            filter: InstanceFilter<ExecutingSideEffect<SideEffect>, *>,
            failOnError: Boolean,
            requireSuccess: Boolean
        ) {
            actions.add {
                executingSideEffectsStore.get().filter(filter::accept).forEach { sideEffect ->
                    (sideEffect as SoloSideEffectHandler<*, *, *>.ExecutingSideEffectImpl)
                        .isActiveState
                        .filterNot { it }
                        .firstOrNull()
                }
            }
        }

        override fun registerCancelOnSignal(signalFilter: InstanceFilter<Any, *>) {
            cancelOnSignalFilters.add(signalFilter)
        }

        override fun sendSignal(signal: Any) {
            coroutineScope.launch {
                signalFlow.emit(signal)
            }
        }

        override fun cancelOther(sideEffectsFilter: InstanceFilter<ExecutingSideEffect<SideEffect>, *>) {
            actions.add {
                executingSideEffectsStore.get().filter(sideEffectsFilter::accept).forEach { sideEffect ->
                    (sideEffect as SoloSideEffectHandler<*, *, *>.ExecutingSideEffectImpl)
                        .cancelInternal()
                }
            }
        }

        override fun <S : SideEffect> registerListener(
            filter: InstanceFilter<ExecutingSideEffect<SideEffect>, ExecutingSideEffect<S>>,
            listener: (ExecutingSideEffect<S>) -> Unit
        ) {
            listenerFilters.add { sideEffect ->
                if (filter.accept(sideEffect)) {
                    listener.invoke(filter.get(sideEffect))
                }
            }
        }
    }

    private abstract inner class BuilderRule(
        protected val ruleExecutingSideEffect: ExecutingSideEffect<SideEffect>,
    ) : Builder<SideEffect> {

        override fun <S : SideEffect> getExecutingSideEffects(
            filter: InstanceFilter<ExecutingSideEffect<SideEffect>, ExecutingSideEffect<S>>
        ): List<ExecutingSideEffect<S>> {
            while (true) {
                val source = executingSideEffectsStore.get()
                val filteredResult = source.mapNotNull { executingSideEffect ->
                    executingSideEffect.takeIf { it !== ruleExecutingSideEffect && filter.accept(it) }
                        ?.let(filter::get)
                }

                if (source === executingSideEffectsStore.get()) {
                    return filteredResult
                }
            }
        }
    }

    internal companion object {

        fun <SideEffect : Any> createSideEffectsStore(): AtomicStore<Array<ExecutingSideEffect<SideEffect>>> {
            return JvmAtomicReferenceStore(emptyArray())
        }
    }
}