package com.empowerops.getoptk

interface ListSpreadMode {

    fun reduce(tokens: List<Token>): Pair<List<String>, List<Token>>

    companion object {
        //indicate that a list arg is --list x,y,z
        val CSV: ListSpreadMode = separator(",")

        //indicate that you want the args like --list x y z
        val varargs: ListSpreadMode = Varargs

        //indicate that you want args --list x;y;z, where separator = ;
        fun separator(separator: String): ListSpreadMode = SeparatorParseMode(separator).adapted()

        //indicate that you want to use a custom regex to split the list
        fun regex(regex: Regex, captureGroupName: String = "item"): ListSpreadMode = TODO()
    }
}

internal object Varargs : ListSpreadMode, ErrorReporting {

    override val errorReporter = ErrorReporter.Default

    override fun reduce(tokens: List<Token>): Pair<List<String>, List<Token>> = analyzing(tokens) {

        var resultValues = emptyList<String>()

        var current = next()

        while(current is Argument){
            resultValues += current.text

            if(isLastElement(rest())) break

            expect<SuperTokenSeparator>()
            current = next()
        }

        return resultValues to rest()
    }

    private fun isLastElement(tokens: List<Token>): Boolean = analyzing(tokens){
        return nextIs<SuperTokenSeparator>() && (!hasNext() || next() !is Argument)
    }
}

interface Simple {
    fun spread(argumentText: String): List<String>
}

class SimpleParseModeAdapter(val simple: Simple): ListSpreadMode {
    override fun reduce(tokens: List<Token>): Pair<List<String>, List<Token>> {
        val argument = (tokens.firstOrNull() as? Argument)?.text ?: return emptyList<String>() to tokens

        return simple.spread(argument) to tokens.tail()
    }
}

class SeparatorParseMode(val separator: String): Simple {
    override fun spread(argumentText: String): List<String> = argumentText.split(separator)
}

fun Simple.adapted() = SimpleParseModeAdapter(this)
fun <T> List<T>.tail() = drop(1)