package com.empowerops.getoptk

import org.stringtemplate.v4.Interpreter
import org.stringtemplate.v4.ST
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

    //TODO: this should use reference equality on the CLI object.
    internal var optionProperties: List<AbstractCommandLineOption<*>> = emptyList()
    internal val errorReporter = ConfigErrorReporter()

    //EQUALS logic TODO

    companion object {

        //so the behaviour here is wierd if hostFactory returns an already initialized instance
        //do I want to commit to this flow & add error handling or do we want to use another scheme?

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

            val root = parser.parseCLI(tokens)

            val visitor = ValueCreationVisitor(opts, parseErrorReporter).apply { root.accept(this) }

            if(parseErrorReporter.requestedHelp){
                val helpMessage = makeHelpMessage(programName, cmd.optionProperties)
                altHandler(HelpRequested(helpMessage))
            }
            if(parseErrorReporter.parsingProblems.any()){
                altHandler(ParseFailure(parseErrorReporter.parsingProblems))
            }

            val requiredButNotSpecifiedOptions: List<AbstractCommandLineOption<*>> = visitor.unconsumedOptions.filter { it.isRequired }
            if(requiredButNotSpecifiedOptions.any()){
                altHandler(MissingOptions(requiredButNotSpecifiedOptions))
            }

            return cmd
        }

        private fun makeHelpMessage(programName: String, opts: List<AbstractCommandLineOption<*>>): String{
            val stg = STGroupFile("com/empowerops/getoptk/HelpMessage.stg", "UTF-8").apply {
                registerModelAdaptor(String::class.java, StringExtensionFunctionsAdapter)
                registerModelAdaptor(AbstractCommandLineOption::class.java, OptionExtensionFunctionsAdapter)
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

/**
 * Defines an object option type from the command line.
 */
inline fun <reified T: Any> CLI.getOpt(noinline spec: ObjectOptionConfiguration<T>.() -> Unit = {})
        = getOpt(this, spec, T::class)

inline fun <reified T: Any> CLI.getNullableOpt(noinline spec: NullableObjectOptionConfiguration<T>.() -> Unit = {})
        = getNullableOpt(this, spec, T::class)

/**
 * Defines a boolean (flag) option type from the command line.
 */
fun CLI.getFlagOpt(spec: BooleanOptionConfiguration.() -> Unit = {}): BooleanOptionConfiguration
        = BooleanOptionConfigurationImpl(spec)

fun <T: Any> getValueOpt(cli: CLI, spec: ValueOptionConfiguration<T>.() -> Unit, type: KClass<T>): ValueOptionConfiguration<T>
        = ValueOptionConfigurationImpl(type, spec)

fun <T: Any> getNullableValueOpt(cli: CLI, spec: NullableValueOptionConfiguration<T>.() -> Unit, type: KClass<T>): NullableValueOptionConfiguration<T>
        = NullableValueOptionConfigurationImpl(type, spec)

fun <T: Any> getListOpt(cli: CLI, spec: ListOptionConfiguration<T>.() -> Unit, elementType: KClass<T>): ListOptionConfiguration<T>
        = ListOptionConfigurationImpl(elementType, spec)

fun <T: Any> getOpt(cli: CLI, spec: ObjectOptionConfiguration<T>.() -> Unit, objectType: KClass<T>): ObjectOptionConfiguration<T>
        = ObjectOptionConfigurationImpl(objectType, spec)

fun <T: Any> getNullableOpt(cli: CLI, spec: NullableObjectOptionConfiguration<T>.() -> Unit, objectType: KClass<T>): NullableObjectOptionConfiguration<T>
        = NullableObjectOptionConfigurationImpl(objectType, spec)

sealed class SpecialCaseInterpretation
data class ConfigurationFailure(val configurationProblems: List<ConfigurationProblem>) : SpecialCaseInterpretation()
data class ParseFailure(val parseProblems: List<ParseProblem>) : SpecialCaseInterpretation()
data class HelpRequested(val helpMessage: String) : SpecialCaseInterpretation()
data class MissingOptions(val missingOptions: List<CommandLineOption<*>>): SpecialCaseInterpretation()

fun throwSpecialCase(specialCase: SpecialCaseInterpretation): Nothing = when(specialCase){
    is ConfigurationFailure -> throw ConfigurationException(specialCase.configurationProblems)
    is ParseFailure -> throw ParseFailedException(
            specialCase.parseProblems.map { it.message },
            specialCase.parseProblems.firstOrNull { it.stackTrace != null}?.stackTrace
    )
    is HelpRequested -> throw HelpException(specialCase.helpMessage)
    is MissingOptions -> throw MissingOptionsException(specialCase.missingOptions)
}

fun ignoreUnrecognized(specialCase: SpecialCaseInterpretation): Unit = when(specialCase){
    is ConfigurationFailure -> throw ConfigurationException(specialCase.configurationProblems)
    is ParseFailure -> {
        val recognizedParseFailures = specialCase.parseProblems.filter { "unknown option " !in it.message }
        if(recognizedParseFailures.any()) throw ParseFailedException(
                recognizedParseFailures.map { it.message },
                recognizedParseFailures.firstOrNull { it.stackTrace != null }?.stackTrace
        )
        else Unit
    }
    is HelpRequested -> throw HelpException(specialCase.helpMessage)
    is MissingOptions -> throw MissingOptionsException(specialCase.missingOptions)
}
