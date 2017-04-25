package com.empowerops.getoptk

import com.sun.org.apache.xpath.internal.Arg
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
 *   -> [SeparatorToken] [Argument]
 *   -> ([SuperTokenSeparator] [Argument])*
 */
internal class Parser(
        override val errorReporter: ParseErrorReporter,
        val componentCombinators: List<CommandLineOption<*>>
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
                else -> { logAndRecover("expected $Preambles") }
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

    /**
     * [parseOptionsBackHalf]
     *   -> {config is ValueOptionConfiguration}?[SeparatorToken] [Argument]
     *   -> {config is ListOptionConfiguration}?[parseObjectArgList]|[parseVarargsList]|[parseCSVArgList]
     *   -> {config is ObjectOptionConfiguration)?[parseObjectArgList]
     **/
    private fun parseOptionsBackHalf(optName: Token, tokens: List<Token>)
            : Pair<CommandLineOption<*>?, ParseNode> = analyzing(tokens){

        val config = componentCombinators.singleOrNull { optionSpec ->
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
                ErrorNode
            }
            config is BooleanOptionConfiguration && config.isHelp -> {
                errorReporter.requestedHelp = true
                ArgumentListNode(emptyList())
            }
            config is BooleanOptionConfiguration -> ArgumentListNode(emptyList())

            //when there's an argument:
            hasArgument && config is ListOptionConfiguration<*> -> when(config.parseMode){
                is CSV -> parseCSVArgList(config, rest())
                is Varargs -> parseVarargsList(config, rest())
                is ImplicitObjects -> parseObjectArgList(config, config.asImpl.factoryOrErrors as UnrolledAndUntypedFactory<*>, rest())
            }
            hasArgument && config is ObjectOptionConfiguration<*> -> {
                //TODO this should cap the tokens it reads, this call is greedy when we dont need to be.
                parseObjectArgList(config, config.asImpl.factoryOrErrors as UnrolledAndUntypedFactory<*>, rest())
            }
            hasArgument && config is ValueOptionConfiguration<*> -> {
                val separator = expect<SeparatorToken>()
                val arg = expect<Argument>()

                ArgumentListNode(listOf(ArgumentNode(separator, config, arg)))
            }
            else -> ArgumentListNode(emptyList())
        }
        return@analyzing Pair(config, argumentList)
    }

    private fun makeOptionNode(preamble: Token, optName: Token, config: CommandLineOption<*>?, argumentList: ParseNode)
        : ParseNode = when (config) {
            is ValueOptionConfiguration<*> -> ValueOptionNode(preamble, optName, config.asImpl, argumentList)
            is BooleanOptionConfiguration -> BooleanOptionNode(preamble, optName, config.asImpl)
            is ListOptionConfiguration<*> -> ListOptionNode(preamble, optName, config.asImpl, argumentList)
            is ObjectOptionConfiguration<*> -> ObjectOptionNode(preamble, optName, config.asImpl, argumentList)
            else -> ErrorNode.apply { errorReporter.internalError(optName, "expected $config to be a known type") }
    }

    fun parseCSVArgList(config: ListOptionConfiguration<*>, tokens: List<Token>): ParseNode = analyzing(tokens){

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

        val nodes = separators.zip(args) { sep, arg -> ArgumentNode(sep, config.asImpl, arg) }
        
        return@analyzing ArgumentListNode(nodes)
    }
    
    fun <T> parseObjectArgList(
            spec: CommandLineOption<T>,
            factory: UnrolledAndUntypedFactory<T>,
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

    fun parseVarargsList(spec: CommandLineOption<*>, tokens: List<Token>): ArgumentListNode =  analyzing(tokens) {
        val nodes = buildSequence {
            while (peek() is SuperTokenSeparator && peek(1) is Argument) {
                val separator = expect<SuperTokenSeparator>()
                val arg = expect<Argument>()

                yield(ArgumentNode(separator, spec, arg))
            }
        }.toList()

        return@analyzing ArgumentListNode(nodes)
    }

    private fun availableOptions(componentCombinators: List<CommandLineOption<*>>): String {
        return componentCombinators.flatMap { it.names() }.joinToString("' or '", "'", "'")
    }

    private fun Marker.logAndRecover(message: String): ErrorNode {

        errorReporter.reportParsingProblem(peek(), message)

        while( !(peek() is SuperTokenSeparator && peek(1) is OptionPreambleToken) && peek(1) !is Epsilon) {
            next()
        }

        return ErrorNode
    }

    //quoted-comma-separated
    val List<String>.qcs: String get() = joinToString { "'$it'" }
}

