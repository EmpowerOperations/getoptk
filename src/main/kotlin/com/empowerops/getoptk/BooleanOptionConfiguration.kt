package com.empowerops.getoptk

import kotlin.reflect.KProperty

class BooleanOptionConfiguration(source: CLI): CommandLineOption<Boolean>, OptionCombinator {

    init { RegisteredOptions.optionProperties += source to this }

    override var description: String = ""

    //problem: how do we express "compact" form (eg tar -xfvj)?
    override var names: List<String> = CommandLineOption.INFER_NAMES

    // problem: worth allowing a user to specify a custom parsing mode?
    // dont think so.

    override operator fun getValue(thisRef: CLI, property: KProperty<*>): Boolean = TODO();

    override fun finalizeInit(hostingProperty: KProperty<*>) {
        if(description == "") description = Inferred.generateInferredDescription(hostingProperty)
        if(names === CommandLineOption.INFER_NAMES) names = Inferred.generateInferredNames(hostingProperty)
    }

    override fun reduce(tokens: List<Token>): List<Token> {
        if(tokens[0] is OptionPreambleToken
                && tokens[1].let { it is OptionName && it.text in names }
                && tokens[2].let { it is SuperTokenSeparator }){

            return tokens.subList(3, tokens.size)
        }
        else return tokens
    }
}

object Inferred {

    fun generateInferredDescription(prop: KProperty<*>) = "[description of $prop]"

    fun generateInferredNames(prop: KProperty<*>) = with(prop) { listOf("$name", "${name[0]}") }
}