package com.empowerops.getoptk

class Marker(
        val errorReporter: ErrorReporter,
        private var tokens: List<Token>
){

    var previouslyReadTokens = emptyList<Token>(); private set;
    val allReadTokens: List<Token> get() = previouslyReadTokens + marked()

    var index = 0

    init { resetTo(tokens) }

    fun next() = tokens[index++]
    fun hasNext() = index < tokens.size
    fun current() = tokens[index]
    fun peek() = tokens[index + 1]
    fun marked() = tokens.subList(0, (index).coerceAtMost(tokens.size))
    fun rest() = tokens.subList(index, tokens.size)

    inline fun <reified T: Token> expect(){
        val next = next()
        if (next !is T){
            errorReporter.internalError(next, "token type miss-match: expected token of type ${T::class.simpleName} ")
        }
    }

    inline fun <reified T: Token> nextIs(noinline condition: (T) -> Boolean = { true })
            = (next() as? T)?.run(condition) ?: false

    fun resetTo(tokens: List<Token>){
        previouslyReadTokens += marked()
        this.tokens = tokens
        index = 0
    }
}

internal inline fun <R> ErrorReporting.analyzing(tokens: List<Token>, block: Marker.() -> R): R {

    val marker = Marker(errorReporter, tokens)

    try {
        return marker.block()
    }
    finally {
        log(marker, tokens)
    }
}

private fun ErrorReporting.log(marker: Marker, tokens: List<Token>) {
    errorReporter.debug {
        """finished analysis
          |tokens=$tokens
          |caller=${this}
          |lastReadToken=${marker.allReadTokens.lastOrNull()}
          |allReadTokens=${marker.allReadTokens}
          |consumedTokenCount=${marker.allReadTokens.size}
          """.trimMargin()
    }
}