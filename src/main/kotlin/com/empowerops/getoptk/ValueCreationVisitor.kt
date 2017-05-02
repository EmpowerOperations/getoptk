package com.empowerops.getoptk

internal class ValueCreationVisitor(val allOptions: List<AbstractCommandLineOption<*>>, val errorReporter: ParseErrorReporter) {

    var unconsumedOptions: List<AbstractCommandLineOption<*>> = allOptions
        private set

    fun visitEnter(cliNode: CLIRootNode) {}
    fun visitLeave(cliNode: CLIRootNode) {}

    fun visitEnter(optionNode: BooleanOptionNode) {}
    fun visitLeave(optionNode: BooleanOptionNode) {
        val config = optionNode.config ?: return
        unconsumedOptions -= config

        if(config.isHelp){
            errorReporter.requestedHelp = true
        }

        config._value = when(config.interpretation){
            FlagInterpretation.FLAG_IS_TRUE -> Value(true)
            FlagInterpretation.FLAG_IS_FALSE -> Value(false)
        }
    }

    fun visitEnter(optionNode: ValueOptionNode) {}
    fun visitLeave(optionNode: ValueOptionNode) {
        val config = optionNode.config ?: return
        unconsumedOptions -= (config as AbstractCommandLineOption<*>)
        
        val argumentNode = optionNode.argumentNode.children.single() as? ArgumentNode ?: return

        val argToken = argumentNode.valueToken
        val converted = config.converter.tryConvert(config, argToken)

        config._value = Value(converted)
    }

    fun visitEnter(optionNode: ObjectOptionNode) {}
    fun visitLeave(optionNode: ObjectOptionNode) {
        val config = optionNode.config ?: return
        unconsumedOptions -= (config as AbstractCommandLineOption<*>)

        val argumentNodes = optionNode.arguments.children.filterIsInstance<ArgumentNode>()

        val factory = config.factoryOrErrors as UnrolledAndUntypedFactory<*>
        val converted = factory.tryMake(config, argumentNodes.map { it.valueToken })

        config._value = Value(converted)
    }

    fun visitEnter(optionNode: ListOptionNode) {}
    fun visitLeave(optionNode: ListOptionNode) {
        val config = optionNode.config ?: return
        unconsumedOptions -= (config as AbstractCommandLineOption<*>)
        
        val parseMode = config.parseMode
        val argumentTokens = optionNode.arguments.children.filterIsInstance<ArgumentNode>().map { it.valueToken }

        val instances: List<Any?> = when(parseMode) {
            is CSV -> argumentTokens.map { parseMode.elementConverter.tryConvert(config, it) }
            is Varargs -> argumentTokens.map { parseMode.elementConverter.tryConvert(config, it) }
            is ImplicitObjects -> {
                val factory = config.factoryOrErrors!! as UnrolledAndUntypedFactory<*>

                var instances: List<Any?> = emptyList()
                for(i in 0 .. argumentTokens.size-1 step factory.argCount) {
                    val args = argumentTokens.slice(i .. i + factory.argCount - 1)
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

    private fun reportError(token: Token, exception: Exception, config: AbstractCommandLineOption<*>? = null){

        // I really cant stand the idea of catching debugging-oriented errors,
        // so I'm going to throw them immediately.
        if(exception is NullPointerException){
            throw exception
        }
        else {
            val message = "Failed to parse value" + if (config != null) " for "+config.toPropertyDescriptor() else ""
            errorReporter.reportParsingProblem(token, message, exception)
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