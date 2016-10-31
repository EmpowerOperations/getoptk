package com.empowerops.getoptk

import kotlin.reflect.KProperty

internal interface ErrorReporting {
    val errorReporter: ErrorReporter
}

internal interface OptionParser: ErrorReporting {

    override val errorReporter: ErrorReporter

    fun finalizeInit(hostingProperty: KProperty<*>)

    fun reduce(tokens: List<Token>): List<Token>
}

internal inline fun <R> ErrorReporting.analyzing(tokens: List<Token>, block: Marker.() -> R): R {
    with(Marker(errorReporter, tokens)){

        val result: R = block()

        errorReporter.debug {
            """finished analysis
              |tokens=$tokens
              |caller=${this@analyzing}
              |lastReadToken=${allReadTokens.lastOrNull()}
              |allReadTokens=$allReadTokens
              |result=$result
              |consumedTokenCount=${allReadTokens.size}
              """.trimMargin()
        }
        return result;
    }
}

internal class AggregateParser(
        override val errorReporter: ErrorReporter,
        val componentCombinators: List<OptionParser>
) : OptionParser {

    override fun finalizeInit(hostingProperty: KProperty<*>) = throw UnsupportedOperationException()

    override fun reduce(tokens: List<Token>): List<Token> {

        var remainingTokens = tokens

        do {
            val oldTokens = remainingTokens

            remainingTokens = componentCombinators.fold(remainingTokens) { remaining, opt ->
                if(remaining.any()) opt.reduce(remaining) else remaining
            }

            if(remainingTokens == oldTokens){
                remainingTokens = recover(remainingTokens)
            }
        }
        while (remainingTokens != oldTokens && remainingTokens.any())

        return remainingTokens;
    }

    private fun recover(remainingTokens: List<Token>): List<Token> {
        errorReporter.reportParsingProblem(remainingTokens.first(), "unrecognized option")
        return remainingTokens.dropWhile { it !is SuperTokenSeparator }.drop(1)
    }

}