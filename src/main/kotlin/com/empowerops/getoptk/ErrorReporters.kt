package com.empowerops.getoptk

import java.util.*

/**
 * Created by Geoff on 2017-03-09.
 */


data class ConfigurationProblem(val message: String, val stackTrace: Exception = ConfigurationExceptionCause())

data class ParseProblem(val message: String, val stackTrace: Exception?, val usage: String)
data class UsageRequest(val usage: String)

class ConfigErrorReporter(){

    fun reportConfigProblem(message: String){
        configurationErrors += ConfigurationProblem(message)
    }
    fun reportConfigProblem(message: String, ex: Exception){
        configurationErrors += ConfigurationProblem(message, ex)
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
        private set

    var usages: List<UsageRequest> = emptyList()
        private set

    private var commandScope: Deque<Pair<String, List<AbstractCommandLineOption<*>>>> = LinkedList()

    internal fun enterScope(commandName: String?, opts: List<AbstractCommandLineOption<*>>?){
        val prev = commandScope.peek()
        commandScope.push((commandName ?: prev.first) to (opts ?: prev.second))
    }
    internal fun exitScope(){
        commandScope.pop()
    }

    internal fun printUsage(commandName: String, opts: List<AbstractCommandLineOption<*>>){
        usages += UsageRequest(makeHelpMessage(commandName, opts))
    }
    fun reportParsingProblem(token: Token, errorMessage: String) {
        val (name, opts) = commandScope.peek()
        reportParsingProblem(token, errorMessage, name, opts)
    }
    internal fun reportParsingProblem(
            token: Token,
            message: String,
            commandName: String,
            opts: List<AbstractCommandLineOption<*>>,
            exception: Exception? = null
    ){
        
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
                  |
                  |${exception ?: ""}
                  """.trimMargin().trim()

        val usage = makeHelpMessage(commandName, opts)

        val updatedException = if(!debug) exception else Exception(exception)

        parsingProblems += ParseProblem(rendered, updatedException, usage)
    }

    fun debug(message: () -> String){
        if(debug){
            println("debug: ${message()}")
        }
    }
    fun internalError(token: Token, errorMessage: String) {
        throw Exception("internal error at $token: $errorMessage")
    }
}

class ConfigurationException private constructor(message: String, cause: Exception?) : RuntimeException(message, cause){
    companion object {
        operator fun invoke(failure: ConfigurationFailure): ConfigurationException {

            val newlineSeparatedMessages = failure.configurationProblems.joinToString("\n  ") { it.message }
            val message = """CLI configuration errors:
                |  $newlineSeparatedMessages
                """.trimMargin()

            val exceptions = failure.configurationProblems.map { it.stackTrace }
            val cause = exceptions.firstOrNull()
            val suppressed = exceptions.drop(1)

            return ConfigurationException(message, cause).apply {
                suppressed.forEach { cause!!.addSuppressed(it) }
            }
        }
    }
}

class ParseFailedException private constructor(message: String, cause: Exception?) : RuntimeException(message, cause){
    companion object {
        operator fun invoke(failure: ParseFailure): ParseFailedException {

            val newlineSeparatedMessages = failure.parseProblems.joinToString("\n\n") { it.message + "\n\n" + it.usage }
            val message = """Failure in parsing command line:
                |
                |$newlineSeparatedMessages
                """.trimMargin()

            val exceptions = failure.parseProblems.map { it.stackTrace }
            val cause = exceptions.firstOrNull()
            val suppressed = exceptions.drop(1)

            return ParseFailedException(message, cause).apply {
                suppressed.forEach { cause!!.addSuppressed(it) }
            }
        }
    }
}

class HelpException private constructor(message: String) : RuntimeException(message) {
    companion object {
        operator fun invoke(failure: HelpRequested) =
                HelpException(failure.helpMessages.joinToString("\n\n") { it.usage })
    }
}

class MissingOptionsException private constructor(message: String): RuntimeException(message){
    companion object {
        operator fun invoke(failure: MissingOptions): MissingOptionsException {
            val options = failure.missingOptions
                    .map { it as AbstractCommandLineOption }
                    .joinToString("', '", "'", "'") { it.longName }

            val message = """Missing required options: $options
                |${failure.helpMessage}
                """.trimMargin()

            return MissingOptionsException(message)
        }
    }
}

private class ConfigurationExceptionCause: RuntimeException("Configuration Exception")
private class ParseExceptionCause: RuntimeException("Parse Exception")

interface ErrorReporting { val errorReporter: ParseErrorReporter }