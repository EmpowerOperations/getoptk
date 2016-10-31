package com.empowerops.getoptk

interface ListSpreadMode {

    //return value: left is a set of list item sub-tokens, right is the remaining unparsed tokens
    fun reduce(tokens: List<Token>): Pair<List<ListItemText>, List<Token>>

    companion object {
        //indicate that a list arg is --list x,y,z
        val CSV: ListSpreadMode = separator(",")

        //indicate that you want the args like --list x y z
        val varargs: ListSpreadMode = Varargs(ErrorReporter.Default)

        //indicate that you want args --list x;y;z, where separator = ;
        fun separator(separator: String): ListSpreadMode = SeparatorParseMode(ErrorReporter.Default, separator)

        //indicate that you want to use a custom regex to split the list
        fun regex(regex: Regex, captureGroupName: String = "item"): ListSpreadMode = TODO()
    }
}

class Varargs(override val errorReporter: ErrorReporter) : ListSpreadMode, ErrorReporting {

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
        return nextIs<SuperTokenSeparator>() && (!hasNext() || next() !is Argument)
    }
}

class SeparatorParseMode(
        override val errorReporter: ErrorReporter,
        val separator: String
): ListSpreadMode, ErrorReporting {

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