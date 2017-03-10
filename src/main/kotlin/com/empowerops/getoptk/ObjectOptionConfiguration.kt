package com.empowerops.getoptk

import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KClass
import kotlin.reflect.KProperty

interface ObjectOptionConfiguration<T>: ReadOnlyProperty<CLI, T> {
    var description: String
    var shortName: String
    var longName: String

    fun <N: Any> registerConverter(type: KClass<N>, converter: Converter<N>): Unit
}

inline fun <reified N> ObjectOptionConfiguration<*>.registerConverter(converter: Converter<N>): Unit where N: Any
        = registerConverter(N::class, converter)