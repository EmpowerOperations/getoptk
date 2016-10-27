package com.empowerops.getoptk

import kotlin.reflect.KClass

//looks up strategies to convert strings to T's, eg "Double.parseDouble", "Boolean.parseBoolean", etc.
// please note this object returns a closed parser, which might be weird
// Could just as easily return a T instead of a (String) -> T
object Parsers {
    fun <T : Any> getDefaultFor(type: KClass<T>): (String) -> T = when(type){
        String::class -> { it -> type.cast(it) }
        else -> TODO()
    }


    fun <T: Any> KClass<T>.cast(instance: Any) = java.cast(instance)
}