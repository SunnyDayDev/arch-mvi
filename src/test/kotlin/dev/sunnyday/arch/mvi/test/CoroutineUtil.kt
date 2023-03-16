package dev.sunnyday.arch.mvi.test

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow

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