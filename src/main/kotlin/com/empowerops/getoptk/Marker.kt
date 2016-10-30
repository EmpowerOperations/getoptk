package com.empowerops.getoptk

class Marker(private var tokens: List<Token>){

    var index = 0
    lateinit var iterator: Iterator<Token>

    init { resetTo(tokens) }

    fun next(): Token {
        index += 1
        return iterator.next()
    }

    fun hasNext() = iterator.hasNext()

    fun current() = tokens[index]
    fun peek() = tokens[index + 1]

    inline fun <reified T: Token> expect(){
        val next = next()
        if (next !is T){
            TODO("log some kind of warning")
        }
    }

    inline fun <reified T: Token> nextIs(noinline condition: (T) -> Boolean = { true })
            = (next() as? T)?.run(condition) ?: false

    fun marked(): List<Token> = tokens.subList(0, (index+1).coerceAtMost(tokens.size))
    fun rest(): List<Token> = tokens.subList(index, tokens.size)

    fun resetTo(tokens: List<Token>){
        this.tokens = tokens
        iterator = tokens.iterator()
        index = 0
    }
}