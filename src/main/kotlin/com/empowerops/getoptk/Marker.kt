package com.empowerops.getoptk

import kotlin.reflect.KClass

class Marker(
        val errorReporter: ParseErrorReporter,
        private var tokens: List<Token>
){

    var previouslyReadTokens = emptyList<Token>(); private set;
    val allReadTokens: List<Token> get() = previouslyReadTokens + marked()

    var index = 0

    init { resetTo(tokens) }

    fun next() = tokens[index++]
    fun hasNext() = index < tokens.size
    fun previous() = if(index == 0) Epsilon else tokens[index - 1]
    fun peek() = if(index >= tokens.size) null else tokens[index]
    fun marked() = tokens.subList(0, (index).coerceAtMost(tokens.size))
    fun rest() = tokens.subList(index, tokens.size)

    inline fun <reified T: Token> expect(){
        val next = next()
        if (next !is T){
            errorReporter.internalError(next, "token type miss-match: expected token of type ${T::class.simpleName} ")
        }
    }

    inline fun <reified T: Token> nextIs(noinline condition: (T) -> Boolean = { true }) = nextIs(T::class, condition)

    fun <T: Any> nextIs(type: KClass<T>, condition: (T) -> Boolean = { true }): Boolean{
        if ( ! hasNext()) return false
        val current = next()
        return type.java.isInstance(current) && condition(current as T)
    }

    fun resetTo(tokens: List<Token>){
        previouslyReadTokens += marked()
        this.tokens = tokens
        index = 0
    }

    override fun toString() = "previous:${previous()}, rest:${rest()}"
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