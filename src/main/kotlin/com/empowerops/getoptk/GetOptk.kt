package com.empowerops.getoptk

import com.google.common.collect.HashMultimap
import com.google.common.collect.Multimap
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KClass
import kotlin.reflect.KProperty

/**
 * Created by Geoff on 2016-10-26.
 */

// base class(ish) for JCommander-style "object with parsed arguments".
// in this sense I figured it was easier to simply require the object to have a `getArgs`
// than use some kind of reflective set call or factory or anything else.
/**
 * Marker interface for a class which is the result of parsing CLI arguments.
 */
interface CLI {

    companion object {

        //so the behaviour here is wierd if hostFactory returns an already initialized instance
        //do I want to commit to this flow & add error handling or do we want to use another scheme?

        fun <T: CLI> parse(programName: String, args: Array<String>, hostFactory: () -> T): T
                = parse(programName, args.asIterable(), hostFactory)

        fun <T: CLI> parse(programName: String, args: Iterable<String>, hostFactory: () -> T): T {

            val cmd = hostFactory()

            val opts = RegisteredOptions.getOptions(cmd)
            val configErrors = RegisteredOptions.getConfigErrorReporter(cmd).configurationErrors

            if(configErrors.any()){
                throw ConfigurationException(configErrors)
            }

            val tokens = Lexer.lex(args)

            val parseErrorReporter = ParseErrorReporter(programName, tokens)

            val parser = Parser(parseErrorReporter, opts)

            val root = parser.parseCLI(tokens)

            ValueCreationVisitor(parseErrorReporter).apply { root.accept(this) }

            if(parseErrorReporter.parsingProblems.any()){
                throw ParseFailedException(parseErrorReporter.parsingProblems)
            }

            return cmd
        }
    }
}

/**
 * Parses the reciever string array (typically `args`) into the CLI instance
 * provided by calling the host factory
 */
fun <T: CLI> Array<String>.parsedAs(programName: String, hostFactory: () -> T): T
        = CLI.parse(programName, this, hostFactory)

/**
 * Defines a value option type from the command line.
 *
 * Value options are preferable for simple types such as
 * `Double`'s, `Path`'s, `String`'s, etc.
 *
 * Note that you may provide a custom converter which will be called
 * with the argument supplied to the option.
 */
inline fun <reified T: Any> CLI.getValueOpt(noinline spec: ValueOptionConfiguration<T>.() -> Unit = {})
        = getValueOpt(this, spec, T::class)

/**
 * Defines a list option type from the command line.
 *
 * List options are expected to have a dynamic number of arguments,
 * either separated by spaces or by commas.
 */
inline fun <reified E: Any> CLI.getListOpt(noinline spec: ListOptionConfiguration<E>.() -> Unit = {})
        = getListOpt(this, spec, E::class)

/**
 * Defines an object option type from the command line.
 */
inline fun <reified T: Any> CLI.getOpt(noinline spec: ObjectOptionConfiguration<T>.() -> Unit = {})
        = getOpt(this, spec, T::class)

/**
 * Defines a boolean (flag) option type from the command line.
 */
fun CLI.getFlagOpt(spec: BooleanOptionConfiguration.() -> Unit = {}): BooleanOptionConfiguration
        = BooleanOptionConfigurationImpl(spec)

fun <T: Any> getValueOpt(cli: CLI, spec: ValueOptionConfiguration<T>.() -> Unit, type: KClass<T>): ValueOptionConfiguration<T>
        = ValueOptionConfigurationImpl(type, spec)

fun <T: Any> getListOpt(cli: CLI, spec: ListOptionConfiguration<T>.() -> Unit, elementType: KClass<T>): ListOptionConfiguration<T>
        = ListOptionConfigurationImpl(elementType, spec)

fun <T: Any> getOpt(cli: CLI, spec: ObjectOptionConfiguration<T>.() -> Unit, objectType: KClass<T>): ObjectOptionConfiguration<T>
        = ObjectOptionConfigurationImpl(objectType, spec)

fun <H, T: Any> ReadOnlyProperty<H, T?>.notNull(): ReadOnlyProperty<H, T> = NotNullDelegatingProperty(this)
fun <K, V> MutableMap<K, V>.getIfAbsentPut(key: K, ifAbsentFactory: () -> V): V {
    if ( ! containsKey(key)){
        val value = ifAbsentFactory()
        put(key, value)
        return value
    }
    else {
        return get(key)!!
    }
}

class NotNullDelegatingProperty<H, T: Any>(val delegate: ReadOnlyProperty<H, T?>): ReadOnlyProperty<H, T> {
    override fun getValue(thisRef: H, property: KProperty<*>): T = delegate.getValue(thisRef, property)!!
}

