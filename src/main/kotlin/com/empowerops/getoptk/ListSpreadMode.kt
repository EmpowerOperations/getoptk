package com.empowerops.getoptk

interface ListSpreadMode {

    //return: left is a set of list item sub-tokens, right is the remaining unparsed tokens
    // eg CSV.reduce(a,b,c --nextArg value ...) returns Pair(listOf("a", "b", "c"), listOf("--nextArg", "value"...)
    fun reduce(tokens: List<Token>): Pair<List<ListItemText>, List<Token>>

    fun toTokenGroupDescriptor(): String
}

//indicate that a list arg is --list x,y,z
val OptionParser.CSV: ListSpreadMode get() = separator(",")

//indicate that you want the args like --list x y z
val OptionParser.varargs: ListSpreadMode get() = Varargs(this.errorReporter)

//indicate that you want args --list x;y;z, where separator = ;
fun OptionParser.separator(separator: String): ListSpreadMode = SeparatorParseMode(errorReporter, separator)

//indicate that you want to use a custom regex to split the list
fun OptionParser.regex(regex: Regex, captureGroupName: String = "item"): ListSpreadMode = TODO()

class Varargs(override val errorReporter: ParseErrorReporter) : ListSpreadMode, ErrorReporting {

    override fun toTokenGroupDescriptor() = "[element1] [element2] [...]"

    override fun reduce(tokens: List<Token>): Pair<List<ListItemText>, List<Token>> = analyzing(tokens) {

        var resultValues = emptyList<ListItemText>()

        var current = next()

        while(current is Argument){
            resultValues += ListItemText(current, 0 .. current.length-1)

            if(isLastElement(rest())) break

            expect<SuperTokenSeparator>()
            current = next()
        }

        return resultValues to rest()
    }

    private fun isLastElement(tokens: List<Token>): Boolean = analyzing(tokens){
        return nextIs<SuperTokenSeparator>() && ( ! hasNext() || next() !is Argument)
    }
}

class SeparatorParseMode(
        override val errorReporter: ParseErrorReporter,
        val separator: String
): ListSpreadMode, ErrorReporting {

    override fun toTokenGroupDescriptor() = "[element1][${separator}element2] [...]"

    override fun reduce(tokens: List<Token>): Pair<List<ListItemText>, List<Token>> = analyzing(tokens){

        val argument = (next() as? Argument) ?: return emptyList<ListItemText>() to tokens

        val results = argument.text.split(separator).fold(Pair(0, emptyList<ListItemText>())){ items, nextListItemText ->
            val contentLowIndex = items.first
            val contentHighIndex = contentLowIndex + nextListItemText.length - 1
            val newOffset = contentLowIndex + separator.length + nextListItemText.length

            Pair(newOffset, items.second + ListItemText(argument, contentLowIndex .. contentHighIndex))
        }

        return results.second to rest()
    }

}