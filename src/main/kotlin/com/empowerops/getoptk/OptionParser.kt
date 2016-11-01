package com.empowerops.getoptk

import kotlin.reflect.KProperty

interface ErrorReporting {
    val errorReporter: ErrorReporter
}

interface OptionParser: ErrorReporting {

    override val errorReporter: ErrorReporter

    fun finalizeInit(hostingProperty: KProperty<*>)

    fun reduce(tokens: List<Token>): List<Token>
}

internal class TopLevelParser(
        override val errorReporter: ErrorReporter,
        val componentCombinators: List<OptionParser>
) : OptionParser {

    override fun finalizeInit(hostingProperty: KProperty<*>) = throw UnsupportedOperationException()

    override fun reduce(tokens: List<Token>): List<Token> {

        var remainingTokens = tokens

        do {
            val oldTokens = remainingTokens

            for(option in componentCombinators){

                remainingTokens = option.reduce(remainingTokens)

                if(remainingTokens.isEmpty()) break;
            }

            if(remainingTokens == oldTokens){
                remainingTokens = recover(remainingTokens)
            }
        }
        while (remainingTokens != oldTokens && remainingTokens.any())

        return remainingTokens;
    }

    private fun recover(remainingTokens: List<Token>): List<Token> {
        val unconsumedTokens = remainingTokens.takeWhile { it !is SuperTokenSeparator }.map { it.text }
        errorReporter.internalError(remainingTokens.first(), "dropped tokens $unconsumedTokens")
        return remainingTokens.dropWhile { it !is SuperTokenSeparator }.drop(1)
    }

}