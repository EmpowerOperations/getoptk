package com.empowerops.getoptk

import kotlin.reflect.KProperty

class BooleanOptionConfiguration(source: CLI): CommandLineOption<Boolean> {

    init { RegisteredOptions.optionProperties += source to this }

    override var description: String = ""

    //problem: how do we express "compact" form (eg tar -xfvj)?
    override var names: List<String> = CommandLineOption.INFER_NAMES

    // problem: worth allowing a user to specify a custom parsing mode?
    // dont think so.

    operator fun getValue(self: CLI, property: KProperty<*>): Boolean = TODO();
}