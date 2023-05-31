package dev.sunnyday.arch.mvi.side_effect.solo

import dev.sunnyday.arch.mvi.SideEffectHandler
import dev.sunnyday.arch.mvi.primitive.ObservableEvent
import dev.sunnyday.arch.mvi.coroutine.ktx.toObservable
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
        val rule = SoloExecutionRuleConfigInstance()
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
        rule: SoloExecutionRuleConfigInstance,
    ): RealExecutingSideEffect<SideEffect> {
        val executingSideEffect = RealExecutingSideEffect(
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
        sideEffect: RealExecutingSideEffect<SideEffect>,
        rule: (OnAnyRuleBuilderInstance.() -> Unit)?,
    ): OnAnyRuleBuilderInstance {
        val onEnqueueRule = OnAnyRuleBuilderInstance(sideEffect)
        rule?.let { configure -> onEnqueueRule.configure() }

        onEnqueueRule.execute()

        if (onEnqueueRule.isSkipped) {
            sideEffect.executionState = ExecutingSideEffect.ExecutionState.COMPLETED
        }

        return onEnqueueRule
    }

    private fun getCancelSignal(
        sideEffect: RealExecutingSideEffect<SideEffect>,
        rulesConfig: SoloExecutionRuleConfigInstance,
        onExecuteRule: OnAnyRuleBuilderInstance,
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
                val onEnqueueRule = OnAnyRuleBuilderInstance(sideEffect)
                rulesConfig.onCancelRule?.let { configure -> onEnqueueRule.configure() }
                onEnqueueRule.execute()
            }
    }

    private fun <T> listenWhileExecuting(rule: OnAnyRuleBuilderInstance): Flow<T> {
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

    private inner class SoloExecutionRuleConfigInstance : SoloExecutionRuleConfig<SideEffect> {

        var sideEffectId: ExecutingSideEffect.Id = ExecutingSideEffect.Id.Unique()
        var dispatcher: CoroutineDispatcher? = null

        var onEnqueueRule: (OnEnqueueBuilder<SideEffect>.() -> Unit)? = null
        var onExecuteRule: (OnExecuteBuilder<SideEffect>.() -> Unit)? = null
        var onCancelRule: (OnCancelBuilder<SideEffect>.() -> Unit)? = null

        override fun setId(id: ExecutingSideEffect.Id): SoloExecutionRuleConfig<SideEffect> = apply {
            sideEffectId = id
        }

        override fun setDispatcher(dispatcher: CoroutineDispatcher): SoloExecutionRuleConfig<SideEffect> = apply {
            this.dispatcher = dispatcher
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

    private inner class OnAnyRuleBuilderInstance(
        private val executionSideEffect: ExecutingSideEffect<SideEffect>,
    ) : OnEnqueueBuilder<SideEffect>,
        OnExecuteBuilder<SideEffect>,
        OnCancelBuilder<SideEffect> {

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
                delay(duration.inWholeMilliseconds)
            }
        }

        override fun awaitComplete(
            filter: InstanceFilter<ExecutingSideEffect<SideEffect>, *>,
            failOnError: Boolean,
            requireSuccess: Boolean,
        ) {
            ruleActions.add {
                executingSideEffectsStore.get().filter(filter::accept).forEach { sideEffect ->
                    (sideEffect as RealExecutingSideEffect)
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
                    (sideEffect as RealExecutingSideEffect)
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

    private class RealExecutingSideEffect<SideEffect : SoloSideEffect<*, SideEffect, *>>(
        override val id: ExecutingSideEffect.Id,
        override val sideEffect: SideEffect,
        private val coroutineScope: CoroutineScope,
    ) : ExecutingSideEffect<SideEffect> {

        override var executionState: ExecutingSideEffect.ExecutionState = ExecutingSideEffect.ExecutionState.ENQUEUED

        val isActiveFlow: StateFlow<Boolean>
            get() = _isActiveFlow
        private val _isActiveFlow: MutableStateFlow<Boolean> = MutableStateFlow(true)

        override fun cancel() {
            coroutineScope.launch {
                cancelInternal()
            }
        }

        suspend fun cancelInternal() {
            _isActiveFlow.emit(false)
        }

        override fun toString(): String {
            return "ExecutingSideEffect($id)"
        }
    }

    internal companion object {

        fun <SideEffect : Any> createSideEffectsStore(): AtomicStore<Array<ExecutingSideEffect<SideEffect>>> {
            return JvmAtomicReferenceStore(emptyArray())
        }
    }
}