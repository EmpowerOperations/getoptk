package com.empowerops.getoptk

object Lexer {

    fun lex(superTokens: Iterable<String>): List<Token>{

        var tokens: List<Token> = emptyList()

        for(superToken in superTokens){

            var startIndex = 0

            val openingToken = when {
                superToken.startsWith(LongPreamble.Lemma) -> LongPreamble
                superToken.startsWith(ShortPreamble.Lemma) -> ShortPreamble
                superToken.startsWith(WindowsPreamble.Lemma) -> WindowsPreamble
                else -> null
            }

            if(openingToken != null) {
                tokens += openingToken
                startIndex += openingToken.length
            }

            val resultingTokenText = superToken.substring(startIndex)

            tokens += when {
                tokens.lastOrNull() is OptionPreambleToken && AssignmentSeparator.Lemma in resultingTokenText -> {
                    splitAssignmentTokens(resultingTokenText)
                }
                tokens.lastOrNull() is OptionPreambleToken -> {
                    OptionName(resultingTokenText).asSingleList()
                }
                else -> {
                    Argument(resultingTokenText).asSingleList()
                }
            }

            tokens += SuperTokenSeparator
        }

        return tokens
    }

    private fun splitAssignmentTokens(resultingTokenText: String): List<Token> = with(resultingTokenText){
        val indexOfAssignment = indexOf(AssignmentSeparator.Lemma)

        val option = OptionName(substring(0, indexOfAssignment))
        val argument = Argument(substring(indexOfAssignment + 1))

        return listOf(option, AssignmentSeparator, argument)
    }
}


//TODO add location info for debug messages
interface Token { val text: String; val length: Int get() = text.length }

object Epsilon: Token {
    override val text = ""

    override fun toString() = "[ε]"
}

abstract class Lemma(val Lemma: String): Token {
    override val length: Int get() = Lemma.length;
    override val text: String get() = Lemma

    override fun toString() = "[$Lemma]"
}

interface SeparatorToken : Token
object SuperTokenSeparator: SeparatorToken {
    override val text = ""

    override fun toString() = "[separator]"
}
object AssignmentSeparator: Lemma("="), SeparatorToken

interface OptionPreambleToken: Token
object ShortPreamble: Lemma("-"), OptionPreambleToken
object LongPreamble: Lemma("--"), OptionPreambleToken
object WindowsPreamble: Lemma("/"), OptionPreambleToken

data class OptionName(override val text: String): Token
data class Argument(override val text: String): Token

fun <T> T.asSingleList() = listOf(this)

data class ListItemText(val parent: Token, val rangeInParent: IntRange): Token {
    override val text: String get() = parent.text.substring(rangeInParent)
}
