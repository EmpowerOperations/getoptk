package com.empowerops.getoptk

import kotlin.properties.ReadOnlyProperty

// this is really a marker interface, I put these members on it because I could,
// but really it only exists for the implementation detail mentioned blow about `Map<CLI,
interface CommandLineOption<out T: Any>: ReadOnlyProperty<CLI, T> {
    val description: String
    val shortName: String
    val longName: String
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
