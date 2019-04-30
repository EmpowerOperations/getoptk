package com.empowerops.getoptk

import java.util.*
import kotlin.reflect.full.cast

internal class ValueCreationVisitor(
        val errorReporter: ParseErrorReporter
) {

    val unconsumedOptions: Deque<MutableList<AbstractCommandLineOption<*>>> = LinkedList()

    fun visitEnter(cliNode: CLINode) {
        errorReporter.enterScope(cliNode.commandName, cliNode.opts)

        unconsumedOptions.push(cliNode.opts.toMutableList())
    }

    fun visitLeave(cliNode: CLINode) {

        val config = cliNode.config?.let { subCommand ->
            unconsumedOptions.peek() -= subCommand as AbstractCommandLineOption<*>
            val instance: Any? = subCommand.optionType.cast(subCommand.resolvedCommand)
            subCommand._value = Value(instance) as ValueStrategy<Nothing>
        }

        val missingOptions = unconsumedOptions.peek().filter { it.isRequired }
        for(missingOption in missingOptions){
            val message = "No value for required option '${missingOption.toPropertyDescriptor()}'"
            errorReporter.reportParsingProblem(cliNode.lastToken, message)
        }
        errorReporter.exitScope()
    }

    fun visitEnter(optionNode: BooleanOptionNode) {}
    fun visitLeave(optionNode: BooleanOptionNode) {
        val config = optionNode.config ?: return
        unconsumedOptions.peek() -= config

        if(config.isHelp){
            errorReporter.printUsage()
        }

        config._value = when(config.interpretation){
            FlagInterpretation.FLAG_IS_TRUE -> Value(true)
            FlagInterpretation.FLAG_IS_FALSE -> Value(false)
        }
    }

    fun visitEnter(optionNode: ValueOptionNode) {}
    fun visitLeave(optionNode: ValueOptionNode) {
        val config = optionNode.config ?: return
        val configAsAbstract = config as AbstractCommandLineOption<*>
        unconsumedOptions.peek() -= configAsAbstract
        
        val argumentNode = optionNode.argumentNode.children.single() as? ArgumentNode ?: return

        val argToken = argumentNode.valueToken
        val converted = config.converter.tryConvert(configAsAbstract, argToken)

        config._value = Value(converted)
    }

    fun visitEnter(optionNode: ObjectOptionNode) {}
    fun visitLeave(optionNode: ObjectOptionNode) {
        val config = optionNode.config ?: return
        val configAsAbstract = config as AbstractCommandLineOption<*>
        unconsumedOptions.peek() -= configAsAbstract

        val argumentNodes = optionNode.arguments.children.filterIsInstance<ArgumentNode>()

        val factory = config.factoryOrErrors as UnrolledAndUntypedFactory<*>
        val converted = factory.tryMake(configAsAbstract, argumentNodes.map { it.valueToken })

        config._value = Value(converted)
    }

    fun visitEnter(optionNode: ListOptionNode) {}
    fun visitLeave(optionNode: ListOptionNode) {
        val config = optionNode.config ?: return
        val configAsAbstract = config as AbstractCommandLineOption<*>
        unconsumedOptions.peek() -= configAsAbstract
        
        val parseMode = config.parseMode
        val argumentTokens = optionNode.arguments.children.filterIsInstance<ArgumentNode>().map { it.valueToken }

        val instances: List<Any?> = when(parseMode) {
            is CSV -> argumentTokens.map { parseMode.elementConverter.tryConvert(config, it) }
            is Varargs -> argumentTokens.map { parseMode.elementConverter.tryConvert(config, it) }
            is ImplicitObjects -> {
                val factory = config.factoryOrErrors!! as UnrolledAndUntypedFactory<*>

                var instances: List<Any?> = emptyList()
                for(i in 0 .. argumentTokens.size-1 step factory.arity) {
                    val args = argumentTokens.slice(i .. i + factory.arity - 1)
                    instances += factory.tryMake(config, args)
                }

                instances
            }
        }

        //note: I might have polluted this list with nulls for a not-nullable type.
        // TODO: some kind of flag to return early?
        config._value = @Suppress("UNCHECKED_CAST") (Value(instances) as ValueStrategy<Nothing>)
    }

    fun visitEnter(argumentNode: ArgumentNode) {}
    fun visitLeave(argumentNode: ArgumentNode) {}

    fun visitEnter(argumentNode: ArgumentListNode) {}
    fun visitLeave(argumentNode: ArgumentListNode) {}

    fun visitEnter(errorNode: ErrorNode) {}
    fun visitLeave(errorNode: ErrorNode) {}

    private fun reportError(token: Token?, exception: Exception?, config: AbstractCommandLineOption<*>){

        // I really cant stand the idea of catching debugging-oriented errors,
        // so I'm going to throw them immediately.
        if(exception is NullPointerException){
            throw exception
        }
        else {
            val message = "Failed to parse value" + if (config != null) " for "+config.toPropertyDescriptor() else ""
            errorReporter.reportParsingProblem(token, message, exception = exception)
        }
    }
    
    fun <T> Converter<T>.tryConvert(config: AbstractCommandLineOption<T>, token: Token): Any?
            = try { this.invoke(token.text) }
            catch(ex: Exception) { reportError(token, ex, config); null }

    fun <T> UnrolledAndUntypedFactory<T>.tryMake(config: AbstractCommandLineOption<T>, args: List<Token>): Any?
            = try { this.make(args.map { it.text } ) }
            catch(ex: Exception) {
                val token = (ex as? FactoryCreateFailed)?.index?.let { args[it] } ?: args.first()
                val exActual = ((ex as? FactoryCreateFailed)?.cause ?: ex) as Exception
                reportError(token, exActual, config)
            }
}