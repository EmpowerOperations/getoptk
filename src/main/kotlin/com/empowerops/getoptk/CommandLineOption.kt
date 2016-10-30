package com.empowerops.getoptk

import kotlin.properties.ReadOnlyProperty

// this is really a marker interface, I put these members on it because I could,
// but really it only exists for the implementation detail mentioned blow about `Map<CLI,
interface CommandLineOption<out T: Any>: ReadOnlyProperty<CLI, T> {
    val description: String
    val shortName: String
    val longName: String
}

fun CommandLineOption<*>.names() = listOf(shortName, longName)

