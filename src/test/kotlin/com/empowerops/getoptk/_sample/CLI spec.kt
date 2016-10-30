package com.empowerops.getoptk._sample

import kotlin.reflect.KClass
import kotlin.reflect.KProperty

/**
 * Created by Geoff on 2016-10-15.
 */


//region client code


class Impl(override val args: Array<String>) : CLI {

    val isSpecial by getFlagOpt()

    val things: List<DomainModel> by getListOpt { parseMode = ParseMode.CSV }

    val moreThings: List<DomainModel> by getListOpt { parseMode = ParseMode.iteratively; arity = 2 }

    val complexThing: ComplexDomainModel by getOpt {
        parser = { ComplexDomainModel.parse(it) }
        names = listOf("-c", "--copT")
    }
}

class DomainModel {}
class ComplexDomainModel {

    companion object {
        fun parse(it: String): ComplexDomainModel = TODO()
    }
}

//endregion

//region library code

// base class(ish) for JCommander-style "object with parsed arguments".
// in this sense I figured it was easier to simply require the object to have a `getArgs`
// than use some kind of reflective set call or factory or anything else.
interface CLI {
    val args: Array<String>
}

// this is really a marker interface, I put these members on it because I could,
// but really it only exists for the implementation detail mentioned blow about `Map<CLI,
interface CommandLineOption<T : Any> {
    val description: String
    val names: List<String>

    // so, kotlin supplies us a KProperty, which we might assume follows some conventions.
    // If we did, we can infer 'val sigma: Int by getOpt()`to automatic names, like listOf("--sigma", "-s")
    // this has the added benefit of being more-refactor safe than traditional CLI parsers
    // though, it means its easier to introduce breaking changes.
    // (ie, after a user renames "sigma" to "alpha", a script with `prog --sigma` wont work)
    object INFER_NAMES : List<Nothing> by emptyList()
}


//note: the three below classes could almost certainly be combined polymorphically to spread the number of "duplicate"
// fields, but my core principal here is "ctrl + click"able-ness.
// If i have to repeat some documentation and some `var` statements, so be it.

class ValueCommandLineOption<T : Any>(source: CLI, optionType: KClass<T>
) : CommandLineOption<T> {

    init {
        RegisteredOptions.optionProperties += source to this
    }

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

    operator fun getValue(self: CLI, property: KProperty<*>): T {
        TODO()
    }

}

class ListCommandLineOption<T : List<*>>(source: CLI, optionType: KClass<T>)

: CommandLineOption<T> {

    init {
        RegisteredOptions.optionProperties += source to this
    }

    //name change to avoid confusion, user might want a "parse the whole value as a list"
    var elementParser: (String) -> T = Parsers.getDefaultFor(optionType)

    override var description: String = ""
    override var names: List<String> = CommandLineOption.INFER_NAMES

    //can this always be inferred?
    //should we support `prog --list 1 2 3` as being varags for a list with 3 elements?
    //what if you want a list of multi-arity (the obvious example being a map) then you get --list "hello" 1 "world" 2.
    var arity: Int = 1

    var parseMode: ParseMode = ParseMode.CSV

    operator fun getValue(self: CLI, property: KProperty<*>): T {
        TODO()
    }

}

class BooleanCommandLineOption(source: CLI) : CommandLineOption<Boolean> {

    init {
        RegisteredOptions.optionProperties += source to this
    }

    override var description: String = ""

    //problem: how do we express "compact" form (eg tar -xfvj)?
    override var names: List<String> = CommandLineOption.INFER_NAMES

    // problem: worth allowing a user to specify a custom parsing mode?
    // dont think so.

    operator fun getValue(self: CLI, property: KProperty<*>): Boolean = TODO();
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


// library-user facing. Idomatic use included in CLIClient.kt
fun <T : Any> CLI.getOpt(spec: ValueCommandLineOption<T>.() -> Unit = {}): ValueCommandLineOption<T> = TODO()

fun CLI.getFlagOpt(spec: BooleanCommandLineOption.() -> Unit = {}): BooleanCommandLineOption = TODO()
fun <E, T : List<E>> CLI.getListOpt(spec: ListCommandLineOption<T>.() -> Unit = {}): ListCommandLineOption<T> = TODO()

// this is an implementation detail, but basically if we want "eager" parsing of the CLI
// --which we need if we want to do context sensitive parsing
// then we need a flat list of the user specified options eagerly.
// in this sense I'm using this as a static object to keep that list.
// Thread safety is now an issue with this implementation.
internal object RegisteredOptions {
    //to solve thread-safety... some kind of atomically updating map? += on immutable maps probably wont do it.
    // also, should be a WeakHashMap or Map<WeakReference<CLI..., probably.
    // attempting to maintain that nice eager parsing property when KProperty is lazy is going to result in some odd code.
    var optionProperties: Map<CLI, CommandLineOption<*>> = emptyMap()
}

//looks up strategies to convert strings to T's, eg "Double.parseDouble", "Boolean.parseBoolean", etc.
// please note this object returns a closed converter, which might be weird
// Could just as easily return a T instead of a (String) -> T
object Parsers {
    fun <T : Any> getDefaultFor(type: KClass<T>): (String) -> T = TODO()
}

//endregion