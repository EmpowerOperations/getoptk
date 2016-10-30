package com.empowerops.getoptk

import kotlin.reflect.KClass

//looks up strategies to convert strings to T's, eg "Double.parseDouble", "Boolean.parseBoolean", etc.
// please note this object returns a closed converter, which might be weird
// Could just as easily return a T instead of a (String) -> T
interface Converter<out T>{
    fun convert(text: String): T
}

class Converters(val errorReporter: ErrorReporter) {

    fun <T : Any> getDefaultFor(type: KClass<T>): Converter<T> = when{
        type == String::class -> DefaultConverter(type) { it }
        type == Int::class -> DefaultConverter(type, String::toInt)
        type == Long::class -> DefaultConverter(type, String::toLong)
        type == Char::class -> DefaultConverter(type) { text ->
            require(text.length == 1){ "char variables must be exactly 1 character" };
            return@DefaultConverter text[0]
        }
        type.java.isEnum -> {
            TODO()
        }
        else -> TODO()
    }

    inner class DefaultConverter<T: Any>(val type: KClass<T>, val parser: (String) -> Any): Converter<T>{
        override fun convert(text: String): T {
            return try {
                val parsedValue: Any = parser(text)
                type.java.cast(parsedValue)
            }
            catch(e: Exception) {
                errorReporter.reportProblem(TODO(), TODO(), "failed to parse '$text': ${e.message}")
                CONVERT_FAILED as T //use heap pollution? really??
            }
        }
    }
}