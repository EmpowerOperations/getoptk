package com.empowerops.getoptk

import com.google.common.collect.HashMultimap
import kotlin.reflect.KProperty
import kotlin.reflect.jvm.javaField

object FrontEndParser {

    fun <T : CLI> parse(args: Array<String>, hostFactory: () -> T): T {

        val (opts, result) = captureRegisteredOpts(hostFactory)

//            val ast = ANTLR.buildAST(args, opts)
//            ast.walk(OptionUpdatingWalker(opts))

//            CakeParser.buildAST(args, opts)

        var tokens = Lexer.lex(args.asIterable())
//            val parsedOpts = parse(tokens, opts)

        val root = AggregateCombinator(opts)

        tokens = root.reduce(tokens)

        if (tokens.any()) TODO("tokens werent consumed!")

        //also recovery from fixed-point convergence on non-empty tokens
        // (IE: there are unconsumed things in the args list)

        return result;
    }

    internal fun <T : CLI> captureRegisteredOpts(hostFactory: () -> T): Pair<List<OptionCombinator>, T> {
        RegisteredOptions.optionProperties = HashMultimap.create()
        val result = hostFactory()

        val members = result.javaClass.kotlin.members.filterIsInstance<KProperty<*>>()
        val registeredOptions = RegisteredOptions.optionProperties[result]!!.toList()

        //TODO: sort to allow deterministic hierarchy of duplicate-avoidance scheme
        // in other words, if you have two properties that both start with 'h', what does -h mean?
        // well the alphabetically-first one gets -h, the second gets -hwhatevs
        for (registered in registeredOptions) {

            //hmm, seems like there is no way to avoid SecurityExceptions if we want parsing to be eager
            // in other words, its probably a better idea to stick to the lazy parsing idea...
            val matchingProp = members.single { it.javaField?.apply { isAccessible = true }?.get(result) == registered }
            registered.finalizeInit(matchingProp)
        }

        return registeredOptions to result
    }
}