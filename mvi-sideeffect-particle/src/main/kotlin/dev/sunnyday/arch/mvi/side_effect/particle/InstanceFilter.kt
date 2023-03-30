package dev.sunnyday.arch.mvi.side_effect.particle

interface InstanceFilter<in T, out R> {

    fun accept(value: T): Boolean

    fun get(value: T): R
}

