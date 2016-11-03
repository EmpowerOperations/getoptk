package com.empowerops.getoptk

import com.google.common.collect.HashMultimap
import com.google.common.collect.Multimap
import kotlin.reflect.KClass
import kotlin.reflect.KProperty

/**
 * Created by Geoff on 2016-10-26.
 */


// base class(ish) for JCommander-style "object with parsed arguments".
// in this sense I figured it was easier to simply require the object to have a `getArgs`
// than use some kind of reflective set call or factory or anything else.
interface CLI {

    companion object {

        //so the behaviour here is wierd if hostFactory returns an already initialized instance
        //do I want to commit to this flow & add error handling or do we want to use another scheme?

        fun <T: CLI> parse(programName: String, args: Array<String>, hostFactory: () -> T): T
                = Parser.parse(programName, args.asIterable(), hostFactory)

        fun <T: CLI> parse(programName: String, args: Iterable<String>, hostFactory: () -> T): T
                = Parser.parse(programName, args, hostFactory)
    }

    interface LocalRegistration{
        var registry: List<OptionParser>
    }
}

fun <T: CLI> Array<String>.parsedAs(programName: String, hostFactory: () -> T): T = CLI.parse(programName, this, hostFactory)

inline fun <reified T: Any> CLI.getValueOpt(noinline spec: ValueOptionConfiguration<T>.() -> Unit = {})
        = getValueOpt(this, spec, T::class)

inline fun <reified E: Any> CLI.getListOpt(noinline spec: ListOptionConfiguration<E>.() -> Unit = {})
        = getListOpt(this, spec, E::class)

inline fun <reified T: Any> CLI.getOpt(noinline spec: ObjectOptionConfiguration<T>.() -> Unit = {})
        = getOpt(this, spec, T::class)

fun CLI.getFlagOpt(spec: BooleanOptionConfiguration.() -> Unit = {})
        = BooleanOptionConfiguration(ConfigErrorReporter.Default, spec).registeredTo(this)

fun <T: Any> getValueOpt(cli: CLI, spec: ValueOptionConfiguration<T>.() -> Unit, type: KClass<T>)
        = ValueOptionConfiguration(type, Converters(ConfigErrorReporter.Default), ConfigErrorReporter.Default, spec).registeredTo(cli)

fun <T: Any> getListOpt(cli: CLI, spec: ListOptionConfiguration<T>.() -> Unit, elementType: KClass<T>)
        = ListOptionConfiguration(elementType, Converters(ConfigErrorReporter.Default), ConfigErrorReporter.Default, spec).registeredTo(cli)

fun <T: Any> getOpt(cli: CLI, spec: ObjectOptionConfiguration<T>.() -> Unit, objectType: KClass<T>)
        = ObjectOptionConfiguration(objectType, Converters(ConfigErrorReporter.Default), ConfigErrorReporter.Default, spec).registeredTo(cli)


private fun <T: OptionParser> T.registeredTo(cli: CLI): T = apply {
    when(cli){
        is CLI.LocalRegistration -> cli.registry += this
        else -> RegisteredOptions.optionProperties += cli to this
    }
}

