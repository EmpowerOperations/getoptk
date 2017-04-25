package com.empowerops.getoptk

import java.math.BigDecimal
import java.math.BigInteger
import java.nio.file.Path
import kotlin.reflect.KClass
import kotlin.reflect.KProperty

object Inferred {

    fun description(prop: KProperty<*>) = "[description of $prop]"

    fun shortName(prop: KProperty<*>) = prop.name[0].toString()
    fun longName(prop: KProperty<*>) = prop.name

    fun argumentType(type: KClass<*>) = when(type){
        Double::class, Float::class, BigDecimal::class -> "decimal"
        Int::class, Long::class, BigInteger::class -> "int"
        Path::class -> "path"
        else -> "value"
    }
}