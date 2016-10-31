package com.empowerops.getoptk

import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

// this is really a marker interface, I put these members on it because I could,
// but really it only exists for the implementation detail mentioned blow about `Map<CLI,
interface CommandLineOption<out T: Any>: ReadOnlyProperty<CLI, T> {
    val description: String
    val shortName: String
    val longName: String
}

fun CommandLineOption<*>.names() = listOf(shortName, longName)

fun <H, T: Any> ReadOnlyProperty<H, T?>.notNull(): ReadOnlyProperty<H, T> = NotNullDelegatingProperty(this)

class NotNullDelegatingProperty<H, T: Any>(val delegate: ReadOnlyProperty<H, T?>): ReadOnlyProperty<H, T>{
    override fun getValue(thisRef: H, property: KProperty<*>): T = delegate.getValue(thisRef, property)!!
}

