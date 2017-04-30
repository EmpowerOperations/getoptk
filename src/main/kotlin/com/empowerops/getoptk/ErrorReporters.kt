package com.empowerops.getoptk

/**
 * Created by Geoff on 2017-03-09.
 */


data class ConfigurationProblem(val message: String, val stackTrace: Exception = ConfigurationExceptionCause())

data class ParseProblem(val message: String, val stackTrace: Exception?)

class ConfigErrorReporter(){

    fun reportConfigProblem(message: String){
        configurationErrors += ConfigurationProblem(message)
    }

    fun internalError(token: Token, errorMessage: String) {
        println("internal error at $token: $errorMessage")
    }

    fun debug(message: () -> String){
//        println("debug: ${message()}")
    }

    var configurationErrors: List<ConfigurationProblem> = emptyList()
        private set;
}

class ParseErrorReporter(val programNamePrefix: String, val tokens: List<Token>) {

    var parsingProblems: List<ParseProblem> = emptyList()
        private set;

    var requestedHelp: Boolean = false

    fun reportParsingProblem(token: Token, message: String, exception: Exception? = null){
        
        val tokens = tokens.dropLastWhile { it is SuperTokenSeparator }
        val commandLine = programNamePrefix + " " + tokens.joinToString(separator = "") { it.text }

        val superposition = (" ".repeat(programNamePrefix.length + 1 - "at:".length + token.location.start)
                + "~".repeat(token.text.length)
//                + " ".repeat(commandLine.length - token.location.endInclusive))
        )

        val rendered =
                """$message
                  |$commandLine
                  |at:$superposition
                  |${exception ?: ""}
                  """.trimMargin().trim()

        parsingProblems += ParseProblem(rendered, exception)
    }

    fun debug(message: () -> String){
//        println("debug: ${message()}")
    }
    fun internalError(token: Token, errorMessage: String) {
        println("internal error at $token: $errorMessage")
    }
}

class ConfigurationException(val messages: List<ConfigurationProblem>)
    : RuntimeException((listOf("CLI configuration errors:") + messages.map { it.message }).joinToString("\n"), messages.firstOrNull()?.stackTrace)

class ParseFailedException(val messages: List<String>, cause: Exception?)
    : RuntimeException(messages.joinToString("\n\n"), cause)

class HelpException(message: String) : RuntimeException(message)

class MissingOptionsException: RuntimeException()

private class ConfigurationExceptionCause: RuntimeException("Configuration Exception")
private class ParseExceptionCause: RuntimeException("Parse Exception")

interface ErrorReporting { val errorReporter: ParseErrorReporter }