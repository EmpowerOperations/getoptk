package com.empowerops.getoptk

object Lexer {

    fun lex(superTokens: Iterable<String>): List<Token>{

        var tokens: List<Token> = emptyList()

        for(superToken in superTokens){

            var startIndex = 0;
            when{
                superToken.startsWith(LongPreamble.Lexeme) -> { tokens += LongPreamble; startIndex += LongPreamble.Lexeme.length }
                superToken.startsWith(ShortPreamble.Lexeme) -> { tokens += ShortPreamble; startIndex += ShortPreamble.Lexeme.length }
                superToken.startsWith(WindowsPreamble.Lexeme) -> { tokens += WindowsPreamble; startIndex += WindowsPreamble.Lexeme.length }
            }

            val resultingTokenText = superToken.substring(startIndex)

            if(tokens.lastOrNull() is OptionPreambleToken) tokens += OptionName(resultingTokenText)
                    else tokens += Argument(resultingTokenText)

            tokens += SuperTokenSeparator
        }

        return tokens
    }
}

interface Token
interface OptionPreambleToken: Token
object SuperTokenSeparator: Token { override fun toString() = "[separator]"}
object ShortPreamble: OptionPreambleToken { val Lexeme = "-"; override fun toString() = "[preamble -]"}
object LongPreamble: OptionPreambleToken { val Lexeme = "--"; override fun toString() = "[preamble --]" }
object WindowsPreamble: OptionPreambleToken { val Lexeme = "/"; override fun toString() = "[preamble /]" }
data class OptionName(val text: String): Token
data class Argument(val text: String): Token
