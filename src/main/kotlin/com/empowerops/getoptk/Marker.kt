package com.empowerops.getoptk

class Marker(val tokens: List<Token>){

    var index = 0;
    val iterator = tokens.iterator()

    fun next(): Token {
        index += 1
        return iterator.next()
    }

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
}