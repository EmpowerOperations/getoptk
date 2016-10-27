package com.empowerops.getoptk

import kotlin.reflect.KClass

/**
 * Created by Geoff on 2016-10-26.
 */


// base class(ish) for JCommander-style "object with parsed arguments".
// in this sense I figured it was easier to simply require the object to have a `getArgs`
// than use some kind of reflective set call or factory or anything else.
interface CLI {

    companion object {
        fun <T: CLI> parse(args: Array<String>, hostFactory: () -> T): T
                = Parser.parse(args, hostFactory)
    }
}

fun <T: CLI> Array<String>.parsedAs(hostFactory: () -> T): T = CLI.parse(this, hostFactory)


inline fun <reified T: Any> CLI.getOpt(noinline spec: ValueOptionConfiguration<T>.() -> Unit = {}): ValueOptionConfiguration<T>
        = getOpt(this, spec, T::class)

fun <T: Any> getOpt(cli: CLI, spec: ValueOptionConfiguration<T>.() -> Unit, type: KClass<T>): ValueOptionConfiguration<T>
        = ValueOptionConfiguration(cli, type).apply(spec)

fun CLI.getFlagOpt(spec: BooleanOptionConfiguration.() -> Unit = {}): BooleanOptionConfiguration
        = BooleanOptionConfiguration(this).apply(spec)

inline fun <reified E: Any> CLI.getListOpt(noinline spec: ListOptionConfiguration<E>.() -> Unit = {}): ListOptionConfiguration<E>
        = getListOpt(this, spec, E::class)

fun <T: Any> getListOpt(cli: CLI, spec: ListOptionConfiguration<T>.() -> Unit, elementType: KClass<T>): ListOptionConfiguration<T>
        = ListOptionConfiguration(cli, elementType).apply(spec)

