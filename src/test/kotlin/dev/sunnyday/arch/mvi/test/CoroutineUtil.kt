package dev.sunnyday.arch.mvi.test

import dev.sunnyday.arch.mvi.internal.primitive.CoroutineObservable
import dev.sunnyday.arch.mvi.primitive.Observable
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

suspend fun <T : Any> Flow<T>.collectWithScope(): List<T> {
    val flow = this

    val testCoroutineContext = currentCoroutineContext()
    val parentJob = testCoroutineContext.job
    val collectScope = CoroutineScope(testCoroutineContext + SupervisorJob())

    val collector = mutableListOf<T>()

    val collectJob = collectScope.launch { flow.collect(collector::add) }
    collectScope.launch {
        parentJob.join()
        collectJob.cancel()
    }

    return collector
}

suspend fun <T : Any> Observable<T>.collectWithScope(): List<T> {
    if (this is CoroutineObservable<T>) {
        return flow.collectWithScope()
    }

    val testCoroutineContext = currentCoroutineContext()
    val parentJob = testCoroutineContext.job
    val collectScope = CoroutineScope(testCoroutineContext + SupervisorJob())

    val collector = mutableListOf<T>()

    val collectCancellable = observe(collector::add)
    collectScope.launch {
        parentJob.join()
        collectCancellable.cancel()
    }

    return collector
}

suspend fun createTestSubScope(): CoroutineScope {
    val testCoroutineContext = currentCoroutineContext()
    val testJob = testCoroutineContext.job
    val stateMachineCoroutineScope = CoroutineScope(testCoroutineContext + SupervisorJob())
    stateMachineCoroutineScope.launch {
        testJob.join()
        cancel()
    }

    return stateMachineCoroutineScope
}

@ExperimentalCoroutinesApi
fun runUnconfinedTest(
    context: CoroutineContext = EmptyCoroutineContext,
    dispatchTimeoutMs: Long = 60_000L,
    testBody: suspend TestScope.() -> Unit
) = runTest(context + UnconfinedTestDispatcher(), dispatchTimeoutMs, testBody)