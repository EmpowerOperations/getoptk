package com.empowerops.getoptk

import com.google.common.collect.HashMultimap
import com.google.common.collect.Multimap
import org.stringtemplate.v4.Interpreter
import org.stringtemplate.v4.ST
import org.stringtemplate.v4.STGroupFile
import org.stringtemplate.v4.misc.ObjectModelAdaptor
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KClass
import kotlin.reflect.KProperty
import kotlin.reflect.jvm.internal.impl.utils.StringsKt

/**
 * Created by Geoff on 2016-10-26.
 */

// base class(ish) for JCommander-style "object with parsed arguments".
// in this sense I figured it was easier to simply require the object to have a `getArgs`
// than use some kind of reflective set call or factory or anything else.
/**
 * Marker interface for a class which is the result of parsing CLI arguments.
 */
abstract class CLI {

    //TODO: this should use reference equality on the CLI object.
    internal var optionProperties: List<CommandLineOption<*>> = emptyList()
    internal val errorReporter = ConfigErrorReporter()

    companion object {

        //so the behaviour here is wierd if hostFactory returns an already initialized instance
        //do I want to commit to this flow & add error handling or do we want to use another scheme?

        fun <T: CLI> parse(programName: String, args: Array<String>, outputStream: Appendable = System.out, hostFactory: () -> T): T
                = parse(programName, args.asIterable(), outputStream, hostFactory)

        fun <T: CLI> parse(programName: String, args: Iterable<String>, outputStream: Appendable = System.out, hostFactory: () -> T): T {

            val cmd = hostFactory()

            var opts: List<CommandLineOption<*>> = cmd.optionProperties

            if(opts.filterIsInstance<BooleanOptionConfiguration>().all { ! it.isHelp }) {
                opts += makeHelpOption(opts).apply { applyAdditionalConfiguration(cmd, prop = null) }
            }

            val configErrors = cmd.errorReporter.configurationErrors

            if(configErrors.any()){
                throw ConfigurationException(configErrors)
            }

            val tokens = Lexer.lex(args)

            val parseErrorReporter = ParseErrorReporter(programName, tokens)

            val parser = Parser(parseErrorReporter, opts)

            val root = parser.parseCLI(tokens)

            root.accept(ValueCreationVisitor(parseErrorReporter))

            if(parseErrorReporter.requestedHelp){
                throw HelpException(makeHelpMessage(programName, cmd.optionProperties))
            }
            if(parseErrorReporter.parsingProblems.any()){
                throw ParseFailedException(parseErrorReporter.parsingProblems, parseErrorReporter.firstException)
            }

            return cmd
        }

        private fun makeHelpMessage(programName: String, opts: List<CommandLineOption<*>>): String{
            val stg = STGroupFile("com/empowerops/getoptk/HelpMessage.stg", "UTF-8").apply {
                registerModelAdaptor(String::class.java, StringExtensionFunctionsAdapter)
                registerModelAdaptor(CommandLineOption::class.java, OptionExtensionFunctionsAdapter)
            }


            val st = stg.getInstanceOf("helpMessage").apply {
                add("programName", programName)
                add("options", opts)
            }

            val result = st.render(80)

            return result
        }

        object OptionExtensionFunctionsAdapter: ObjectModelAdaptor(){
            override fun getProperty(interp: Interpreter?, self: ST, o: Any, property: Any, propertyName: String): Any? {
                return if(o is CommandLineOption<*> && property == "fillTo30") with(o) {
                    if(shortName == "" || longName == "" || ! hasArgument) TODO()
                    val length = 30 - 1 - 1 - shortName.length - 1 - 2 - longName.length - 1 - 1 - argumentTypeDescription.length - 1
                    return (0 until length).joinToString(separator = "") { " " }
                }
                else super.getProperty(interp, self, o, property, propertyName)
            }
        }
        object StringExtensionFunctionsAdapter: ObjectModelAdaptor(){
            override fun getProperty(interp: Interpreter?, self: ST, o: Any, property: Any, propertyName: String): Any? {
                return when(property) {
                    "chars" -> object : AbstractList<Char>() {
                        override val size = (o as String).length
                        override fun get(index: Int) = (o as String).get(index)
                    }
                    "wordsAndSpaces" -> {
                        val allWords = (o as String).split(" ")
                        return allWords.map { "$it " }.dropLast(1) + allWords.last()
                    }
                    else -> super.getProperty(interp, self, o, property, propertyName)
                }
            }
        }
    }
}

/**
 * Parses the reciever string array (typically `args`) into the CLI instance
 * provided by calling the host factory
 */
@Throws(ConfigurationException::class, ParseFailedException::class)

fun <T: CLI> Array<String>.parsedAs(programName: String, outputStream: Appendable = System.out, hostFactory: () -> T): T
        = CLI.parse(programName, this.asIterable(), outputStream, hostFactory)

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

