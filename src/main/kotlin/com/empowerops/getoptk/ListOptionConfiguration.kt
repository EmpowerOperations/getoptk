package com.empowerops.getoptk

import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KClass

interface ListOptionConfiguration<E: Any>: ReadOnlyProperty<CLI, List<E>>{

    var description: String
    var shortName: String
    var longName: String

    var parseMode: ListSpreadMode<E>

}

sealed class ListSpreadMode<out T>

// --things frodo 9000 sam 9001
// where `things` is List<Hobbit>, data class Hobbit(val name: String, val powerLevel: Int)
data class ImplicitObjects<T>(var converters: Map<KClass<*>, Converter<*>> = emptyMap()): ListSpreadMode<T>(){
    
    operator fun <U: Any> plus(converterByType: Pair<KClass<U>, Converter<U>>): ImplicitObjects<T>
            = ImplicitObjects(converters + converterByType)
    inline operator fun <reified U: Any> plus (noinline converter: Converter<U>): ImplicitObjects<T>
            = plus(U::class to converter)
}

// --things x,y,z
inline fun <reified T: Any> csv(noinline converter: Converter<T> = DefaultConverters[T::class] ?: InvalidConverter) = CSV(converter)
data class CSV<T>(val elementConverter: Converter<T>): ListSpreadMode<T>()

// --things x y z
inline fun <reified T: Any> varargs(noinline converter: Converter<T> = DefaultConverters[T::class] ?: InvalidConverter) = Varargs(converter)
data class Varargs<T>(val elementConverter: Converter<T>) : ListSpreadMode<T>()

//indicate that you want to use a custom regex to split the list
fun regex(regex: Regex, captureGroupName: String = "item"): ListSpreadMode<Nothing> = TODO()
