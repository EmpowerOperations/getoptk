package com.empowerops.getoptk

import java.util.*
import kotlin.reflect.KClass

class Marker(
        val errorReporter: ParseErrorReporter,
        private var tokens: List<Token>
): List<Token> by tokens {

    val marks: Deque<Int> = LinkedList()

    var index = 0

    internal fun pushMark(): Unit = if(any()) marks.push(index) else Unit
    internal fun popMarkedTokens(): List<Token> = if(any()) validSublistFrom(marks.pop()) else emptyList()
    internal fun markedTokens(): List<Token> = validSublistFrom(marks.peek())
    internal fun popAndRevertToMark() { index = marks.pop() }

    private fun validSublistFrom(startIndex: Int): List<Token>
            = tokens.subList(startIndex, index.coerceAtMost(tokens.size - 1))

    fun next() = tokens[index++]
    fun hasNext() = index < tokens.size
    fun previous() = if(index == 0) Epsilon else tokens[index - 1]
    fun peek(lookAhead: Int = 0) = if(index+lookAhead >= tokens.size) Epsilon else tokens[index+lookAhead]
    fun rest(): List<Token> = this

    inline fun <reified T: Token> expect(): Token
            = expect("expected ${T::class.simpleName}") { it is T }
    inline fun <reified T1: Token, reified T2: Token> expectEither()
            = expect("expected ${T1::class.simpleName} or ${T2::class.simpleName}") { it is T1 || it is T2 }

    fun expect(message: String, requirement: (Token) -> Boolean): Token {
        val next = next()
        if ( ! requirement(next)){
            errorReporter.reportParsingProblem(next, message)
        }
        return next
    }

    override fun toString() = "previous:${previous()}, rest:${rest()}"
}

internal fun <R> ErrorReporting.analyzing(
        tokens: List<Token>,
        commandName: String? = null,
        opts: List<AbstractCommandLineOption<*>>? = null,
        block: Marker.() -> R
): R {

    val marker = tokens as? Marker ?: Marker(errorReporter, tokens)

    marker.pushMark()
    marker.errorReporter.enterScope(commandName, opts)

    try {
        return marker.block()
    }
    finally {
        val readTokens = marker.popMarkedTokens()
        marker.errorReporter.exitScope()
    }
}
