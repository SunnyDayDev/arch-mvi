package dev.sunnyday.arch.mvi.factory

class MviFactoryCallContext {

    private val elements = mutableMapOf<Key<*>, Element>()

    private var parent: MviFactoryCallContext? = null

    operator fun <T : Element> get(key: Key<T>): T? {
        var context: MviFactoryCallContext? = this

        while (context != null) {
            context.elements[key]?.get(key)?.let { return it }
            context = context.parent
        }

        return null
    }

    fun add(element: Element) {
        this[element.key] = element
    }

    operator fun <E: Element> set(key: Key<out E>, value: E) {
        elements[key] = value
    }

    fun <T> runWithFactoryContext(action: () -> T): T {
        val activeContexts = THREAD_LOCAL_FACTORY_CONTEXT.get() ?: mutableListOf()

        val currentTopContext = activeContexts.lastOrNull()

        if (currentTopContext === this) {
            return action.invoke()
        }

        if (activeContexts.isEmpty()) {
            THREAD_LOCAL_FACTORY_CONTEXT.set(activeContexts)
        }

        parent = currentTopContext
        activeContexts.add(this)

        val value = action.invoke()

        parent = null
        activeContexts.removeLast()

        if (activeContexts.isEmpty()) {
            THREAD_LOCAL_FACTORY_CONTEXT.remove()
        }

        return value
    }

    override fun hashCode(): Int {
        return elements.hashCode() * 31 +
                parent.hashCode() * 31
    }

    override fun equals(other: Any?): Boolean {
        if (other !is MviFactoryCallContext) return false

        return elements == other.elements &&
                parent == other.parent
    }

    fun clone(): MviFactoryCallContext {
        return MviFactoryCallContext().also { clone ->
            clone.elements.putAll(elements)
            clone.parent = parent?.clone()
        }
    }

    override fun toString(): String {
        val elementsToString = elements.values.joinToString()
        return if (parent != null) {
            "${parent.toString()} <- $elementsToString"
        } else {
            elementsToString
        }
    }

    interface Key<E : Element>

    interface Element {

        val key: Key<*>

        operator fun <E : Element> get(key: Key<E>): E? =
            @Suppress("UNCHECKED_CAST")
            if (this.key == key) this as E else null
    }

    companion object {

        private val THREAD_LOCAL_FACTORY_CONTEXT = ThreadLocal<MutableList<MviFactoryCallContext>>()

        fun create(vararg elements: Element): MviFactoryCallContext {
            return MviFactoryCallContext().apply {
                elements.forEach(::add)
            }
        }

        @JvmStatic
        fun getCurrentFactoryContext(): MviFactoryCallContext? {
            return THREAD_LOCAL_FACTORY_CONTEXT.get()?.lastOrNull()
        }

        @JvmStatic
        fun requireCurrentFactoryContext(): MviFactoryCallContext {
            return requireNotNull(getCurrentFactoryContext()) { "Call made out of FactoryContext.runWithFactoryContext" }
        }
    }
}