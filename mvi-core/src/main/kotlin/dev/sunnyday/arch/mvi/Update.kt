package dev.sunnyday.arch.mvi

class Update<out State : Any, out SideEffect : Any> private constructor(
    val state: State?,
    val sideEffects: List<SideEffect>,
) {

    operator fun component1(): State? = state

    operator fun component2(): List<SideEffect> = sideEffects

    override fun hashCode(): Int {
        return (state?.hashCode() ?: 0) + 31 * sideEffects.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        if (other !is Update<*, *>) return false
        return state == other.state && sideEffects == other.sideEffects
    }

    override fun toString(): String {
        val description = buildString {
            if (state != null) {
                append("state: ")
                append(state)
            }

            if (sideEffects.isNotEmpty()) {
                if (isNotEmpty()) {
                    append(", ")
                }

                append("sideEffects: ")
                append(sideEffects.joinToString())
            }

            if (isEmpty()) {
                append("nothing")
            }
        }

        return "Update($description)"
    }

    companion object {

        @JvmStatic
        fun nothing(): Update<Nothing, Nothing> = Update(null, emptyList())

        @JvmStatic
        fun <S : Any> state(state: S): Update<S, Nothing> = Update(state, emptyList())

        @JvmStatic
        fun <SE : Any> sideEffects(sideEffects: List<SE>): Update<Nothing, SE> = Update(null, sideEffects)

        @JvmStatic
        fun <SE : Any> sideEffects(vararg sideEffects: SE): Update<Nothing, SE> = Update(null, sideEffects.asList())

        @JvmStatic
        fun <S : Any, SE : Any> stateWithSideEffects(
            state: S,
            sideEffects: List<SE>,
        ): Update<S, SE> = Update(state, sideEffects)

        @JvmStatic
        fun <S : Any, SE : Any> stateWithSideEffects(
            state: S,
            vararg sideEffects: SE,
        ): Update<S, SE> = Update(state, sideEffects.asList())
    }
}