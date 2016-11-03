package com.empowerops.getoptk

import org.stringtemplate.v4.ST
import org.stringtemplate.v4.STGroupFile
import kotlin.reflect.KProperty
import kotlin.reflect.jvm.javaField

object Parser {

    fun <T : CLI> parse(programName: String, args: Iterable<String>, hostFactory: () -> T): T {

        val configErrorReporter = ConfigErrorReporter.Default

        val (opts, result) = captureRegisteredOpts(hostFactory)

        validateNames(configErrorReporter, opts)

        if(configErrorReporter.configurationErrors.any()){
            throw ConfigurationException(configErrorReporter.configurationErrors)
        }

        var tokens = Lexer.lex(args)

        val parseErrorReporter = ParseErrorReporter(programName, tokens)
        opts.forEach { it.errorReporter = parseErrorReporter }

        val root = TopLevelParser(parseErrorReporter, opts)

        tokens = root.reduce(tokens)

        if (tokens.any()){ configErrorReporter.internalError(tokens.first(), "unconsumed tokens") }

        if(parseErrorReporter.parsingProblems.any()){
            printUsage()
            throw ParseFailedException(parseErrorReporter.parsingProblems)
        }

        return result
    }

    private fun printUsage() {

    }

    private fun validateNames(errorReporter: ConfigErrorReporter, opts: List<OptionParser>) {
        val optionNamePairs: Map<String, List<CommandLineOption<*>>> = opts
                .map { it as CommandLineOption<*> }
                .flatMap { opt -> listOf("-${opt.shortName}" to opt, "--${opt.longName}" to opt) }
                .groupBy { nameOptPair -> nameOptPair.first }
                .mapValues { it -> it.value.map { it.second } }

        for ((duplicateName, options) in optionNamePairs.filter { it.value.size >= 2 }) {
            val optionNames = options.sortedBy { it.longName /*for reproducability*/ }.map { it.toTokenGroupDescriptor() }
            errorReporter.reportConfigProblem("Name collision: $duplicateName maps to all of '${optionNames.joinToString("' and '")}'")
        }
    }

    internal fun <T : CLI> captureRegisteredOpts(hostFactory: () -> T): Pair<List<OptionParser>, T> {

        val cmd = hostFactory()

        val members = cmd.javaClass.kotlin.members.filterIsInstance<KProperty<*>>()

        val registeredOptions: List<OptionParser> = when(cmd){
            is CLI.LocalRegistration -> cmd.registry
            else -> RegisteredOptions.optionProperties[cmd].let {
                val result: List<OptionParser> = it.toList()
                it.clear()
                result
            }
        }

        for (registered in registeredOptions) {
            // pending https://youtrack.jetbrains.com/issue/KT-8384
            val matchingProp = members.single { it.javaField?.apply { isAccessible = true }?.get(cmd) == registered }
            registered.finalizeInit(matchingProp)
        }

        return registeredOptions to cmd
    }
}


class ConfigErrorReporter(){

    fun reportConfigProblem(message: String){
        configurationErrors += message
    }

    fun internalError(token: Token, errorMessage: String) {
        println("internal error at $token: $errorMessage")
    }

    fun debug(message: () -> String){
//        println("debug: ${message()}")
    }

    var configurationErrors: List<String> = emptyList()
        private set;

    companion object {
        var Default: ConfigErrorReporter = ConfigErrorReporter()
            internal set;

    }

    fun Token.toLocationString(): String = "'$text'"
}

class ParseErrorReporter(val programNamePrefix: String, val tokens: List<Token>) {

    val template = ST(
            """<error.commandLine>
              |at:<error.superposition>
              |<error.message>
              |<error.exception>
              """.trimMargin()
    )
    data class ErrorPresentation(val commandLine: String, val superposition: String, val message: String, val exception: Exception)

    fun reportParsingProblem(token: Token, message: String, exception: Exception = Exception()){
        val commandLine = programNamePrefix + " " + tokens.joinToString(separator = "") { it.text }

        val superposition = (" ".repeat(programNamePrefix.length + 1 - "at:".length + token.location.start)
                + "~".repeat(token.text.length)
                + " ".repeat(commandLine.length - token.location.endInclusive))

        val template = ST(template)
        template.add("error", ErrorPresentation(
                commandLine,
                superposition,
                message,
                exception
        ))
        val newMessage = template.render()
        parsingProblems += newMessage
    }

    fun debug(message: () -> String){
//        println("debug: ${message()}")
    }
    fun internalError(token: Token, errorMessage: String) {
        println("internal error at $token: $errorMessage")
    }

    var parsingProblems: List<String> = emptyList()
        private set;
}

class ConfigurationException(val messages: List<String>)
: Exception((listOf("CLI configuration errors:") + messages).joinToString("\n"))

class ParseFailedException(val messages: List<String>)
: Exception((listOf("Unrecognized option:") + messages).joinToString("\n"))