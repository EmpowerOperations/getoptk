package com.empowerops.getoptk

import java.util.*

/**
 * Created by Geoff on 2017-03-09.
 */


data class ConfigurationProblem(val message: String, val stackTrace: Exception? = null){}
data class ParseProblem(val message: String, val stackTrace: Exception?, val usage: String){}
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

    internal fun printUsage(){
        val (commandName, opts) = commandScope.peek()
        usages += UsageRequest(makeHelpMessage(commandName, opts))
    }
    fun reportParsingProblem(token: Token?, errorMessage: String, exception: Exception? = null) {
        val (name, opts) = commandScope.peek()
        reportParsingProblem(token, errorMessage, name, opts, exception)
    }
    internal fun reportParsingProblem(
            token: Token?,
            message: String,
            commandName: String,
            opts: List<AbstractCommandLineOption<*>>,
            exception: Exception? = null
    ){
        
        val tokens = tokens.dropLastWhile { it is SuperTokenSeparator }
        val commandLine = programNamePrefix + " " + tokens.joinToString(separator = "") { it.text }

        val AT = "at:"
        val superposition = if(token != null)
                " ".repeat(programNamePrefix.length + 1 - AT.length + token.location.start) +
                "~".repeat(token.text.length.coerceAtLeast(1))
        else null

        val rendered =
                """$message
                  |$commandLine
                  |${if(token != null) "$AT$superposition" else ""}
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

            val newlineSeparatedMessages = failure.parseProblems
                    .groupBy { it.usage }.entries
                    .joinToString { (usage, parseProblems) ->
                        parseProblems.joinToString("\n\n", postfix = "\n\n") { it.message } + usage
                    }
            val message = """Failure in parsing command line:
                |
                |$newlineSeparatedMessages
                """.trimMargin()

            val exceptions = failure.parseProblems.map { it.stackTrace }.filter { it != null }
            val cause = exceptions.firstOrNull()
            val suppressed = exceptions.drop(1)

            return ParseFailedException(message, cause).apply {
                suppressed.forEach { cause ?.addSuppressed(it) }
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

interface ErrorReporting { val errorReporter: ParseErrorReporter }