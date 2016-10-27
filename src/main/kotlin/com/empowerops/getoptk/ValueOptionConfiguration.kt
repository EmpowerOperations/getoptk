package com.empowerops.getoptk

import kotlin.reflect.KClass
import kotlin.reflect.KProperty

class ValueOptionConfiguration<T: Any>(source: CLI, optionType: KClass<T>
) : CommandLineOption<T> {

    init { RegisteredOptions.optionProperties += source to this }

    // Indicates the strategy to convert the "arg" in --opt arg into a T, defaults to things like "Double.ParseDouble" etc.
    // problem: multiple arity should bump this from a Funcion1 to a Funcion2 (ie `(String, String) -> T`).
    // How to do this elegantly?
    var parser: (String) -> T = Parsers.getDefaultFor(optionType)

    //description to be supplied by a --help
    override var description: String = ""

    //aliases that you can use to indicate this param at the command line
    // if you had 'var bigness: Double by getOpt { names = listOf("-b", "--big", "--BN") }
    // then you could write "yourProgram --big 4.0" or "yourProgram --BN 4.5"
    // problem: how to tolerate windows style (ie yourProgram.exe /big 4.0)?
    override var names: List<String> = CommandLineOption.INFER_NAMES

    //defines the number of values at the command line this thing has associated with it
    //for example, if you had a field that was `val thing: Pair<String, Double> by getOpt { arity = 2 }`
    // what you would probably want is for --thing "key" 4.567 to produce `thing == Pair("Key", 4.567)`
    // in this example you would change airty to '2'.
    var arity: Int = 1

    operator fun getValue(self: CLI, property: KProperty<*>): T = parser(Finder.find(self, property, names))
}

internal object Finder{

    fun find(self: CLI, property: KProperty<*>, names: List<String>): String {

        val names = buildNames(property, names)

        val indexOfName = names.map { self.args.indexOf(it) }.filter { it != -1 }.firstOrNull() ?: TODO()

        return self.args[indexOfName + 1]
    }

    private fun buildNames(property: KProperty<*>, names: List<String>): List<String> = when {
        names.any() -> names
        names === CommandLineOption.INFER_NAMES -> listOf("--${property.name}", "-${property.name[0]}")
        else -> TODO()
    }
}