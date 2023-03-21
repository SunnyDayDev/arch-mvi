package dev.sunnyday.arch.mvi.test

import java.lang.reflect.Proxy

inline fun <reified T> createStub(): T {
    val objectDelegate = Any()

    return Proxy.newProxyInstance(T::class.java.classLoader, arrayOf(T::class.java)) { proxy, method, args ->

        if (method.name == "equals" && args?.getOrNull(0) === proxy) {
            return@newProxyInstance true
        }

        if (args != null) {
            method.invoke(objectDelegate, *args)
        } else {
            method.invoke(objectDelegate)
        }
    } as T
}

inline fun <reified T> createStub(delegate: T, delegateCalls: T.() -> Unit): T {
    val objectDelegate = Any()

    val delegateCallsList = buildList {
        val delegateProxy = Proxy.newProxyInstance(T::class.java.classLoader, arrayOf(T::class.java)) { _, method, _ ->
            add(method)
        } as T
        delegateCalls.invoke(delegateProxy)
    }

    return Proxy.newProxyInstance(T::class.java.classLoader, arrayOf(T::class.java)) { proxy, method, args ->
        val target = if (method in delegateCallsList) delegate else objectDelegate

        if (method.name == "equals" && args?.getOrNull(0) === proxy) {
            return@newProxyInstance true
        }

        if (args != null) {
            method.invoke(target, *args)
        } else {
            method.invoke(target)
        }
    } as T
}