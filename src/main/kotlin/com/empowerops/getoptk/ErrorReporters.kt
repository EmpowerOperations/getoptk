package com.empowerops.getoptk

/**
 * Created by Geoff on 2017-03-09.
 */

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

data class ConfigurationProblem(val message: String, val stackTrace: Exception = ConfigurationExceptionCause()){

    fun toDescribedProblem(): String {
        val relevantFrame = stackTrace.stackTrace.firstOrNull() //TODO: make this point at the specific line.
        // problem is its a caller of an inline function
        return "$message\n\tspecified $relevantFrame"
    }
}

class ParseErrorReporter(val programNamePrefix: String, val tokens: List<Token>) {

    var parsingProblems: List<String> = emptyList()
        private set;

    var firstException: Exception? = null

    var requestedHelp: Boolean = false

    fun reportParsingProblem(token: Token, message: String, exception: Exception? = null){
        firstException = firstException ?: exception
        
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

        parsingProblems += rendered
    }

    fun debug(message: () -> String){
//        println("debug: ${message()}")
    }
    fun internalError(token: Token, errorMessage: String) {
        println("internal error at $token: $errorMessage")
    }
}

class ConfigurationException(val messages: List<ConfigurationProblem>)
    : RuntimeException((listOf("CLI configuration errors:") + messages.map { it.toDescribedProblem() }).joinToString("\n"), messages.firstOrNull()?.stackTrace)

class ParseFailedException(val messages: List<String>, cause: Exception?)
    : RuntimeException(messages.joinToString("\n\n"), cause)

class HelpException(message: String) : RuntimeException(message)

class ConfigurationExceptionCause: RuntimeException("Configuration Exception")

interface ErrorReporting { val errorReporter: ParseErrorReporter }