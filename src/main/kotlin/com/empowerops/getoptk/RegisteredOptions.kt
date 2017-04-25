package com.empowerops.getoptk

import com.google.common.collect.HashMultimap
import com.google.common.collect.Multimap

// this is an implementation detail, but basically if we want "eager" parsing of the CLI
// --which we need if we want to do context sensitive parsing
// then we need a flat list of the user specified options eagerly.
// in this sense I'm using this as a static object to keep that list.
// Thread safety is now an issue with this implementation.
internal object RegisteredOptions {
    //to solve thread-safety... some kind of atomically updating map? += on immutable maps probably wont do it.
    // also, should be a WeakHashMap or Map<WeakReference<CLI..., probably.
    // attempting to maintain that nice eager parsing property when KProperty is lazy is going to result in some odd code.

//    //TODO: this should use reference equality on the CLI object.
//    private val optionProperties: Multimap<CLI, CommandLineOption<*>> = HashMultimap.create()
//    private val optionConfigErrors: MutableMap<CLI, ConfigErrorReporter> = hashMapOf()
//
//    fun addOption(cli: CLI, option: CommandLineOption<*>){
//        optionProperties.put(cli, option)
//    }
//
//    fun getOptions(cli: CLI): List<CommandLineOption<*>>
//            = optionProperties[cli]?.toList() ?: emptyList()
//
//
//    fun getConfigErrorReporter(cli: CLI): ConfigErrorReporter
//            = optionConfigErrors.getIfAbsentPut(cli) { ConfigErrorReporter() }
}

operator fun <K, V> Multimap<K, V>.plusAssign(pair: Pair<K, V>) { this.put(pair.first, pair.second) }
