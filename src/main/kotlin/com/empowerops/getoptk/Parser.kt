package com.empowerops.getoptk

import kotlin.reflect.KProperty
import kotlin.reflect.jvm.javaField

object Parser {

    fun <T : CLI> parse(args: Iterable<String>, hostFactory: () -> T): T {

        val errorReporter = ErrorReporter()

        val (opts, result) = captureRegisteredOpts(errorReporter, hostFactory)

        var tokens = Lexer.lex(args)

        val root = AggregateCombinator(errorReporter, opts)

        tokens = root.reduce(tokens)

        if (tokens.any()){ errorReporter.internalError(tokens.first(), "unconsumed tokens") }

        //also recovery from fixed-point convergence on non-empty tokens
        // (IE: there are unconsumed things in the args list)

        return result
    }

    internal fun <T : CLI> captureRegisteredOpts(
            errorReporter: ErrorReporter,
            hostFactory: () -> T
    ): Pair<List<OptionCombinator>, T> {

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

    fun reportProblem(token: Token, location: IntRange, message: String){
        TODO()
    }

    fun internalError(token: Token, errorMessage: String) {
        TODO(errorMessage)
    }

    fun debug(message: () -> String){
        TODO(message())
    }

}