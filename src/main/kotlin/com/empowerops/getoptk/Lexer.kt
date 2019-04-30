package com.empowerops.getoptk

object Lexer {

    fun lex(args: Iterable<String>): List<Token>{

        var tokens: List<Token> = emptyList()
        var currentOffset = 0;

        // args is a pre-lexed command line. Unfortunately we dont have access to the regular command line
        // (consider that we might have been invoked from a C-style "exec(char** args)")
        // so I'm calling these blocks "super tokens", and inserting a "SuperTokenSeparator" between them.
        // see the LexerFixture for samples on how they are parsed,
        for(superToken in args){

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
                tokens.lastOrNull() is OptionPreambleToken && resultingTokenText.length == 1 -> {
                    ShortOptionName(resultingTokenText, currentOffset).asSingleList()
                }
                tokens.lastOrNull() is OptionPreambleToken && resultingTokenText.length > 1 -> {
                    LongOptionName(resultingTokenText, currentOffset).asSingleList()
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

        val optionText = substring(0, indexOfAssignment)
        val option = if(optionText.length == 1) ShortOptionName(optionText, currentOffset) else LongOptionName(optionText, currentOffset)
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

//token to represent EOF or other tokenization failure
object Epsilon: Token {
    override val text = ""
    override val location = 0..0
    override fun toString() = "[ε]"
}

//a keyword
interface Lemma: Token {
    val Lemma: String
    val index: Int
    override val text: String get() = Lemma
    override val location: IntRange get() = index .. index + Lemma.length - 1
}

//a dynamic word
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
data class AssignmentSeparator(override val index: Int): Lemma, SeparatorToken { override val Lemma get() = "=" }
data class MinorSeparator(override val index: Int): Lemma, SeparatorToken { override val Lemma get() = "," }

data class ShortPreamble(override val index: Int): Lemma, OptionPreambleToken { override val Lemma get() = "-"; }
data class LongPreamble(override val index: Int): Lemma, OptionPreambleToken { override val Lemma get() = "--"; }
data class WindowsPreamble(override val index: Int): Lemma, OptionPreambleToken { override val Lemma get() = "/"; }

sealed class OptionName: Token {}
data class ShortOptionName(override val text: String, override val index: Int): OptionName(), Word, Token { init { require(text.length == 1) } }
data class LongOptionName(override val text: String, override val index: Int): OptionName(), Word, Token
data class Argument(override val text: String, override val index: Int): Word, Token

fun <T> T.asSingleList() = listOf(this)
fun Int.offset(range: IntRange) = this + range.start .. this + range.endInclusive
