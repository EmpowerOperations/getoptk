package com.empowerops.getoptk

import kotlin.reflect.KProperty

object Inferred {

    fun generateInferredDescription(prop: KProperty<*>) = "[description of $prop]"

    fun generateInferredShortName(prop: KProperty<*>) = prop.name[0].toString()
    fun generateInferredLongName(prop: KProperty<*>) = prop.name
}