package com.empowerops.getoptk

import kotlin.properties.ReadOnlyProperty

// this is really a marker interface, I put these members on it because I could,
// but really it only exists for the implementation detail mentioned blow about `Map<CLI,
interface CommandLineOption<out T: Any>: ReadOnlyProperty<CLI, T> {
    val description: String
    val names: List<String>

    // so, kotlin supplies us a KProperty, which we might assume follows some conventions.
    // If we did, we can infer 'val sigma: Int by getOpt()`to automatic names, like listOf("--sigma", "-s")
    // this has the added benefit of being more-refactor safe than traditional CLI parsers
    // though, it means its easier to introduce breaking changes.
    // (ie, after a user renames "sigma" to "alpha", a script with `prog --sigma` wont work)
    object INFER_NAMES: List<Nothing> by emptyList()
}

interface ParseMode {

    companion object {
        //indicate that a list arg is --list x,y,z
        val CSV: ParseMode = separator(",")

        //indicate that a list arg is --list x --list y --list z
        val iteratively: ParseMode = TODO()

        fun separator(separator: String): ParseMode = TODO()
    }
}
