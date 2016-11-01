package com.empowerops.getoptk

import kotlin.reflect.KProperty
import kotlin.reflect.jvm.javaField

object Parser {

    fun <T : CLI> parse(args: Iterable<String>, hostFactory: () -> T): T {

        val errorReporter = ErrorReporter.Default

        val (opts, result) = captureRegisteredOpts(errorReporter, hostFactory)

        validateNames(errorReporter, opts)

        if(errorReporter.configurationErrors.any()) throw ConfigurationException(errorReporter.configurationErrors)

        var tokens = Lexer.lex(args)

        val root = TopLevelParser(errorReporter, opts)

        tokens = root.reduce(tokens)

        if (tokens.any()){ errorReporter.internalError(tokens.first(), "unconsumed tokens") }

        if(errorReporter.parsingProblems.any()){
            printUsage()
            throw ParseFailedException(errorReporter.parsingProblems)
        }

        return result
    }

    private fun printUsage() {

    }

    private fun validateNames(errorReporter: ErrorReporter, opts: List<OptionParser>) {
        val optionNamePairs: Map<String, List<CommandLineOption<*>>> = opts
                .map { it as CommandLineOption<*> }
                .flatMap { opt -> listOf("-${opt.shortName}" to opt, "--${opt.longName}" to opt) }
                .groupBy { nameOptPair -> nameOptPair.first }
                .mapValues { it -> it.value.map { it.second } }

        for ((duplicateName, options) in optionNamePairs.filter { it.value.size >= 2 }) {
            val optionNames = options.sortedBy { it.longName /*for reproducability*/ }.map { it.toTokenGroupDescriptor() }
            errorReporter.reportConfigProblem("Name collision: $duplicateName maps to all of '${optionNames.joinToString("' and '")}'")
        }
    }

    internal fun <T : CLI> captureRegisteredOpts(
            errorReporter: ErrorReporter,
            hostFactory: () -> T
    ): Pair<List<OptionParser>, T> {

        val cmd = hostFactory()

        val members = cmd.javaClass.kotlin.members.filterIsInstance<KProperty<*>>()

        val registeredOptions: List<OptionParser> = when(cmd){
            is CLI.LocalRegistration -> cmd.registry
            else -> RegisteredOptions.optionProperties[cmd].let {
                val result: List<OptionParser> = it.toList()
                it.clear()
                result
            }
        }

        for (registered in registeredOptions) {

            // pending https://youtrack.jetbrains.com/issue/KT-8384
            val matchingProp = members.single { it.javaField?.apply { isAccessible = true }?.get(cmd) == registered }
            registered.finalizeInit(matchingProp)
        }


        return registeredOptions to cmd
    }
}

class ErrorReporter {

    fun reportParsingProblem(token: Token, message: String){
        parsingProblems += "at ${token.toLocationString()}: $message"
    }

    fun reportConfigProblem(message: String){
        configurationErrors += message
    }

    fun internalError(token: Token, errorMessage: String) {
        println("internal error at $token: $errorMessage")
    }

    fun debug(message: () -> String){
//        println("debug: ${message()}")
    }

    var configurationErrors: List<String> = emptyList()
        private set;
    var parsingProblems: List<String> = emptyList()
        private set;

    companion object {
        val Default: ErrorReporter = ErrorReporter()

    }

    fun Token.toLocationString(): String = "'$text'"

}

class ConfigurationException(val messages: List<String>)
: Exception((listOf("CLI configuration errors:") + messages).joinToString("\n"))

class ParseFailedException(val messages: List<String>)
: Exception((listOf("Unrecognized option:") + messages).joinToString("\n"))