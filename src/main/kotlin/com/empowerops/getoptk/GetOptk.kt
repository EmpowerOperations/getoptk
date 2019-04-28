package com.empowerops.getoptk

import org.stringtemplate.v4.Interpreter
import org.stringtemplate.v4.ST
import org.stringtemplate.v4.STGroup
import org.stringtemplate.v4.STGroupFile
import org.stringtemplate.v4.misc.ObjectModelAdaptor
import kotlin.reflect.KClass

/**
 * Created by Geoff on 2016-10-26.
 */

// base class(ish) for JCommander-style "object with parsed arguments".
// in this sense I figured it was easier to simply require the object to have a `getArgs`
// than use some kind of reflective set call or factory or anything else.
abstract class CLI {

    internal var errorReporter = ConfigErrorReporter()

    //TODO: this should use reference equality on the CLI object.
    internal var optionProperties: List<AbstractCommandLineOption<*>> = emptyList()

    override fun equals(other: Any?): Boolean {
        if (other !is CLI) return false

        if (optionProperties != other.optionProperties) return false

        if (optionProperties.map { it._value } != other.optionProperties.map { it._value } ) return false

        return true
    }

    override fun hashCode(): Int {
        var result = optionProperties.hashCode()
        result = 31 * result + optionProperties.map { it._value }.hashCode()
        return result
    }

    override fun toString(): String {
        return "${javaClass.simpleName}(${optionProperties.joinToString(", ") { it.toKeyValueString() }})"
    }

    companion object {

        //so the behaviour here is wierd if hostFactory returns an already initialized instance
        //do I want to commit to this flow & add error handling or do we want to use another scheme?
        // where T: new() is pretty nifty...

        fun <T: CLI> parse(
                programName: String,
                args: Array<String>,
                altHandler: (SpecialCaseInterpretation) -> Unit = ::throwSpecialCase,
                hostFactory: () -> T
        ): T
                = parse(programName, args.asIterable(), altHandler, hostFactory)

        fun <T: CLI> parse(
                programName: String,
                args: Iterable<String>,
                altHandler: (SpecialCaseInterpretation) -> Unit = ::throwSpecialCase,
                hostFactory: () -> T
        ): T {

            val cmd = hostFactory()

            var opts: List<AbstractCommandLineOption<*>> = cmd.optionProperties

            if(opts.filterIsInstance<BooleanOptionConfiguration>().all { ! it.isHelp }) {
                opts += makeHelpOption(opts).apply { applyAdditionalConfiguration(cmd, prop = null) }
            }

            val configErrors = cmd.errorReporter.configurationErrors

            if(configErrors.any()){
                altHandler(ConfigurationFailure(configErrors))
            }

            val tokens = Lexer.lex(args)

            val parseErrorReporter = ParseErrorReporter(programName, tokens)

            val parser = Parser(parseErrorReporter, opts)

            val root = parser.parseCLI(tokens, programName)

            val visitor = ValueCreationVisitor(opts, parseErrorReporter, programName, opts)
            root.accept(visitor)

            if(parseErrorReporter.usages.any()){
                altHandler(HelpRequested(parseErrorReporter.usages))
            }
            if(parseErrorReporter.parsingProblems.any()){
                altHandler(ParseFailure(parseErrorReporter.parsingProblems))
            }

            val requiredButNotSpecifiedOptions: List<AbstractCommandLineOption<*>> = visitor.unconsumedOptions.filter { it.isRequired }
            if(requiredButNotSpecifiedOptions.any()){
                TODO()
//                altHandler(MissingOptions(requiredButNotSpecifiedOptions, cmd.makeHelpMessage(programName)))
            }

            // uhh, if `altHandler` is a no-op, and we encounter all of the above errors,
            // are we sure this object is even consistent?
            return cmd
        }

        object OptionExtensionFunctionsAdapter: ObjectModelAdaptor(){
            override fun getProperty(interp: Interpreter?, self: ST, o: Any, property: Any, propertyName: String): Any? {
                return if(o is AbstractCommandLineOption<*> && property == "fillTo30") with(o) {
                    var length = 30
                    length -= 1
                    if(shortName != ""){
                        length -= (1 + shortName.length + 1)
                    }
                    if(longName != ""){
                        length -= (2 + longName.length)
                    }
                    if(hasArgument){
                        length -= (2 + argumentTypeDescription.length + 1)
                    }

//                    val length = 30 - 1 - 1 - shortName.length - 1 - 2 - longName.length - 1 - 1 - argumentTypeDescription.length - 1
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

abstract class Subcommand: CLI() {
    open val name: String = this::class.simpleName!!.toLowerCase()
}

/**
 * Parses the reciever string array (typically `args`) into the CLI instance
 * provided by calling the host factory
 */
@Throws(ConfigurationException::class, ParseFailedException::class)

fun <T: CLI> Array<String>.parsedAs(programName: String, altHandler: (SpecialCaseInterpretation) -> Unit = ::throwSpecialCase, hostFactory: () -> T): T
        = CLI.parse(programName, this.asIterable(), altHandler, hostFactory)

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

inline fun <reified T: Any> CLI.getNullableValueOpt(noinline spec: NullableValueOptionConfiguration<T>.() -> Unit = {})
        = getNullableValueOpt(this, spec, T::class)

/**
 * Defines a list option type from the command line.
 *
 * List options are expected to have a dynamic number of arguments,
 * either separated by spaces or by commas.
 */
inline fun <reified E: Any> CLI.getListOpt(noinline spec: ListOptionConfiguration<E>.() -> Unit = {})
        = getListOpt(this, spec, E::class)
//cant think of a use case for a NullableList since emptyList serves as an effective monadic-unit.


inline fun <reified C: Subcommand> CLI.getSubcommandOpt(noinline spec: SubcommandOptionConfiguration<C>.() -> Unit = {})
        = getSubcommandOpt(this, spec, C::class)

/**
 * Defines an object option type from the command line.
 */
inline fun <reified T: Any> CLI.getOpt(noinline spec: ObjectOptionConfiguration<T>.() -> Unit = {})
        = getOpt(this, spec, T::class)

inline fun <reified T: Any> CLI.getNullableOpt(noinline spec: NullableObjectOptionConfiguration<T>.() -> Unit = {})
        = getNullableOpt(this, spec, T::class)

fun <T: Any> getOpt(cli: CLI, spec: ObjectOptionConfiguration<T>.() -> Unit, objectType: KClass<T>): ObjectOptionConfiguration<T>
        = ObjectOptionConfigurationImpl(objectType, spec)

fun CLI.getFlagOpt(spec: BooleanOptionConfiguration.() -> Unit = {}): BooleanOptionConfiguration
        = BooleanOptionConfigurationImpl(spec)

fun <T: Any> getValueOpt(cli: CLI, spec: ValueOptionConfiguration<T>.() -> Unit, type: KClass<T>): ValueOptionConfiguration<T>
        = ValueOptionConfigurationImpl(type, spec)

fun <T: Any> getNullableValueOpt(cli: CLI, spec: NullableValueOptionConfiguration<T>.() -> Unit, type: KClass<T>): NullableValueOptionConfiguration<T>
        = NullableValueOptionConfigurationImpl(type, spec)

fun <T: Any> getListOpt(cli: CLI, spec: ListOptionConfiguration<T>.() -> Unit, elementType: KClass<T>): ListOptionConfiguration<T>
        = ListOptionConfigurationImpl(elementType, spec)

fun <T: Any> getNullableOpt(cli: CLI, spec: NullableObjectOptionConfiguration<T>.() -> Unit, objectType: KClass<T>): NullableObjectOptionConfiguration<T>
        = NullableObjectOptionConfigurationImpl(objectType, spec)

fun <T: Subcommand> getSubcommandOpt(cli: CLI, spec: SubcommandOptionConfiguration<T>.() -> Unit, objectType: KClass<T>): SubcommandOptionConfiguration<T> {
    return SubcommandOptionConfigurationImpl(cli, objectType, spec)
}

sealed class SpecialCaseInterpretation
data class ConfigurationFailure(val configurationProblems: List<ConfigurationProblem>) : SpecialCaseInterpretation()
data class ParseFailure(val parseProblems: List<ParseProblem>) : SpecialCaseInterpretation()
data class HelpRequested(val helpMessages: List<UsageRequest>) : SpecialCaseInterpretation()
data class MissingOptions(val missingOptions: List<CommandLineOption<*>>, val helpMessage: String): SpecialCaseInterpretation()

fun throwSpecialCase(specialCase: SpecialCaseInterpretation): Nothing = when(specialCase){
    is ConfigurationFailure -> throw ConfigurationException(specialCase)
    is ParseFailure -> throw ParseFailedException(specialCase)
    is HelpRequested -> throw HelpException(specialCase)
    is MissingOptions -> throw MissingOptionsException(specialCase)
}

internal fun makeHelpMessage(programName: String, optionProperties: List<AbstractCommandLineOption<*>>): String {
    //TODO: get this off STG such that we dont have the dep anymore
    // fruther its not really the right tool anyways.

    val stg = STGroupFile("com/empowerops/getoptk/HelpMessage.stg", "UTF-8").apply {
        registerModelAdaptor(String::class.java, CLI.Companion.StringExtensionFunctionsAdapter)
        registerModelAdaptor(AbstractCommandLineOption::class.java, CLI.Companion.OptionExtensionFunctionsAdapter)
    }

    val st = stg.getInstanceOf("helpMessage").apply {
        add("programName", programName)
        add("options", optionProperties)
    }

    val result = st.render(80)

    return result
}