package com.empowerops.getoptk

import kotlin.reflect.KProperty
import kotlin.reflect.jvm.javaField

object Parser {

    fun <T : CLI> parse(args: Iterable<String>, hostFactory: () -> T): T {

        val errorReporter = ErrorReporter()

        val (opts, result) = captureRegisteredOpts(errorReporter, hostFactory)

        validateNames(errorReporter, opts)

        if(errorReporter.configurationErrors.any()) throw ConfigurationException(errorReporter.configurationErrors)

        var tokens = Lexer.lex(args)

        val root = TopLevelParser(errorReporter, opts)

        tokens = root.reduce(tokens)

        if (tokens.any()){ errorReporter.internalError(tokens.first(), "unconsumed tokens") }

        //also recovery from fixed-point convergence on non-empty tokens
        // (IE: there are unconsumed things in the args list)

        return result
    }

    private fun validateNames(errorReporter: ErrorReporter, opts: List<OptionParser>) {
        val optionNamePairs: Map<String, List<CommandLineOption<*>>> = opts
                .map { it as CommandLineOption<*> }
                .flatMap { opt -> listOf("-${opt.shortName}" to opt, "--${opt.longName}" to opt) }
                .groupBy { nameOptPair -> nameOptPair.first }
                .mapValues { it -> it.value.map { it.second } }

        for ((duplicateName, options) in optionNamePairs.filter { it.value.size >= 2 }) {
            val optionNames = options.map { it.toTokenGroupDescriptor() }
            errorReporter.reportConfigProblem("Name collision: $duplicateName maps to all of '${optionNames.joinToString("' and '")}'")
        }
    }

    internal fun <T : CLI> captureRegisteredOpts(
            errorReporter: ErrorReporter,
            hostFactory: () -> T
    ): Pair<List<OptionParser>, T> {

        val cmd = hostFactory()

        val members = cmd.javaClass.kotlin.members.filterIsInstance<KProperty<*>>()
        val registeredOptions = RegisteredOptions.optionProperties[cmd]!!.toList()

        RegisteredOptions.optionProperties[cmd].clear()

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
        println("parse failure at $token: $message")
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

    companion object {
        val Default: ErrorReporter = ErrorReporter()

    }

}

class ConfigurationException(val messages: List<String>)
: Exception((listOf("CLI configuration errors:") + messages).joinToString("\n"))