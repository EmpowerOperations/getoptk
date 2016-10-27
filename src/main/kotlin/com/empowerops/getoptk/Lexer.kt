package com.empowerops.getoptk

object Lexer {

    var tokens: List<Token> = emptyList()
    var currentStack: List<Char> = emptyList()

    val regex = "".toRegex()

    fun lex(superTokens: Iterable<String>): List<Token>{

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
object SuperTokenSeparator: Token
object ShortPreamble: OptionPreambleToken { val Lexeme = "-" }
object LongPreamble: OptionPreambleToken { val Lexeme = "--" }
object WindowsPreamble: OptionPreambleToken { val Lexeme = "/" }
data class OptionName(val text: String): Token
data class Argument(val text: String): Token
