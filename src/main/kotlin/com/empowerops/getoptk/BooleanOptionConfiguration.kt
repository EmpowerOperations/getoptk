package com.empowerops.getoptk

import kotlin.reflect.KProperty

class BooleanOptionConfiguration(
        override val errorReporter: ErrorReporter,
        private val userConfig: BooleanOptionConfiguration.() -> Unit
): CommandLineOption<Boolean>, OptionParser {

    override lateinit var description: String

    override lateinit var longName: String
    override lateinit var shortName: String

    internal var value: Boolean = false

    override operator fun getValue(thisRef: CLI, property: KProperty<*>): Boolean = value

    override fun finalizeInit(hostingProperty: KProperty<*>) {
        description = Inferred.generateInferredDescription(hostingProperty)
        longName = Inferred.generateInferredLongName(hostingProperty)
        shortName = Inferred.generateInferredShortName(hostingProperty)

        userConfig()
    }

    override fun reduce(tokens: List<Token>): List<Token> = analyzing(tokens){

        if ( ! nextIs<OptionPreambleToken>()) return tokens
        if ( ! nextIs<OptionName> { it.text in names() }) return tokens

        value = true

        expect<SuperTokenSeparator>()

        return rest()
    }
}



