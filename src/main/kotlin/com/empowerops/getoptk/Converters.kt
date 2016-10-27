package com.empowerops.getoptk

import kotlin.reflect.KClass

//looks up strategies to convert strings to T's, eg "Double.parseDouble", "Boolean.parseBoolean", etc.
// please note this object returns a closed converter, which might be weird
// Could just as easily return a T instead of a (String) -> T
interface Converter<out T>{
    fun convert(text: String): T
}

object Converters {
    fun <T : Any> getDefaultFor(type: KClass<T>): Converter<T> = when(type){
        String::class -> type.wrap { it }
        Int::class -> type.wrap(String::toInt)
        Long::class -> type.wrap(String::toLong)
        Char::class -> type.wrap { require(it.length == 1); it[0] }
        else -> TODO()
    }

    fun <T: Any> KClass<T>.wrap(parser: (String) -> Any): Converter<T> {
        return object : Converter<T> {
            val type = this@wrap
            override fun convert(text: String): T {
                return try { type.java.cast(parser(text)) } catch(e: Exception) { TODO() }
            }
        }
    }
}