package com.empowerops.getoptk

import kotlin.reflect.KProperty

internal interface OptionCombinator {

    val errorReporter: ErrorReporter

    fun finalizeInit(hostingProperty: KProperty<*>)
    fun reduce(tokens: List<Token>): List<Token>
}

internal inline fun <R> OptionCombinator.analyzing(tokens: List<Token>, block: Marker.() -> R): R {
    with(Marker(errorReporter, tokens)){
        val result: R = block()
        errorReporter.debug {
            """finished analysis
              |tokens=$tokens
              |caller=${this@analyzing}
              |lastReadToken=${allReadTokens.lastOrNull()}
              |allReadTokens=$allReadTokens
              |result=$result
              """.trimMargin()
        }
        return result;
    }
}

internal class AggregateCombinator(
        override val errorReporter: ErrorReporter,
        val componentCombinators: List<OptionCombinator>
) : OptionCombinator {

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