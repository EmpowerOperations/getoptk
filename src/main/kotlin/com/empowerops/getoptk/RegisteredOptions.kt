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
    val optionProperties: Multimap<CLI, OptionParser> = HashMultimap.create()

}

operator fun <K, V> Multimap<K, V>.plusAssign(pair: Pair<K, V>) { this.put(pair.first, pair.second) }

data class Problem(val superToken: String, val description: String, val location: IntRange, val trace: Exception? = null)