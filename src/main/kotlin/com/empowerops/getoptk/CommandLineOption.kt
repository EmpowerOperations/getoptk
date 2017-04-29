package com.empowerops.getoptk

import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KClass
import kotlin.reflect.KProperty
import kotlin.reflect.full.cast

internal sealed class CommandLineOption<out T>: ReadOnlyProperty<CLI, T>{

    lateinit var property: KProperty<*>

    lateinit var description: String
    lateinit var shortName: String
    lateinit var longName: String
    lateinit var argumentTypeDescription: String

    var hasArgument = true

    abstract val optionType: KClass<*>
    abstract fun applyAdditionalConfiguration(thisRef: CLI, prop: KProperty<*>?): Unit

    fun provideDelegateImpl(thisRef: CLI, prop: KProperty<*>) {

        property = prop

        shortName = Inferred.shortName(prop)
        longName = Inferred.longName(prop)
        description = Inferred.description(prop)
        argumentTypeDescription = Inferred.argumentType(prop.returnType.classifier!! as KClass<*>)

        applyAdditionalConfiguration(thisRef, prop)

        thisRef.errorReporter.validateNewEntry(thisRef.optionProperties, this)

        thisRef.optionProperties += this
    }

    internal var value: Any? = UNINITIALIZED
    @Suppress("UNCHECKED_CAST") final override operator fun getValue(thisRef: CLI, property: KProperty<*>): T {
        if(value == UNINITIALIZED) throw IllegalStateException("no value for ${this.toPropertyDescriptor()}")

        //TODO: is there anything stronger I can do to avoid pollution?
        //IIRC with thrown exceptions from `parseCLI`, there is no leakage of this object if the value is left unset.
        return value as T
    }
}

//region must be public!

operator fun <T: Any> ValueOptionConfiguration<T>.provideDelegate(thisRef: CLI, prop: KProperty<*>) = provideDelegateImpl(this, thisRef, prop)
operator fun <T: Any> ObjectOptionConfiguration<T>.provideDelegate(thisRef: CLI, prop: KProperty<*>) = provideDelegateImpl(this, thisRef, prop)
operator fun <T: Any> ListOptionConfiguration<T>.provideDelegate(thisRef: CLI, prop: KProperty<*>) = provideDelegateImpl(this, thisRef, prop)
operator fun BooleanOptionConfiguration.provideDelegate(thisRef: CLI, prop: KProperty<*>) = provideDelegateImpl(this, thisRef, prop)

internal fun <T, E> provideDelegateImpl(host: T, thisRef: CLI, prop: KProperty<*>): T where T: ReadOnlyProperty<CLI, E> {
    (host as CommandLineOption<*>).provideDelegateImpl(thisRef, prop)
    return host
}

//endregion

internal val ValueOptionConfiguration<*>.asImpl: ValueOptionConfigurationImpl<*> get() = this as ValueOptionConfigurationImpl<*>
internal val ObjectOptionConfiguration<*>.asImpl: ObjectOptionConfigurationImpl<*> get() = this as ObjectOptionConfigurationImpl<*>
internal val ListOptionConfiguration<*>.asImpl: ListOptionConfigurationImpl<*> get() = this as ListOptionConfigurationImpl<*>
internal val BooleanOptionConfiguration.asImpl: BooleanOptionConfigurationImpl get() = this as BooleanOptionConfigurationImpl


private object UNINITIALIZED

internal fun CommandLineOption<*>.names() = listOf(shortName, longName)
internal fun CommandLineOption<*>.toPropertyDescriptor(): String {
    val valOrVarPrefix = property.toString().substring(0, 3)

    val name = property.name
    val type = (property.returnType.classifier as? KClass<*>)?.simpleName ?: return property.toString()

    val getOptFlavour = when(this){
        is ValueOptionConfigurationImpl<*> -> "getValueOpt"
        is BooleanOptionConfigurationImpl -> "getFlagOpt"
        is ListOptionConfigurationImpl<*> -> "getValueOpt"
        is ObjectOptionConfigurationImpl<*> -> "getValueOpt"
    }

    return "$valOrVarPrefix $name: $type by $getOptFlavour()"
}

open internal class BooleanOptionConfigurationImpl(
        val userConfig: BooleanOptionConfiguration.() -> Unit
) : CommandLineOption<Boolean>(), BooleanOptionConfiguration {

    override lateinit var interpretation: FlagInterpretation
    override val optionType = Boolean::class
    override var isHelp = false

    init {
        hasArgument = false
    }

    override fun applyAdditionalConfiguration(thisRef: CLI, prop: KProperty<*>?) {

        interpretation = FlagInterpretation.FLAG_IS_TRUE

        userConfig()

        value = when(interpretation){
            FlagInterpretation.FLAG_IS_TRUE -> false
            FlagInterpretation.FLAG_IS_FALSE -> true
        } 
    }
}

internal fun makeHelpOption(otherOptions: List<CommandLineOption<*>>) = BooleanOptionConfigurationImpl {
    longName = if(otherOptions.any { it.longName == "help" }) "" else "help"
    shortName = if(otherOptions.any { it.shortName == "h" }) "" else "h"
    isHelp = true
}

internal class ValueOptionConfigurationImpl<T: Any>(
        override val optionType: KClass<T>,
        val userConfig: ValueOptionConfiguration<T>.() -> Unit
) : CommandLineOption<T>(), ValueOptionConfiguration<T> {

    override lateinit var converter: Converter<T>

    override fun applyAdditionalConfiguration(thisRef: CLI, prop: KProperty<*>?) {
        converter = DefaultConverters[optionType] ?: InvalidConverter

        userConfig()
    }
}

internal class ListOptionConfigurationImpl<E: Any>(
        override val optionType: KClass<E>,
        val userConfig: ListOptionConfiguration<E>.() -> Unit
) : CommandLineOption<List<E>>(), ListOptionConfiguration<E>{

    override lateinit var parseMode: ListSpreadMode<E>

    var factoryOrErrors: FactorySearchResult<E>? = null
    var converter: Converter<E>? = null

    override fun applyAdditionalConfiguration(thisRef: CLI, prop: KProperty<*>?) {

        parseMode = Varargs(DefaultConverters[optionType] ?: InvalidConverter)

        userConfig()

        //this line must be done after the user has been allowed to configure the converters
        val parseMode = parseMode
        @Suppress("UNUSED_VARIABLE") val dk = when(parseMode){
            is ImplicitObjects<E> -> factoryOrErrors = makeFactoryFor(optionType, ConverterSet(parseMode.converters))
            is Varargs<E> -> converter = parseMode.elementConverter
            is CSV<E> -> converter = parseMode.elementConverter
        }
    }
}

internal class ObjectOptionConfigurationImpl<T: Any>(
        override val optionType: KClass<T>,
        val userConfig: ObjectOptionConfigurationImpl<T>.() -> Unit
) : CommandLineOption<T>(), ObjectOptionConfiguration<T> {

    internal var converters: ConverterSet = ConverterSet(emptyMap())
    internal lateinit var factoryOrErrors: FactorySearchResult<T>

    override fun <N : Any> registerConverter(type: KClass<N>, converter: Converter<N>) {
        converters += type to converter
    }

    override fun applyAdditionalConfiguration(thisRef: CLI, prop: KProperty<*>?) {

        userConfig()

        //this line must be done after the user has been allowed to configure the converters
        factoryOrErrors = makeFactoryFor(optionType, converters)
    }

}