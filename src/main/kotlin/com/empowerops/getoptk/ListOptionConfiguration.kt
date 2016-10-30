package com.empowerops.getoptk

import kotlin.reflect.KClass
import kotlin.reflect.KProperty

class ListOptionConfiguration<T: Any>(
        source: CLI,
        optionType: KClass<T>,
        private val userConfig: ListOptionConfiguration<T>.() -> Unit)
: CommandLineOption<T>, OptionCombinator {

    init { RegisteredOptions.optionProperties += source to this }

    //name change to avoid confusion, user might want a "parse the whole value as a list"
    var elementConverter: Converter<T> = Converters.getDefaultFor(optionType)

    override var description: String = ""
    override lateinit var shortName: String
    override lateinit var longName: String

    var parseMode: ParseMode = ParseMode.CSV

    override operator fun getValue(thisRef: CLI, property: KProperty<*>): T {
        TODO()
    }

    override fun finalizeInit(hostingProperty: KProperty<*>) {
        description = Inferred.generateInferredDescription(hostingProperty)
        shortName = Inferred.generateInferredShortName(hostingProperty)
        longName = Inferred.generateInferredLongName(hostingProperty)

        userConfig()
    }

    override fun reduce(tokens: List<Token>): List<Token> {

    }

}