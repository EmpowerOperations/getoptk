package com.empowerops.getoptk

import kotlin.reflect.KClass
import kotlin.reflect.KProperty

class ValueOptionConfiguration<T: Any>(
        val optionType: KClass<T>,
        val converters: Converters,
        val configErrorReporter: ConfigErrorReporter,
        val userConfig: ValueOptionConfiguration<T>.() -> Unit
) : CommandLineOption<T>(), OptionParser {

    override fun toTokenGroupDescriptor() = "-$shortName|--$longName <${optionType.simpleName}-arg>"

    var converter: Converter<T> = converters.getConverterFor(optionType)

    override lateinit var shortName: String
    override lateinit var longName: String
    override lateinit var description: String

    internal var initialized = false
    internal var _value: T? = null
    override lateinit var errorReporter: ParseErrorReporter

    override operator fun getValue(thisRef: CLI, property: KProperty<*>): T{
        require(initialized) { "TODO: nice error message" }
        return _value!!
    }

    override fun finalizeInit(hostingProperty: KProperty<*>) {
        description = Inferred.generateInferredDescription(hostingProperty)
        longName = Inferred.generateInferredLongName(hostingProperty)
        shortName = Inferred.generateInferredShortName(hostingProperty)

        userConfig()
    }

    override fun reduce(tokens: List<Token>): List<Token> = analyzing(tokens){

        if( ! nextIs<OptionPreambleToken>()) return tokens
        if( ! nextIs<OptionName> { it.text in names() }) return tokens
        if( ! nextIs<SeparatorToken>()) return tokens

        val argText = (next() as? Argument)?.text ?: return tokens

        _value = converter.convert(argText)
        initialized = true

        expect<SuperTokenSeparator>()

        return rest()
    }
}

