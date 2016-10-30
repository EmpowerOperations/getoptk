package com.empowerops.getoptk

interface ParseMode {

    fun reduce(tokens: List<Token>): Pair<List<String>, List<Token>>

    companion object {
        //indicate that a list arg is --list x,y,z
        val CSV: ParseMode = separator(",")

        //indicate that a list arg is --list x --list y --list z
        val iteratively: ParseMode = IterativeParseMode.adapted()

        //indicate that you want the args like --list x y z
        val varargs: ParseMode = VarargsParseMode

        //indicate that you want args --list x;y;z, where separator = ;
        fun separator(separator: String): ParseMode = SeparatorParseMode(separator).adapted()

        //indicate that you want to use a custom regex to split the list
        fun regex(regex: Regex, captureGroupName: String = "item"): ParseMode = TODO()
    }
}

object VarargsParseMode: ParseMode {
    override fun reduce(tokens: List<Token>): Pair<List<String>, List<Token>> = with(Marker(tokens)) {

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

    private fun isLastElement(tokens: List<Token>): Boolean = with(Marker(tokens)){
        return nextIs<SuperTokenSeparator>() && (!hasNext() || next() !is Argument)
    }
}

interface SimpleParseMode {
    fun spread(argumentText: String): List<String>
}

class SimpleParseModeAdapter(val simple: SimpleParseMode): ParseMode {
    override fun reduce(tokens: List<Token>): Pair<List<String>, List<Token>> {
        val argument = (tokens.firstOrNull() as? Argument)?.text ?: return emptyList<String>() to tokens

        return simple.spread(argument) to tokens.tail()
    }
}

class SeparatorParseMode(val separator: String): SimpleParseMode {
    override fun spread(argumentText: String): List<String> = argumentText.split(separator)
}

object IterativeParseMode: SimpleParseMode {
    override fun spread(argumentText: String): List<String> = listOf(argumentText)
}


fun SimpleParseMode.adapted() = SimpleParseModeAdapter(this)
fun <T> List<T>.tail() = drop(1)