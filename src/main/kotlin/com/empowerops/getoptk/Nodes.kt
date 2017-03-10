package com.empowerops.getoptk

/**
 * Created by Geoff on 2017-03-05.
 */

internal sealed class ParseNode(open val children: List<ParseNode> = emptyList())

internal data class CLIRootNode(override val children: List<ParseNode>): ParseNode()

internal data class BooleanOptionNode(
        val preamble: Token,
        val optionName: Token,
        val config: BooleanOptionConfigurationImpl?
): ParseNode()

internal data class ListOptionNode(
        val preamble: Token,
        val optionName: Token,
        val config: ListOptionConfigurationImpl<*>?,
        val arguments: ParseNode
): ParseNode(listOf(arguments))

internal data class ObjectOptionNode(
        val preamble: Token,
        val optionName: Token,
        val config: ObjectOptionConfigurationImpl<*>?,
        val arguments: ParseNode
): ParseNode(listOf(arguments))

internal data class ValueOptionNode(
        val preamble: Token,
        val optionName: Token,
        val config: ValueOptionConfigurationImpl<*>?,
        val argumentNode: ParseNode
): ParseNode(listOf(argumentNode)) 

internal data class ArgumentListNode(
        val argumentNodes: List<ParseNode>
):ParseNode(argumentNodes)

internal data class ArgumentNode(
        val separator: Token,
        val config: CommandLineOption<*>,
        val valueToken: Token
): ParseNode()

internal object ErrorNode: ParseNode()

internal fun ParseNode.accept(visitor: ValueCreationVisitor): Unit = when(this){
    is CLIRootNode ->       this.internalAccept(visitor, { visitEnter(it) }, { visitLeave(it) })
    is ValueOptionNode ->   this.internalAccept(visitor, { visitEnter(it) }, { visitLeave(it) })
    is ObjectOptionNode ->  this.internalAccept(visitor, { visitEnter(it) }, { visitLeave(it) })
    is ListOptionNode ->    this.internalAccept(visitor, { visitEnter(it) }, { visitLeave(it) })
    is BooleanOptionNode -> this.internalAccept(visitor, { visitEnter(it) }, { visitLeave(it) })
    is ArgumentNode ->      this.internalAccept(visitor, { visitEnter(it) }, { visitLeave(it) })
    is ArgumentListNode ->  this.internalAccept(visitor, { visitEnter(it) }, { visitLeave(it) })
    is ErrorNode ->         this.internalAccept(visitor, { visitEnter(it) }, { visitLeave(it) })
}

private fun <T> T.internalAccept(
        visitor: ValueCreationVisitor,
        enter: ValueCreationVisitor.(T) -> Unit,
        leave: ValueCreationVisitor.(T) -> Unit
): Unit where T: ParseNode{

    visitor.enter(this)
    children.forEach { it.accept(visitor) }
    visitor.leave(this)
}