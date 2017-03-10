package com.empowerops.getoptk

import java.util.*
import kotlin.reflect.KClass

class Marker(
        val errorReporter: ParseErrorReporter,
        private var tokens: List<Token>
): List<Token> by tokens {

    val marks: Deque<Int> = LinkedList()

    var index = 0

    internal fun pushMark(): Unit = marks.push(index)
    internal fun popMarkedTokens(): List<Token> = validSublistFrom(marks.pop())
    internal fun markedTokens(): List<Token> = validSublistFrom(marks.peek())
    internal fun popAndRevertToMark() { index = marks.pop() }

    private fun validSublistFrom(startIndex: Int): List<Token>
            = tokens.subList(startIndex, index.coerceAtMost(tokens.size - 1))

    fun next() = tokens[index++]
    fun hasNext() = index < tokens.size
    fun previous() = if(index == 0) Epsilon else tokens[index - 1]
    fun peek(lookAhead: Int = 0) = if(index+lookAhead >= tokens.size) Epsilon else tokens[index+lookAhead]
    fun rest(): List<Token> = this

    inline fun <reified T: Token> expect(): Token {
        val next = next()
        if (next !is T){
            errorReporter.internalError(next, "token type miss-match: expected token of type ${T::class.simpleName} ")
        }

        return next
    }

    inline fun <reified T: Token> nextIs(noinline condition: (T) -> Boolean = { true }) = nextIs(T::class, condition)

    fun <T: Any> nextIs(type: KClass<T>, condition: (T) -> Boolean = { true }): Boolean{
        if ( ! hasNext()) return false
        val current = next()
        return type.java.isInstance(current) && condition(current as T)
    }

    override fun toString() = "previous:${previous()}, rest:${rest()}"
}

internal fun <R> ErrorReporting.analyzing(tokens: List<Token>, block: Marker.() -> R): R {

    val marker = tokens as? Marker ?: Marker(errorReporter, tokens)

    marker.pushMark()

    try {
        return marker.block()
    }
    finally {
        val readTokens = marker.popMarkedTokens()
        log(readTokens)
    }
}

private fun ErrorReporting.log(tokens: List<Token>) {
    errorReporter.debug {
        """finished analysis
          |lastReadToken=${tokens.last()}
          |allReadTokens=$tokens
          """.trimMargin()
    }
}