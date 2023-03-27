package dev.sunnyday.arch.mvi.test

import java.lang.reflect.Proxy
import kotlin.reflect.KClass

inline fun <reified T : Any> stub(delegate: T? = null): T {
    return stubByClass(T::class, delegate)
}

@Suppress("UNCHECKED_CAST")
fun <T : Any> stubByClass(klass: KClass<T>, delegate: T? = null): T {
    val objectDelegate = Any()

    val nonDelegetableMethods = listOf("equals", "hashCode")

    return Proxy.newProxyInstance(klass.java.classLoader, arrayOf(klass.java)) { proxy, method, args ->
        if (method.name == "equals" && args?.getOrNull(0) === proxy) {
            return@newProxyInstance true
        }

        val target = if (delegate != null && method.name !in nonDelegetableMethods) {
            delegate
        } else {
            objectDelegate
        }

        if (args != null) {
            method.invoke(target, *args)
        } else {
            method.invoke(target)
        }
    } as T
}