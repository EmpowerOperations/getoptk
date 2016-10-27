package com.empowerops.getoptk

import com.google.common.collect.HashMultimap
import com.google.common.collect.Multimap
import kotlin.properties.ReadOnlyProperty
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KClass
import kotlin.reflect.KProperty
import kotlin.reflect.jvm.javaField

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

//            val ast = ANTLR.buildAST(args, opts)
//            ast.walk(OptionUpdatingWalker(opts))

//            CakeParser.buildAST(args, opts)

            val tokens = Lexer.lex(args.asIterable())
//            val parsedOpts = parse(tokens, opts)

            val newTokens = (opts.first() as ReflectivelyInitialized).reduce(tokens)

            return result;
        }

        internal fun lex(args: Array<String>){

        }

        internal fun <T: CLI> captureRegisteredOpts(hostFactory: () -> T): Pair<List<CommandLineOption<*>>, T>{
            RegisteredOptions.optionProperties = HashMultimap.create()
            val result = hostFactory()

            val members = result.javaClass.kotlin.members.filterIsInstance<KProperty<*>>()
            val registeredOptions = RegisteredOptions.optionProperties[result]!!

            //TODO: sort to allow deterministic hierarchy of duplicate-avoidance scheme
            // in other words, if you have two properties that both start with 'h', what does -h mean?
            // well the alphabetically-first one gets -h, the second gets -hwhatevs
            for(registered in registeredOptions){

                //hmm, seems like there is no way to avoid SecurityExceptions if we want parsing to be eager
                // in other words, its probably a better idea to stick to the lazy parsing idea...
                val matchingProp = members.single { it.javaField?.apply { isAccessible = true }?.get(result) == registered }
                registered.finalizeInit(matchingProp)
            }

            return registeredOptions.map { it as CommandLineOption<*> } to result
        }

    }
}

fun <T: CLI> Array<String>.parsedAs(hostFactory: () -> T): T = CLI.parse(this, hostFactory)


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

internal interface ReflectivelyInitialized {
    fun finalizeInit(hostingProperty: KProperty<*>)
    fun reduce(tokens: List<Token>): List<Token>
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
    var optionProperties: Multimap<CLI, ReflectivelyInitialized> = HashMultimap.create()
}


operator fun <K, V> Multimap<K, V>.plusAssign(pair: Pair<K, V>) { this.put(pair.first, pair.second) }