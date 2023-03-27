package dev.sunnyday.arch.mvi.test

import kotlin.reflect.KClass

interface MockProvider {

    fun <T : Any> getMock(name: String, klass: KClass<T>): T
}

inline fun <reified T : Any> MockProvider.getMock(name: String): T {
    return getMock(name, T::class)
}

class MockkStore : MockProvider {

    @PublishedApi
    internal val mockkStore = mutableMapOf<String, Any>()

    override fun <T : Any> getMock(name: String, klass: KClass<T>): T {
        @Suppress("UNCHECKED_CAST")
        return mockkStore.getOrPut(name) { mockkByClass(klass) } as T
    }
}

class StubProvider : MockProvider {

    override fun <T : Any> getMock(name: String, klass: KClass<T>): T {
        return stubByClass(klass)
    }
}