package com.empowerops.getoptk

import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KClass
import kotlin.reflect.KProperty

/**
 * Created by Geoff on 2017-04-29.
 */
internal sealed class CommandLineOption<out T>: ReadOnlyProperty<CLI, T> {

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


internal class BooleanOptionConfigurationImpl(
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

internal class ValueOptionConfigurationImpl<T: Any>(
        override val optionType: KClass<T>,
        val userConfig: ValueOptionConfiguration<T>.() -> Unit
) : CommandLineOption<T>(), ValueOptionConfiguration<T>, ValueOrNullableValueConfiguration<T> {

    private var _default: Any = UNINITIALIZED

    override var isRequired: Boolean = true
    override lateinit var converter: Converter<T>

    override var default: T
        @Suppress("UNCHECKED_CAST") get() =
        if (_default != UNINITIALIZED) _default as T
        else throw IllegalStateException("'default' is write-only as it currently has no value")
        set(value) { _default = value }

    override fun applyAdditionalConfiguration(thisRef: CLI, prop: KProperty<*>?) {
        converter = DefaultConverters[optionType] ?: InvalidConverter

        userConfig()

        _default = getDefaultValueOrLogError(thisRef.errorReporter, optionType, _default, this.toPropertyDescriptor())
        value = _default
    }
}

internal class NullableValueOptionConfigurationImpl<T: Any>(
        override val optionType: KClass<T>,
        val userConfig: NullableValueOptionConfiguration<T>.() -> Unit
) : CommandLineOption<T?>(), NullableValueOptionConfiguration<T>, ValueOrNullableValueConfiguration<T?> {

    override lateinit var converter: Converter<T?>
    override var default: T? = DefaultValues[optionType]
    override var isRequired: Boolean = false

    override fun applyAdditionalConfiguration(thisRef: CLI, prop: KProperty<*>?) {
        converter = DefaultConverters[optionType] ?: InvalidConverter

        userConfig()

        value = default
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
) : CommandLineOption<T>(), ObjectOptionConfiguration<T>, ObjectOrNullableObjectConfiguration<T> {

    private var _default: Any = UNINITIALIZED

    override var converters: ConverterSet = ConverterSet(emptyMap())
    override lateinit var factoryOrErrors: FactorySearchResult<T>

    override var isRequired: Boolean = true

    override var default: T get() =
                if (_default != UNINITIALIZED) @Suppress("UNCHECKED_CAST") (_default as T)
                else throw IllegalStateException("'default' is write-only as it currently has no value")
        set(value) { _default = value }

    override fun <N : Any> registerConverter(type: KClass<N>, converter: Converter<N>) {
        converters += type to converter
    }

    override fun applyAdditionalConfiguration(thisRef: CLI, prop: KProperty<*>?) {

        userConfig()

        //this line must be done after the user has been allowed to configure the converters
        factoryOrErrors = makeFactoryFor(optionType, converters)

        _default = getDefaultValueOrLogError(thisRef.errorReporter, optionType, _default, this.toPropertyDescriptor())
        value = _default
    }
}

internal class NullableObjectOptionConfigurationImpl<T: Any>(
        override val optionType: KClass<T>,
        val userConfig: NullableObjectOptionConfiguration<T>.() -> Unit
) : CommandLineOption<T?>(), NullableObjectOptionConfiguration<T>, ObjectOrNullableObjectConfiguration<T?> {

    override var default: T? = DefaultValues[optionType]
    override var isRequired: Boolean = false

    override var converters: ConverterSet = ConverterSet(emptyMap())
    override lateinit var factoryOrErrors: FactorySearchResult<T?>

    override fun <N : Any> registerConverter(type: KClass<N>, converter: Converter<N?>) {
        converters += type to converter
    }

    override fun applyAdditionalConfiguration(thisRef: CLI, prop: KProperty<*>?) {

        userConfig()

        //this line must be done after the user has been allowed to configure the converters
        factoryOrErrors = makeFactoryFor(optionType, converters)

        value = default
    }
}


internal interface ObjectOrNullableObjectConfiguration<T> {
    val converters: ConverterSet
    val factoryOrErrors: FactorySearchResult<T>
}
internal interface ValueOrNullableValueConfiguration<T>{
    val converter: Converter<T>
}