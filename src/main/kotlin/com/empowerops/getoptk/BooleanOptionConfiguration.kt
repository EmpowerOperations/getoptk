package com.empowerops.getoptk

import kotlin.reflect.KProperty

class BooleanOptionConfiguration(
        source: CLI,
        private val userConfig: BooleanOptionConfiguration.() -> Unit
): CommandLineOption<Boolean>, OptionCombinator {

    init { RegisteredOptions.optionProperties += source to this }

    override lateinit var description: String

    override lateinit var longName: String
    override lateinit var shortName: String

    internal var value: Boolean? = null

    override operator fun getValue(thisRef: CLI, property: KProperty<*>): Boolean = value!!

    override fun finalizeInit(hostingProperty: KProperty<*>) {
        description = Inferred.generateInferredDescription(hostingProperty)
        longName = Inferred.generateInferredLongName(hostingProperty)
        shortName = Inferred.generateInferredShortName(hostingProperty)

        userConfig()
    }

    override fun reduce(tokens: List<Token>): List<Token> = with(Marker(tokens)){

        if ( ! nextIs<OptionPreambleToken>()
                || ! nextIs<OptionName>{ it.text in names() }
                || ! nextIs<SuperTokenSeparator>()){
            return tokens;
        }

        return rest()
    }
}

