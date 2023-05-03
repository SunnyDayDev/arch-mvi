package dev.sunnyday.arch.mvi.side_effect.solo

import dev.sunnyday.arch.mvi.coroutine.ktx.toFlow
import dev.sunnyday.arch.mvi.side_effect.solo.util.JvmAtomicReferenceStore
import dev.sunnyday.arch.mvi.test.*
import io.mockk.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.*
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.util.concurrent.TimeUnit
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds

@Timeout(10, unit = TimeUnit.SECONDS)
@OptIn(ExperimentalCoroutinesApi::class, ExperimentalStdlibApi::class)
class SoloSideEffectHandlerTest {

    private val dependencies = mockk<TestDependencies>()

    private val executingSideEffectsStore = spyk(
        JvmAtomicReferenceStore<Array<ExecutingSideEffect<TestSideEffect>>>(
            emptyArray(),
        ),
    )

    @BeforeEach
    fun setUp() {
        mockkObject(SoloSideEffectHandler)
        every { SoloSideEffectHandler.createSideEffectsStore<TestSideEffect>() } returns executingSideEffectsStore
    }

    @AfterEach
    fun tearDown() {
        unmockkObject(SoloSideEffectHandler)
    }

    @Test
    fun `handler properly tracks side effect statuses`() = runTest {
        val handler = createSoloSideEffectHandler()
        advanceUntilIdle()

        val sideEffect: TestSideEffect = object : TestSideEffect() {
            override val executionRule = executionRule<TestSideEffect> {
                onEnqueue {
                    delay(TEST_DELAY.milliseconds)
                }
            }

            override fun execute(dependency: TestDependencies): Flow<Event> = flow {
                delay(TEST_DELAY.milliseconds)
            }
        }

        handler.onSideEffect(sideEffect)
        runCurrent()

        val executingSideEffect = executingSideEffectsStore.get().singleOrNull()
        assertNotNull(executingSideEffect)
        assertEquals(ExecutingSideEffect.ExecutionState.ENQUEUED, executingSideEffect.executionState)

        advanceTimeBy(TEST_DELAY - 1)

        assertEquals(ExecutingSideEffect.ExecutionState.ENQUEUED, executingSideEffect.executionState)

        advanceTimeBy(1)
        runCurrent()

        assertEquals(ExecutingSideEffect.ExecutionState.EXECUTING, executingSideEffect.executionState)

        advanceUntilIdle()

        assertEquals(ExecutingSideEffect.ExecutionState.COMPLETED, executingSideEffect.executionState)
        assertTrue(executingSideEffectsStore.get().isEmpty())
    }

    @Test
    fun `rule id is sideEffect id`() = runUnconfinedTest {
        val expectedId = ExecutingSideEffect.Id.Custom("custom")
        val sideEffect: TestSideEffect = object : TestSideEffect() {
            override val executionRule = executionRule<TestSideEffect> {
                setId(expectedId)
            }

            override fun execute(dependency: TestDependencies): Flow<Event> = MutableSharedFlow()
        }

        val handler = createSoloSideEffectHandler()
        handler.onSideEffect(sideEffect)

        val executingSideEffects = executingSideEffectsStore.get()

        assertEquals(1, executingSideEffects.size)
        assertEquals(expectedId, executingSideEffects.single().id)
        assertEquals(sideEffect, executingSideEffects.single().sideEffect)
    }

    @Test
    fun `delay on enqueue delays side effect execution`() = runTest {
        val delayDuration = 100.milliseconds
        val flow = spyk(emptyFlow<Event>())

        val sideEffect = object : TestSideEffect() {
            override val executionRule = executionRule<TestSideEffect> {
                onEnqueue {
                    delay(delayDuration)
                }
            }

            override fun execute(dependency: TestDependencies): Flow<Event> = flow
        }

        val handler = createSoloSideEffectHandler()
        advanceUntilIdle()

        handler.onSideEffect(sideEffect)
        advanceTimeBy(delayDuration.inWholeMilliseconds - 1)
        runCurrent()

        confirmVerified(flow)

        advanceTimeBy(1)
        runCurrent()
        coVerify { flow.collect(any()) }
    }

    @Test
    fun `await complete on enqueue delays execution until specified sideeffects complete`() = runUnconfinedTest {
        val root = TestSideEffect()

        val dependent = object : TestSideEffect() {

            override val executionRule = executionRule<TestSideEffect> {
                onEnqueue {
                    awaitComplete(InstanceFilter.Filter { it.sideEffect === root })
                }
            }
        }

        val handler = createSoloSideEffectHandler()
        handler.onSideEffect(root)
        handler.onSideEffect(dependent)

        assertEquals(TestSideEffect.State.UNEXECUTED, dependent.state)

        root.complete()

        assertEquals(TestSideEffect.State.EXECUTING, dependent.state)
    }

    @Test
    fun `on enqueue, skip if already executing specified sideeffect`() = runUnconfinedTest {
        val signalSideEffectCompletionChannel = Channel<Unit>()
        val targetSideEffect = object : TestSideEffect() {

            override fun execute(dependency: TestDependencies): Flow<Event> =
                flow { signalSideEffectCompletionChannel.receive() }
        }

        val dependentFlow = spyk(emptyFlow<Event>())
        val dependendSideEffect = object : TestSideEffect() {

            override val executionRule = executionRule<TestSideEffect> {
                onEnqueue { skipIfAlreadyExecuting(InstanceFilter.Filter { it.sideEffect === targetSideEffect }) }
            }

            override fun execute(dependency: TestDependencies): Flow<Event> = dependentFlow
        }

        val handler = createSoloSideEffectHandler()

        handler.onSideEffect(targetSideEffect)
        handler.onSideEffect(dependendSideEffect)
        signalSideEffectCompletionChannel.send(Unit)

        confirmVerified(dependentFlow)
    }

    @Test
    fun `on enqueue, don't skip if already executing sideeffect isn't present`() = runUnconfinedTest {
        val dependentFlow = spyk(emptyFlow<Event>())
        val dependendSideEffect = object : TestSideEffect() {

            override val executionRule = executionRule<TestSideEffect> {
                onEnqueue { skipIfAlreadyExecuting(InstanceFilter.Filter { false }) }
            }

            override fun execute(dependency: TestDependencies): Flow<Event> = dependentFlow
        }

        val handler = createSoloSideEffectHandler()

        handler.onSideEffect(dependendSideEffect)

        coVerify { dependentFlow.collect(any()) }
    }

    @Test
    fun `on execute, skip if already executing specified sideeffect`() = runUnconfinedTest {
        val signalSideEffectCompletionChannel = Channel<Unit>()
        val targetSideEffect = object : TestSideEffect() {

            override fun execute(dependency: TestDependencies): Flow<Event> =
                flow { signalSideEffectCompletionChannel.receive() }
        }

        val dependentFlow = spyk(emptyFlow<Event>())
        val dependendSideEffect = object : TestSideEffect() {

            override val executionRule = executionRule<TestSideEffect> {
                onExecute { skipIfAlreadyExecuting(InstanceFilter.Filter { it.sideEffect === targetSideEffect }) }
            }

            override fun execute(dependency: TestDependencies): Flow<Event> = dependentFlow
        }

        val handler = createSoloSideEffectHandler()

        handler.onSideEffect(targetSideEffect)
        handler.onSideEffect(dependendSideEffect)
        signalSideEffectCompletionChannel.send(Unit)

        confirmVerified(dependentFlow)
    }

    @Test
    fun `on execute, don't skip if already executing sideeffect isn't present`() = runUnconfinedTest {
        val dependentFlow = spyk(emptyFlow<Event>())
        val dependendSideEffect = object : TestSideEffect() {

            override val executionRule = executionRule<TestSideEffect> {
                onExecute { skipIfAlreadyExecuting(InstanceFilter.Filter { false }) }
            }

            override fun execute(dependency: TestDependencies): Flow<Event> = dependentFlow
        }

        val handler = createSoloSideEffectHandler()

        handler.onSideEffect(dependendSideEffect)

        coVerify { dependentFlow.collect(any()) }
    }

    @ParameterizedTest
    @MethodSource("provideCancelOtherRules")
    fun `cancelOther(_) cancels other side effect`(targetSideEffectRuleFactory: TargetSideEffectRuleFactory) =
        runUnconfinedTest {
            val targetSideEffect = TestSideEffect()

            val sideEffect = object : TestSideEffect() {
                override val executionRule = targetSideEffectRuleFactory.createRuleForTarget(targetSideEffect)
            }

            val handler = createSoloSideEffectHandler()

            handler.onSideEffect(targetSideEffect)

            assertEquals(TestSideEffect.State.EXECUTING, targetSideEffect.state)

            handler.onSideEffect(sideEffect)

            assertEquals(TestSideEffect.State.CANCELLED, targetSideEffect.state)
        }

    private suspend fun TestScope.createSoloSideEffectHandler(): SoloSideEffectHandler<TestDependencies, TestSideEffect, Event> {
        val currentCoroutineContext = currentCoroutineContext()

        return SoloSideEffectHandler<TestDependencies, TestSideEffect, Event>(
            dependencies = dependencies,
            coroutineScope = this,
            dispatcher = requireNotNull(currentCoroutineContext[CoroutineDispatcher]),
        ).also { it.outputEvents.toFlow().collectWithScope() }
    }

    interface TestDependencies

    open class TestSideEffect : SoloSideEffect<TestDependencies, TestSideEffect, Event> {

        var state: State = State.UNEXECUTED
            private set

        private val eventChannel = Channel<Event>()

        suspend fun send(event: Event) {
            eventChannel.send(event)
        }

        fun complete() {
            eventChannel.close()
        }

        override fun execute(dependency: TestDependencies): Flow<Event> = flow {
            for (event in eventChannel) {
                emit(event)
            }
        }
            .onStart { state = State.EXECUTING }
            .onCompletion { state = if (it != null) State.CANCELLED else State.COMPLETED }

        enum class State {
            UNEXECUTED,
            EXECUTING,
            COMPLETED,
            CANCELLED,
        }
    }

    fun interface TargetSideEffectRuleFactory {

        fun createRuleForTarget(sideEffect: TestSideEffect): SoloExecutionRule<TestSideEffect>
    }

    class NamedTargetSideEffectRuleFactory(
        private val name: String,
        ruleFactory: TargetSideEffectRuleFactory,
    ) : TargetSideEffectRuleFactory by ruleFactory {

        override fun toString(): String = name
    }

    private companion object {

        const val TEST_DELAY = 100L

        @JvmStatic
        fun provideCancelOtherRules(): List<TargetSideEffectRuleFactory> {
            return listOf(
                NamedTargetSideEffectRuleFactory("onEnqueue") { target ->
                    executionRule {
                        onEnqueue {
                            cancelOther(InstanceFilter.Filter { it.sideEffect === target })
                        }
                    }
                },
                NamedTargetSideEffectRuleFactory("onExecute") { target ->
                    executionRule {
                        onExecute {
                            cancelOther(InstanceFilter.Filter { it.sideEffect === target })
                        }
                    }
                }
            )
        }
    }
}