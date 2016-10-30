package com.empowerops.getoptk

import com.google.common.collect.HashMultimap
import kotlin.reflect.KProperty
import kotlin.reflect.jvm.javaField

object Parser {

    fun <T : CLI> parse(args: Iterable<String>, hostFactory: () -> T): T {

        val (opts, result) = captureRegisteredOpts(hostFactory)

        var tokens = Lexer.lex(args)

        val root = AggregateCombinator(opts)

        tokens = root.reduce(tokens)

        if (tokens.any()){
            TODO("tokens werent consumed: $tokens")
        }

        //also recovery from fixed-point convergence on non-empty tokens
        // (IE: there are unconsumed things in the args list)

        return result
    }

    internal fun <T : CLI> captureRegisteredOpts(hostFactory: () -> T): Pair<List<OptionCombinator>, T> {

        val result = hostFactory()

        val members = result.javaClass.kotlin.members.filterIsInstance<KProperty<*>>()
        val registeredOptions = RegisteredOptions.optionProperties[result]!!.toList()

        RegisteredOptions.optionProperties[result].clear()

        //TODO: sort to allow deterministic hierarchy of duplicate-avoidance scheme
        // in other words, if you have two properties that both start with 'h', what does -h mean?
        // well the alphabetically-first one gets -h, the second gets -hwhatevs
        for (registered in registeredOptions) {

            // pending https://youtrack.jetbrains.com/issue/KT-8384
            val matchingProp = members.single { it.javaField?.apply { isAccessible = true }?.get(result) == registered }
            registered.finalizeInit(matchingProp)
        }

        return registeredOptions to result
    }
}