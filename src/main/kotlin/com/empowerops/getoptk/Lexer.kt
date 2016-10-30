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
interface Token { val length: Int }
abstract class Lemma: Token { abstract val Lemma: String; override val length: Int get() = Lemma.length }

interface SeparatorToken : Token
object SuperTokenSeparator: SeparatorToken { override val length = 0; override fun toString() = "[separator]"}
object AssignmentSeparator: Lemma(), SeparatorToken { override val Lemma = "="; override fun toString() = "[=]"}

interface OptionPreambleToken: Token
object ShortPreamble: Lemma(), OptionPreambleToken { override val Lemma = "-"; override fun toString() = "[-]"}
object LongPreamble: Lemma(), OptionPreambleToken { override val Lemma = "--"; override fun toString() = "[--]" }
object WindowsPreamble: Lemma(), OptionPreambleToken { override val Lemma = "/"; override fun toString() = "[/]" }

data class OptionName(val text: String): Token { override val length: Int get() = text.length }
data class Argument(val text: String): Token { override val length: Int get() = text.length }

fun <T> T.asSingleList() = listOf(this)
