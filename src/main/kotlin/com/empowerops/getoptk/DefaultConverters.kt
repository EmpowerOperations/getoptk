package com.empowerops.getoptk

import java.lang.reflect.Method
import java.lang.reflect.Modifier
import kotlin.reflect.KClass
import kotlin.reflect.companionObject
import kotlin.reflect.companionObjectInstance
import java.lang.Enum as JavaEnum

//looks up strategies to convert strings to T's, eg "Double.parseDouble", "Boolean.parseBoolean", etc.
// please note this object returns a closed converter, which might be weird
// Could just as easily return a T instead of a (String) -> T
interface Converter<out T>{
    fun convert(text: String): T
}


object DefaultConverters {

    @Suppress("UNCHECKED_CAST", "IMPLICIT_CAST_TO_ANY") //unfortunately I'm moving from dynamic back into static types here.
            //AFAIK there is no way to tell kotlin that if type == Int::class, then T == Int
    operator fun <T: Any> get(type: KClass<T>): Converter<T>? = when {

        type.hasStaticMethod("valueOf", returnType = type, paramTypes = listOf(String::class)) ->
            StaticMethodCallConverter(type, "valueOf")

        type.companionObject?.hasLocalMethod("valueOf", returnType = type, paramTypes = listOf(String::class)) ?: false ->
            CompanionMethodCallConverter(type, "valueOf")

        type.hasStaticMethod("parse", returnType = type, paramTypes = listOf(String::class)) ->
            StaticMethodCallConverter(type, "parse")

        type.companionObject?.hasLocalMethod("parse", returnType = type, paramTypes = listOf(String::class)) ?: false ->
            CompanionMethodCallConverter(type, "parse")

        type == String::class -> StringConverter
        type == Int::class -> IntConverter
        type == Long::class -> LongConverter
        type == Float::class -> FloatConverter
        type == Double::class -> DoubleConverter
        type == Char::class -> CharConverter
        type.java.isEnum -> EnumConverter(type.java as Class<Nothing>)
        //Nothing is a bottom type, only way I could think to satisfy the Enum<Enum<Enum...>>> problem
        else -> null
    } as Converter<T>?
}

object InvalidConverter: Converter<Nothing>{
    override fun convert(text: String) = throw UnsupportedOperationException("not implemented")
}
object DoubleConverter: DelegatingConverter<Double>(String::toDouble), Primative
object FloatConverter: DelegatingConverter<Float>(String::toFloat), Primative
object IntConverter: DelegatingConverter<Int>(String::toInt), Primative
object LongConverter: DelegatingConverter<Long>(String::toLong), Primative
object StringConverter: Converter<String>{ override fun convert(text: String) = text }
object CharConverter: Converter<Char>{
    override fun convert(text: String): Char {
        require(text.length == 1){ "char variables must be exactly 1 character" }
        return text[0]
    }
}
class EnumConverter<T: Enum<T>>(val enumType: Class<T>): Converter<T>{
    override fun convert(text: String): T {
        val generatedEnum = JavaEnum.valueOf(enumType, text)
        return enumType.cast(generatedEnum)
    }
}

class StaticMethodCallConverter<T: Any>(val type: KClass<T>, methodName: String) : Converter<T>{

    val method = type.java.getMethod(methodName, String::class.java).apply {
        require(Modifier.isStatic(modifiers))
    }

    override fun convert(text: String) = type.java.cast(method.invoke(null, text))
}

class CompanionMethodCallConverter<T: Any>(val type: KClass<T>, methodName: String): Converter<T>{

    init {
        require(type.companionObject != null)
    }

    val companionInstance = type.companionObjectInstance!!
    val method = type.companionObject!!.getMethod(methodName, listOf(String::class))!!

    override fun convert(text: String): T = type.java.cast(method.invoke(companionInstance, text))
}

interface Primative {}
abstract class DelegatingConverter<T>(val convertActual: (String) -> T): Converter<T>{
    override fun convert(text: String): T = convertActual(text)
}

fun KClass<*>.hasStaticMethod(name: String, returnType: KClass<*>, paramTypes: List<KClass<*>>)
        = getMethod(name, paramTypes)?.let { it.isStatic && it.returnType == returnType.java } ?: false

private fun KClass<*>.getMethod(name: String, paramTypes: List<KClass<*>>)
        = try { this.java.getMethod(name, *paramTypes.map { it.java }.toTypedArray()) }
          catch (ex: NoSuchMethodException) { null }

val Method.isStatic: Boolean get() = Modifier.isStatic(modifiers)

fun KClass<*>.hasLocalMethod(name: String, returnType: KClass<*>, paramTypes: List<KClass<*>>)
        = getMethod(name, paramTypes)?.let { it.returnType == returnType.java }

class ErrorHandlingConverter<T: Any>(
        val errorReporter: ParseErrorReporter,
        val type: KClass<T>,
        val converter: Converter<Any>
){
    fun convert(listItem: Token): Pair<Boolean, T?> {
        return try {
            val parsedValue: Any = converter.convert(listItem.text)
            when(converter){
                is Primative -> true to parsedValue as T //avoid boxing/unboxing issues
                else -> true to type.java.cast(parsedValue)
            }

        }
        catch(ex: Exception) {
            errorReporter.reportParsingProblem(listItem, "expected ${type.simpleName}", ex)
            false to null
        }
    }
}
