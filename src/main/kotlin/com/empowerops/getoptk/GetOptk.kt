package com.empowerops.getoptk

import kotlin.reflect.KClass
import kotlin.reflect.KProperty

/**
 * Created by Geoff on 2016-10-26.
 */


// base class(ish) for JCommander-style "object with parsed arguments".
// in this sense I figured it was easier to simply require the object to have a `getArgs`
// than use some kind of reflective set call or factory or anything else.
interface CLI {

    companion object {

        fun <T: CLI> parse(args: Array<String>, hostFactory: () -> T): T {

            val (opts, result) = captureRegisteredOpts(hostFactory)

            val ast = buildAST(args, opts)
            ast.walk(OptionUpdatingWalker(opts))

            return result;
        }

        internal fun <T> captureRegisteredOpts(hostFactory: () -> T): Pair<List<CommandLineOption<*>>, T>{
            TODO()
        }

        internal fun buildAST(args: Array<String>, opts: List<CommandLineOption<*>>):Nothing = TODO()


    }
}

fun <T: CLI> Array<String>.parsedAs(hostFactory: () -> T): T = CLI.parse(this, hostFactory)


// this is really a marker interface, I put these members on it because I could,
// but really it only exists for the implementation detail mentioned blow about `Map<CLI,
interface CommandLineOption<T: Any> {
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


inline fun <reified T: Any> CLI.getOpt(noinline spec: ValueOptionConfiguration<T>.() -> Unit = {}): ValueOptionConfiguration<T>
        = getOpt(this, spec, T::class)

fun <T: Any> getOpt(cli: CLI, spec: ValueOptionConfiguration<T>.() -> Unit, type: KClass<T>): ValueOptionConfiguration<T> {
    val optionConfig = ValueOptionConfiguration(cli, type)

    spec(optionConfig)

    return optionConfig
}

fun CLI.getFlagOpt(spec: BooleanOptionConfiguration.() -> Unit = {}): BooleanOptionConfiguration = TODO()
fun <E, T: List<E>> CLI.getListOpt(spec: ListOptionConfiguration<T>.() -> Unit = {}): ListOptionConfiguration<T> = TODO()

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
