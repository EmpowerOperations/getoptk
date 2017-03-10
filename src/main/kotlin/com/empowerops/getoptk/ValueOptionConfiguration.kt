package com.empowerops.getoptk

import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

interface ValueOptionConfiguration<T: Any>: ReadOnlyProperty<CLI, T> {
    var converter: Converter<T>
    var shortName: String
    var longName: String
    var description: String
}
