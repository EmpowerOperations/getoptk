package com.empowerops.getoptk

import kotlin.reflect.KClass
import kotlin.reflect.KProperty

class ListOptionConfiguration<T: Any>(
        source: CLI,
        optionType: KClass<T>,
        private val userConfig: ListOptionConfiguration<T>.() -> Unit)
: CommandLineOption<List<T>>, OptionCombinator {

    init { RegisteredOptions.optionProperties += source to this }

    //name change to avoid confusion, user might want a "parse the whole value as a list"
    var elementConverter: Converter<T> = Converters.getDefaultFor(optionType)

    override var description: String = ""
    override lateinit var shortName: String
    override lateinit var longName: String

    var parseMode: ParseMode = ParseMode.CSV

    internal lateinit var value: List<T>;

    override operator fun getValue(thisRef: CLI, property: KProperty<*>): List<T> = value

    override fun finalizeInit(hostingProperty: KProperty<*>) {
        description = Inferred.generateInferredDescription(hostingProperty)
        shortName = Inferred.generateInferredShortName(hostingProperty)
        longName = Inferred.generateInferredLongName(hostingProperty)

        userConfig()
    }

    override fun reduce(tokens: List<Token>): List<Token> = with(Marker(tokens)) {

        if ( ! nextIs<OptionPreambleToken>()) return tokens
        if ( ! nextIs<OptionName>{ it.text in names() }) return tokens
        if ( ! nextIs<SeparatorToken>()) return tokens

        val argument = (next() as? Argument)?.text ?: return tokens

        val splitItems = parseMode.spread(argument)

        if (splitItems.isEmpty()) return tokens

        val parsedItems = splitItems.map { elementConverter.convert(it) }

        value = parsedItems

        expect<SuperTokenSeparator>()

        return rest()
    }

}