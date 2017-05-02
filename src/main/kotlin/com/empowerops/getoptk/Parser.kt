package com.empowerops.getoptk

import kotlin.coroutines.experimental.buildSequence


/**
 * simple recursive descent parser for command line
 *
 * note that because of the requirement that we dynamically lex tokens
 * as per the users configuration, this component does read the property
 * configuration and feed it back into the [Lexer] to determine some tokens.
 *
 * Parses according to the following (kdoc-friendly EBNF) table:
 *
 * symbol               -> production
 * -------------------- -> ----------
 * _start_              -> [parseCLI]
 * [parseCLI]           -> ( ([parseWindowsOption] OR [parseShortOption] OR [parseLongOption]) [SuperTokenSeparator])*
 * [parseWindowsOption] -> [WindowsPreamble] ([ShortOptionName] OR [LongOptionName]) [parseOptionsBackHalf]*
 * [parseShortOption]   -> [ShortPreamble] [ShortOptionName] [parseOptionsBackHalf]*
 * [parseLongOption]    -> [LongPreamble] [LongOptionName] [parseOptionsBackHalf]
 * [parseOptionsBackHalf]
 *   -> {config is ValueOptionConfiguration}?[SeparatorToken] [Argument]
 *   -> {config is ListOptionConfiguration}?[parseObjectArgList]|[parseVarargsList]|[parseCSVArgList]
 *   -> {config is ObjectOptionConfiguration)?[parseObjectArgList]
 */
internal class Parser(
        override val errorReporter: ParseErrorReporter,
        val componentCombinators: List<AbstractCommandLineOption<*>>
): ErrorReporting {

    companion object {
        val Preambles = """'-' or '--' or '/'"""
    }

    /** as per [Parser]:
     * [parseCLI] -> ( ([parseWindowsOption] OR [parseShortOption] OR [parseLongOption]) [SuperTokenSeparator])*
     **/
    fun parseCLI(tokens: List<Token>): CLIRootNode = analyzing(tokens) {

        var children = emptyList<ParseNode>()

        while(hasNext()){
            val newChild = when(peek()){
                is WindowsPreamble -> parseWindowsOption(rest())
                is ShortPreamble -> parseShortOption(rest())
                is LongPreamble -> parseLongOption(rest())
                else -> logAndRecover("expected $Preambles")
            }

            children += newChild
            expect<SuperTokenSeparator>()
        }

        return@analyzing CLIRootNode(children)
    }

    /** as per [Parser]:
     * [parseLongOption] -> [LongPreamble] [LongOptionName] [parseOptionsBackHalf]
     **/
    private fun parseLongOption(tokens: List<Token>): ParseNode = analyzing(tokens) {

        val preamble = expect<LongPreamble>()
        val optName = expect<LongOptionName>()

        val (config, argNodes) = parseOptionsBackHalf(optName, rest())

        return@analyzing makeOptionNode(preamble, optName, config, argNodes)
    }


    /** as per [Parser]:
     * [parseShortOption] -> [ShortPreamble] [ShortOptionName] [parseOptionsBackHalf]
     **/
    private fun parseShortOption(tokens: List<Token>): ParseNode = analyzing(tokens) {

        val preamble = expect<ShortPreamble>()
        val optName = expect<ShortOptionName>()

        val (config, argNodes) = parseOptionsBackHalf(optName, rest())

        return@analyzing makeOptionNode(preamble, optName, config, argNodes)
    }

    /** as per [Parser]:
     * [parseWindowsOption] -> [WindowsPreamble] ([ShortOptionName] OR [LongOptionName]) [parseOptionsBackHalf]
     **/
    fun parseWindowsOption(tokens: List<Token>): ParseNode = analyzing(tokens){

        val preamble = expect<WindowsPreamble>()

        var optName = next()

        optName = when (optName){
            is ShortOptionName -> optName
            is LongOptionName -> optName
            else -> return@analyzing logAndRecover("expected ${availableOptions(componentCombinators)}")
        }

        val (config, valueNode) = parseOptionsBackHalf(optName, rest())

        return@analyzing makeOptionNode(preamble, optName, config, valueNode)
    }

    /**[parseOptionsBackHalf]
     *   -> {config is ValueOptionConfiguration}?[SeparatorToken] [Argument]
     *   -> {config is ListOptionConfiguration}?[parseObjectArgList]|[parseVarargsList]|[parseCSVArgList]
     *   -> {config is ObjectOptionConfiguration)?[parseObjectArgList]
     **/
    private fun parseOptionsBackHalf(optName: Token, tokens: List<Token>)
            : Pair<AbstractCommandLineOption<*>?, ParseNode> = analyzing(tokens){

        val config = componentCombinators.firstOrNull { optionSpec ->
            when (optName as? OptionName) {
                is LongOptionName -> optName.text == optionSpec.longName
                is ShortOptionName -> optName.text == optionSpec.shortName
                null -> false
            }
        }

        val hasArgument = peek() is SeparatorToken && peek(1) is Argument

        val argumentList: ParseNode = when {
            config == null -> {
                val problemOpt = optName.text
                val available = componentCombinators.map { when(optName as? OptionName){
                    is ShortOptionName -> it.shortName
                    is LongOptionName -> it.longName
                    null -> "???"
                }}
                errorReporter.reportParsingProblem(optName, "unknown option '$problemOpt', expected ${available.qcs}")

                if(peek() is SuperTokenSeparator && peek(1) is Argument){
                    expect<SuperTokenSeparator>()
                    expect<Argument>()
                }
                ErrorNode
            }
            
            ! hasArgument -> ArgumentListNode(emptyList())

            else -> when(config){
                is BooleanOptionConfigurationImpl -> ArgumentListNode(emptyList())

                is ListOptionConfigurationImpl<*> -> when(config.parseMode){
                    is CSV -> parseCSVArgList(config, rest())
                    is Varargs -> parseVarargsList(config, rest())
                    is ImplicitObjects -> parseObjectArgList(config, config.factoryOrErrors as UnrolledAndUntypedFactory<*>, rest())
                }

                is ObjectOptionConfigurationImpl<*>, is NullableObjectOptionConfigurationImpl<*> -> {
                    parseObjectArgList(config, config.factoryOrErrors as UnrolledAndUntypedFactory<*>, rest())
                }
                is ValueOptionConfigurationImpl<*>, is NullableValueOptionConfigurationImpl<*> -> {
                    val separator = expect<SeparatorToken>()
                    val arg = expect<Argument>()

                    val argumentNode = ArgumentNode(separator, config, arg)
                    ArgumentListNode(listOf(argumentNode))
                }
            }
        }
        return@analyzing Pair(config, argumentList)
    }

    private fun makeOptionNode(
            preamble: Token,
            optName: Token,
            config: AbstractCommandLineOption<*>?,
            argumentList: ParseNode
    ): ParseNode = when (config) {
        is BooleanOptionConfigurationImpl -> BooleanOptionNode(         preamble, optName, config)
        is ValueOptionConfigurationImpl<*> -> ValueOptionNode(          preamble, optName, config, argumentList)
        is NullableValueOptionConfigurationImpl<*> -> ValueOptionNode(  preamble, optName, config, argumentList)
        is ListOptionConfigurationImpl<*> -> ListOptionNode(            preamble, optName, config, argumentList)
        is ObjectOptionConfigurationImpl<*> -> ObjectOptionNode(        preamble, optName, config, argumentList)
        is NullableObjectOptionConfigurationImpl<*> -> ObjectOptionNode(preamble, optName, config, argumentList)
        null -> ErrorNode.apply { errorReporter.internalError(optName, "expected $config to be a known type") }
    }

    fun parseCSVArgList(config: ListOptionConfigurationImpl<*>, tokens: List<Token>): ParseNode = analyzing(tokens){

        // so I was thinking about trying to employ a fancy re-lexing strategy here,
        // but im not sure what the advantage is
        val separator = expect<SuperTokenSeparator>()
        val argument = expect<Argument>()

        val values = argument.text.split(',')
        var args: List<Argument> = emptyList()
        var separators: List<Token> = listOf(separator)

        for((index, value) in values.withIndex()){
            val newTokenIndex = argument.location.start + values.take(index).sumBy { it.length } + index
            if(index != 0) separators += MinorSeparator(newTokenIndex - ",".length)
            args += Argument(value, newTokenIndex)
        }

        val nodes = separators.zip(args) { sep, arg -> ArgumentNode(sep, config, arg) }
        
        return@analyzing ArgumentListNode(nodes)
    }
    
    fun parseObjectArgList(
            spec: AbstractCommandLineOption<*>,
            factory: UnrolledAndUntypedFactory<*>,
            tokens: List<Token>
    ): ParseNode = analyzing(tokens){                

        //.asSequence().groupByIndexed { idx / 2 }.values().takeWhile { left is STS && right is Arg }...

        var argList: List<ArgumentNode> = emptyList()
        var index = 0
        pushMark()

        while(peek() is SuperTokenSeparator && peek(1) is Argument){

            if(factory.argCount evenlyDivides index){
                popMarkedTokens()
                pushMark()
            }

            val separator = expect<SuperTokenSeparator>()
            val arg = expect<Argument>()

            argList += ArgumentNode(separator, spec, arg)

            index += 1
        }

        argList = when {
            factory.argCount evenlyDivides argList.size -> {
                popMarkedTokens()
                argList
            }
            else -> {
                popAndRevertToMark();
                argList.let { it.subList(0, it.size - (it.size % factory.argCount)) }
            }
        }

        return@analyzing ArgumentListNode(argList)
    }

    fun parseVarargsList(spec: AbstractCommandLineOption<*>, tokens: List<Token>): ArgumentListNode =  analyzing(tokens) {
        val nodes = buildSequence {
            while (peek() is SuperTokenSeparator && peek(1) is Argument) {
                val separator = expect<SuperTokenSeparator>()
                val arg = expect<Argument>()

                yield(ArgumentNode(separator, spec, arg))
            }
        }.toList()

        return@analyzing ArgumentListNode(nodes)
    }

    private fun availableOptions(componentCombinators: List<AbstractCommandLineOption<*>>): String {
        return componentCombinators.flatMap { it.names() }.joinToString("' or '", "'", "'")
    }

    private fun Marker.logAndRecover(message: String): ErrorNode {

        errorReporter.reportParsingProblem(peek(), message)

        while(peek(1) !is Epsilon && ! startsNewOption()) {
            next()
        }

        return ErrorNode
    }

    private fun Marker.startsNewOption(): Boolean {
        return peek() is SuperTokenSeparator && peek(1) is OptionPreambleToken
    }

    //quoted-comma-separated
    val List<String>.qcs: String get() = joinToString { "'$it'" }
}

