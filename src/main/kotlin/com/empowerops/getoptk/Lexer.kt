package com.empowerops.getoptk

object Lexer {

    fun lex(superTokens: Iterable<String>): List<Token>{

        var tokens: List<Token> = emptyList()
        var currentOffset = 0;

        for(superToken in superTokens){

            var startIndex = 0

            val openingToken = when {
                superToken.startsWith("--") -> LongPreamble(currentOffset)
                superToken.startsWith("-") -> ShortPreamble(currentOffset)
                superToken.startsWith("/") -> WindowsPreamble(currentOffset)
                else -> null
            }

            if(openingToken != null) {
                tokens += openingToken
                startIndex += openingToken.length
                currentOffset += openingToken.length
            }

            val resultingTokenText = superToken.substring(startIndex)

            tokens += when {
                tokens.lastOrNull() is OptionPreambleToken && "=" in resultingTokenText -> {
                    splitAssignmentTokens(resultingTokenText, currentOffset)
                }
                tokens.lastOrNull() is OptionPreambleToken -> {
                    OptionName(resultingTokenText, currentOffset).asSingleList()
                }
                else -> {
                    Argument(resultingTokenText, currentOffset).asSingleList()
                }
            }
            currentOffset += resultingTokenText.length

            val separator = SuperTokenSeparator(currentOffset)
            tokens += separator
            currentOffset += separator.length
        }

        return tokens
    }

    private fun splitAssignmentTokens(resultingTokenText: String, currentOffset: Int): List<Token> = with(resultingTokenText){
        val indexOfAssignment = indexOf("=")

        val option = OptionName(substring(0, indexOfAssignment), currentOffset)
        val assignment = AssignmentSeparator(currentOffset + indexOfAssignment)
        val argument = Argument(substring(indexOfAssignment + 1), currentOffset + indexOfAssignment + 1)

        return listOf(option, assignment, argument)
    }
}


//TODO add location info for debug messages
interface Token {
    val text: String;
    val location: IntRange;
    val length: Int get() = text.length
}

object Epsilon: Token {
    override val text = ""
    override val location = 0..0
    override fun toString() = "[ε]"
}

interface Lemma: Token {
    val Lemma: String
    val index: Int
    override val text: String get() = Lemma
    override val location: IntRange get() = index .. index + Lemma.length - 1
}

interface Word: Token {
    val index: Int
    override val location: IntRange get() = index .. index + text.length - 1
}

interface SeparatorToken : Token
interface OptionPreambleToken: Token

data class SuperTokenSeparator(val index: Int): SeparatorToken {
    override val text = " "
    override val location = index .. index + text.length -1
}
data class AssignmentSeparator(override val index: Int): Lemma, SeparatorToken { override val Lemma: String = "=" }

data class ShortPreamble(override val index: Int): Lemma, OptionPreambleToken { override val Lemma = "-"; }
data class LongPreamble(override val index: Int): Lemma, OptionPreambleToken { override val Lemma = "--"; }
data class WindowsPreamble(override val index: Int): Lemma, OptionPreambleToken { override val Lemma = "/"; }

data class OptionName(override val text: String, override val index: Int): Word, Token
data class Argument(override val text: String, override val index: Int): Word, Token

data class ListItemText(val parent: Token, val rangeInParent: IntRange): Token {
    override val text: String get() = parent.text.substring(rangeInParent)
    override val location: IntRange get() = parent.location.start.offset(rangeInParent)
}

fun <T> T.asSingleList() = listOf(this)
fun Int.offset(range: IntRange) = this + range.start .. this + range.endInclusive
