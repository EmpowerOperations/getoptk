package com.empowerops.getoptk

import kotlin.reflect.KClass

//looks up strategies to convert strings to T's, eg "Double.parseDouble", "Boolean.parseBoolean", etc.
// please note this object returns a closed converter, which might be weird
// Could just as easily return a T instead of a (String) -> T
object Converter {
    fun <T : Any> getDefaultFor(type: KClass<T>): (String) -> T = when(type){
        String::class -> type.wrap { it }
        Int::class -> type.wrap(String::toInt)
        Long::class -> type.wrap(String::toLong)
        Char::class -> type.wrap { require(it.length == 1); it[0] }
        else -> TODO()
    }

    //for the record: no functional programmer worth his salt would call this "nice"

    fun <T: Any> KClass<T>.wrap(parser: (String) -> Any): (String) -> T
            = { it: String -> try { this.java.cast(parser(it)) } catch(e: Exception) { TODO() } }
}