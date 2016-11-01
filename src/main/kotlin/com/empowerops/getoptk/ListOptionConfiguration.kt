package com.empowerops.getoptk

import kotlin.reflect.KClass
import kotlin.reflect.KProperty

class ListOptionConfiguration<T: Any>(
        val optionType: KClass<T>,
        val converters: Converters,
        override val errorReporter: ErrorReporter,
        val userConfig: ListOptionConfiguration<T>.() -> Unit
) : CommandLineOption<List<T>>(), OptionParser {

    override fun toTokenGroupDescriptor() = "-$shortName|--$longName ${parseMode.toTokenGroupDescriptor()}"

    //name change to avoid confusion, user might want a "parse the whole value as a list"
    var elementConverter: Converter<T> = converters.getConverterFor(optionType)

    override var description: String = ""
    override lateinit var shortName: String
    override lateinit var longName: String

    var parseMode: ListSpreadMode = ListSpreadMode.CSV

    internal lateinit var value: List<T>

    override operator fun getValue(thisRef: CLI, property: KProperty<*>): List<T> = value

    override fun finalizeInit(hostingProperty: KProperty<*>) {
        description = Inferred.generateInferredDescription(hostingProperty)
        shortName = Inferred.generateInferredShortName(hostingProperty)
        longName = Inferred.generateInferredLongName(hostingProperty)

        userConfig()
    }

    override fun reduce(tokens: List<Token>): List<Token> = analyzing(tokens){

        if ( ! nextIs<OptionPreambleToken>()) return tokens
        if ( ! nextIs<OptionName> { it.text in names() }) return tokens
        if ( ! nextIs<SeparatorToken>()) return tokens

        val converter = converters.tryFindingConverterFor(optionType)

        value = if(converter != null){
            val (splitItems, remainingTokens) = parseMode.reduce(rest())

            if (splitItems.isEmpty()) return tokens

            resetTo(remainingTokens)

            val results = splitItems.map { itemToken ->

                val wrappedConverter = ErrorHandlingConverter(errorReporter, optionType, elementConverter)

                wrappedConverter.convert(itemToken).second!!
            }

            expect<SuperTokenSeparator>()

            results
        }
        else {

            var results: List<T> = emptyList()

            while(peek() is Argument){

                val reducer = RecursiveReducer(optionType, converters, errorReporter)

                val (result, remaining) = reducer.reduce(rest())

                resetTo(remaining)

                if(result != null) {
                    results += result
                }
                else break;
            }

            results
        }

        return rest()
    }

}