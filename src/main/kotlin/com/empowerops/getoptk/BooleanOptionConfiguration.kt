package com.empowerops.getoptk

import kotlin.reflect.KProperty

class BooleanOptionConfiguration(
        source: CLI,
        private val userConfig: BooleanOptionConfiguration.() -> Unit
): CommandLineOption<Boolean>, OptionCombinator {

    init { RegisteredOptions.optionProperties += source to this }

    override var description: String = ""

    //problem: how do we express "compact" form (eg tar -xfvj)?
    override lateinit var longName: String
    override lateinit var shortName: String

    // problem: worth allowing a user to specify a custom parsing mode?
    // dont think so.

    override operator fun getValue(thisRef: CLI, property: KProperty<*>): Boolean = TODO();

    override fun finalizeInit(hostingProperty: KProperty<*>) {
        description = Inferred.generateInferredDescription(hostingProperty)
        longName = Inferred.generateInferredLongName(hostingProperty)
        shortName = Inferred.generateInferredShortName(hostingProperty)

        userConfig()
    }

    override fun reduce(tokens: List<Token>): List<Token> {
        if(tokens[0] is OptionPreambleToken
                && tokens[1].let { it is OptionName && it.text in longName }
                && tokens[2].let { it is SuperTokenSeparator }){

            return tokens.subList(3, tokens.size)
        }
        else return tokens
    }
}

object Inferred {

    fun generateInferredDescription(prop: KProperty<*>) = "[description of $prop]"

    //TODO this needs to register to some kind of conflict-avoiding pool.
    // such that if two properties start with the same letter say ('h'),
    // we don't introduce ambiguity
    fun generateInferredShortName(prop: KProperty<*>) = prop.name[0].toString()
    fun generateInferredLongName(prop: KProperty<*>) = prop.name
}