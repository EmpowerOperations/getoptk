package com.empowerops.getoptk

import kotlin.reflect.KProperty

internal interface OptionCombinator {
    fun finalizeInit(hostingProperty: KProperty<*>)
    fun reduce(tokens: List<Token>): List<Token>
}

internal class AggregateCombinator(val componentCombinators: List<OptionCombinator>) : OptionCombinator {
    override fun finalizeInit(hostingProperty: KProperty<*>) = TODO()

    override fun reduce(tokens: List<Token>): List<Token> {

        var currentTokens = tokens

        do {
            println("iterating once")
            val oldTokens = currentTokens
            currentTokens = componentCombinators.fold(tokens) { remaining, opt -> opt.reduce(remaining) }
        }
        while (currentTokens != oldTokens && currentTokens.any())

        return currentTokens;
    }

}