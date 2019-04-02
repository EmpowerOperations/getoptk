package com.empowerops.getoptk

import kotlin.reflect.KClass
import kotlin.reflect.KProperty
import kotlin.reflect.full.createInstance

/**
 * Created by Geoff on 2017-04-29.
 */
internal sealed class AbstractCommandLineOption<out T>: CommandLineOption<T> {

    lateinit var property: KProperty<*>

    lateinit var description: String
    lateinit var shortName: String
    lateinit var longName: String
    lateinit var argumentTypeDescription: String

    abstract var isRequired: Boolean
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

    var _value: ValueStrategy<@UnsafeVariance T> = NoValue
    var value: @UnsafeVariance T
        get() {
            val valWrapper = _value

            val result = when(valWrapper){
                is Value -> valWrapper.value
                is Provider -> {
                    val provided = valWrapper.provider.value
                    //TODO: synchronization? exceptions?
                    _value = Value(provided)
                    provided
                }
                NoValue -> throw UninitializedPropertyAccessException("${this.toPropertyDescriptor()} has not been initialized")
            }

            if(this !is ListOptionConfigurationImpl<*>) require(optionType.isInstance(result) || result == null)

            return result
        }
        set(newValue) { _value = Value(newValue) }

    val valueString: String get() = value.toString()

    final override operator fun getValue(thisRef: CLI, property: KProperty<*>): T = value

    fun toKeyValueString() = "${property.name}=$valueString"
}


internal data class BooleanOptionConfigurationImpl(
        val userConfig: BooleanOptionConfiguration.() -> Unit,
        override var interpretation: FlagInterpretation = FlagInterpretation.FLAG_IS_TRUE,
        override val optionType: KClass<Boolean> = Boolean::class,
        override var isHelp: Boolean = false,
        override var isRequired: Boolean  = false
) : AbstractCommandLineOption<Boolean>(), BooleanOptionConfiguration {
    
    init {
        hasArgument = false
    }

    override fun applyAdditionalConfiguration(thisRef: CLI, prop: KProperty<*>?) {

        invokeAndReportErrorsTo(thisRef.errorReporter, this) { userConfig() }

        _value = Value(when(interpretation){
            FlagInterpretation.FLAG_IS_TRUE -> false
            FlagInterpretation.FLAG_IS_FALSE -> true
        })
    }
}

internal data class ValueOptionConfigurationImpl<T: Any>(
        override val optionType: KClass<T>,
        val userConfig: ValueOptionConfiguration<T>.() -> Unit,
        override var isRequired: Boolean = true
) : AbstractCommandLineOption<T>(), ValueOptionConfiguration<T>, ValueOrNullableValueConfiguration<T> {

    override var converter: Converter<T> = DefaultConverters[optionType] ?: InvalidConverter

    private var _default: Any = DefaultValues[optionType] ?: NO_DEFAULT_AVAILABLE
    override var default: T
        get(){
            if (_default != NO_DEFAULT_AVAILABLE) @Suppress("UNCHECKED_CAST") return _default as T
            else throw IllegalStateException("'default' is write-only as it currently has no value")
        }
        set(value) {
            _default = value
        }

    override fun applyAdditionalConfiguration(thisRef: CLI, prop: KProperty<*>?) {

        invokeAndReportErrorsTo(thisRef.errorReporter, this) { userConfig() }

        _value = if(_default != NO_DEFAULT_AVAILABLE) Value(default) else NoValue
    }
}

internal data class NullableValueOptionConfigurationImpl<T: Any>(
        override val optionType: KClass<T>,
        val userConfig: NullableValueOptionConfiguration<T>.() -> Unit,
        override var isRequired: Boolean = false
) : AbstractCommandLineOption<T?>(), NullableValueOptionConfiguration<T>, ValueOrNullableValueConfiguration<T?> {

    override var converter: Converter<T?> = DefaultConverters[optionType] ?: InvalidConverter
    override var default: T? = DefaultValues[optionType]

    override fun applyAdditionalConfiguration(thisRef: CLI, prop: KProperty<*>?) {

        invokeAndReportErrorsTo(thisRef.errorReporter, this) { userConfig() }

        if(_value == NoValue) _value = Value(default)
    }
}

internal data class ListOptionConfigurationImpl<E: Any>(
        override val optionType: KClass<E>,
        val userConfig: ListOptionConfiguration<E>.() -> Unit,
        override var parseMode: ListSpreadMode<E> = Varargs(DefaultConverters[optionType] ?: InvalidConverter),
        override var isRequired: Boolean = false,
        override var default: List<E> = emptyList()
) : AbstractCommandLineOption<List<E>>(), ListOptionConfiguration<E>{

    var converter: Converter<E> = InvalidConverter
    var factoryOrErrors: FactorySearchResult<E> = NullFactory
    
    override fun applyAdditionalConfiguration(thisRef: CLI, prop: KProperty<*>?) {

        invokeAndReportErrorsTo(thisRef.errorReporter, this) { userConfig() }

        if(_value == NoValue) _value = Value(default)

        //this line must be done after the user has been allowed to configure the converters
        val parseMode = parseMode
        @Suppress("UNUSED_VARIABLE") val dk = when(parseMode){
            is ImplicitObjects<E> -> factoryOrErrors = makeFactoryFor(optionType, ConverterSet(parseMode.converters))
            is Varargs<E> -> converter = parseMode.elementConverter
            is CSV<E> -> converter = parseMode.elementConverter
        }
    }
}

internal data class SubcommandOptionConfigurationImpl<C: CLI>(
        override val optionType: KClass<C>,
        val userConfig: SubcommandOptionConfiguration<C>.() -> Unit,
        override var isRequired: Boolean = true
) : AbstractCommandLineOption<C>(), SubcommandOptionConfiguration<C>{

    private val subCommands = findSubCommands()
    internal lateinit var resolvedCommand: Subcommand

    private fun findSubCommands() = optionType.sealedSubclasses.map { it.createInstance() }.map { it as Subcommand }

    internal fun hasSubcommandNamed(name: String): Boolean {
        return name in subCommands.map { it.name }
    }

    private var _default: Any = DefaultValues[optionType] ?: NO_DEFAULT_AVAILABLE
    override var default: C
        get(){
            if (_default != NO_DEFAULT_AVAILABLE) @Suppress("UNCHECKED_CAST") return _default as C
            else throw IllegalStateException("'default' is write-only as it currently has no value")
        }
        set(value) {
            _default = value
        }


    fun resolveOpts(text: String, errorReporter: ParseErrorReporter): List<AbstractCommandLineOption<*>> {
        resolvedCommand = subCommands.single { it.name == text }

        return resolvedCommand.optionProperties
    }

    override fun <T: Any> registerCommand(subcommandName: String, type: KClass<T>) {
        TODO()
    }

    override fun applyAdditionalConfiguration(thisRef: CLI, prop: KProperty<*>?) {

        invokeAndReportErrorsTo(thisRef.errorReporter, this) { userConfig() }

//        if(_value == NoValue) _value = Value(default)
    }
}

internal data class ObjectOptionConfigurationImpl<T: Any>(
        override val optionType: KClass<T>,
        val userConfig: ObjectOptionConfigurationImpl<T>.() -> Unit,
        override var isRequired: Boolean = true
) : AbstractCommandLineOption<T>(), ObjectOptionConfiguration<T>, ObjectOrNullableObjectConfiguration<T> {

    override var converters: ConverterSet = ConverterSet(emptyMap())
    override var factoryOrErrors: FactorySearchResult<T> = NullFactory

    private var _default: Any = NO_DEFAULT_AVAILABLE
    override var default: T
        get(){
            if (_default != NO_DEFAULT_AVAILABLE) @Suppress("UNCHECKED_CAST") return _default as T
            else throw IllegalStateException("'default' is write-only as it currently has no value")
        }
        set(value) {
            _default = value
        }

    override fun <N : Any> registerConverter(type: KClass<N>, converter: Converter<N>) {
        converters += type to converter
    }

    override fun applyAdditionalConfiguration(thisRef: CLI, prop: KProperty<*>?) {

        invokeAndReportErrorsTo(thisRef.errorReporter, this) { userConfig() }

        //this line must be done after the user has been allowed to configure the converters
        factoryOrErrors = makeFactoryFor(optionType, converters)

        _value = if(_default != NO_DEFAULT_AVAILABLE) {
            Value(default)
        }
        else {
//            val provider = makeProviderOf(optionType, converters)
//
//            if(provider is UnrolledAndUntypedFactory<*> && provider.arity == 0) {
//                Provider { optionType.cast(provider.make(emptyList())) }
//            }
//            else {
                NoValue
//            }
        }
    }
}

internal data class NullableObjectOptionConfigurationImpl<T: Any>(
        override val optionType: KClass<T>,
        val userConfig: NullableObjectOptionConfiguration<T>.() -> Unit,
        override var isRequired: Boolean = false
) : AbstractCommandLineOption<T?>(), NullableObjectOptionConfiguration<T>, ObjectOrNullableObjectConfiguration<T?> {

    override var default: T? = null
    override var converters: ConverterSet = ConverterSet(emptyMap())
    override var factoryOrErrors: FactorySearchResult<T?> = NullFactory

    override fun <N : Any> registerConverter(type: KClass<N>, converter: Converter<N?>) {
        converters += type to converter
    }

    override fun applyAdditionalConfiguration(thisRef: CLI, prop: KProperty<*>?) {

        invokeAndReportErrorsTo(thisRef.errorReporter, this){ userConfig() }

        //this line must be done after the user has been allowed to configure the converters
        factoryOrErrors = makeFactoryFor(optionType, converters)

        _value = Value(default)
    }
}

internal interface ObjectOrNullableObjectConfiguration<T> {
    val converters: ConverterSet
    val factoryOrErrors: FactorySearchResult<T>
}
internal interface ValueOrNullableValueConfiguration<T>{
    val converter: Converter<T>
}

sealed class ValueStrategy<out T>
object NoValue : ValueStrategy<Nothing>() {
    override fun toString() = "[no value set]"
}
class Provider<out T>(val provider: Lazy<T>): ValueStrategy<T>() {
    constructor(initializer: () -> T): this(lazy(initializer))

    override fun equals(other: Any?): Boolean {
        if(other !is Provider<*>) return false
        return provider.value == other.provider.value
    }

    override fun hashCode() = provider.value?.hashCode() ?: -1
}
data class Value<out T>(val value: T): ValueStrategy<T>()

internal fun <T> invokeAndReportErrorsTo(errorReporter: ConfigErrorReporter, option: AbstractCommandLineOption<*>, func: () -> T){
    try { func() }
    catch(ex: Exception){ errorReporter.reportConfigProblem("specification for '${option.longName}' threw $ex", ex) }
}
