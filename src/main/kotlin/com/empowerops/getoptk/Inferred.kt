package com.empowerops.getoptk

import kotlin.reflect.KProperty

object Inferred {

    fun description(prop: KProperty<*>) = "[description of $prop]"

    fun shortName(prop: KProperty<*>) = prop.name[0].toString()
    fun longName(prop: KProperty<*>) = prop.name
}