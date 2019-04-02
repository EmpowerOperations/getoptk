package com.empowerops.getoptk

import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KClass
import kotlin.reflect.KProperty

interface CommandLineOption<out T>: ReadOnlyProperty<CLI, T>

interface ObjectOptionConfiguration<T>: ReadOnlyProperty<CLI, T> {
    var description: String
    var shortName: String
    var longName: String

    var isRequired: Boolean
    var default: T

    fun <N: Any> registerConverter(type: KClass<N>, converter: Converter<N>): Unit
}
inline fun <reified N> ObjectOptionConfiguration<*>.registerConverter(noinline converter: Converter<N>): Unit where N: Any
        = registerConverter(N::class, converter)


interface NullableObjectOptionConfiguration<T>: CommandLineOption<T?> {
    var description: String
    var shortName: String
    var longName: String

    var isRequired: Boolean
    var default: T?

    fun <N: Any> registerConverter(type: KClass<N>, converter: Converter<N?>): Unit
}
inline fun <reified N> NullableObjectOptionConfiguration<*>.registerConverter(noinline converter: Converter<N?>): Unit where N: Any
        = registerConverter(N::class, converter)


interface ValueOptionConfiguration<T>: CommandLineOption<T> {
    var converter: Converter<T>
    var shortName: String
    var longName: String
    var description: String

    var isRequired: Boolean
    var default: T
}

interface NullableValueOptionConfiguration<T: Any>: CommandLineOption<T?> {

    var converter: Converter<T?>
    var shortName: String
    var longName: String
    var description: String

    var isRequired: Boolean
    var default: T?
}

interface ListOptionConfiguration<E: Any>: CommandLineOption<List<E>>{

    var description: String
    var shortName: String
    var longName: String

    var parseMode: ListSpreadMode<E>
    var default: List<E>

}

interface SubcommandOptionConfiguration<out C: CLI>: CommandLineOption<C>{

    var description: String
    var shortName: String
    var longName: String

    var default: @UnsafeVariance C //TODO this isnt useful unless we can make CLI instances

    fun <T: Any> registerCommand(commandName: String, commandType: KClass<T>)
}
inline fun <reified T: Any> SubcommandOptionConfiguration<CLI>.registerCommand(subcommandName: String) =
        registerCommand(subcommandName, T::class)

interface BooleanOptionConfiguration: ReadOnlyProperty<CLI, Boolean> {

    var interpretation: FlagInterpretation

    var description: String

    var longName: String
    var shortName: String

    var isHelp: Boolean
    var isRequired: Boolean
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

//TODO regex support? a bridge too far? how can i delimit a regex, or assume that the users matcher will stop when it needs to?
//does anybody actually want this feature?

enum class FlagInterpretation { FLAG_IS_TRUE, FLAG_IS_FALSE }
