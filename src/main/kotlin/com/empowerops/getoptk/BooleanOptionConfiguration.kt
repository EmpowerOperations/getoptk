package com.empowerops.getoptk

import kotlin.properties.ReadOnlyProperty

enum class FlagInterpretation { FLAG_IS_TRUE, FLAG_IS_FALSE }

interface BooleanOptionConfiguration: ReadOnlyProperty<CLI, Boolean> {

    var interpretation: FlagInterpretation

    var description: String

    var longName: String
    var shortName: String
}

