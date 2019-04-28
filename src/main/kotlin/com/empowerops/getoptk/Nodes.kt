package com.empowerops.getoptk

/**
 * Created by Geoff on 2017-03-05.
 */

internal sealed class ParseNode(open val children: List<ParseNode> = emptyList())

internal fun ParseNode.toStringTree(): String {

    fun toLocalString(builder: StringBuilder, node: ParseNode): StringBuilder = builder.apply {

        append(node::class.simpleName).append("(")

        @Suppress("IMPLICIT_CAST_TO_ANY") when(node){
            is CLINode -> append("command=${node.commandName}")
            is BooleanOptionNode -> append("name=${node.optionName.text}")
            is ListOptionNode -> append("name=${node.optionName.text}")
            is ObjectOptionNode -> append("name=${node.optionName.text}")
            is ValueOptionNode -> append("name=${node.optionName.text}")
            is ArgumentListNode -> append("size=${node.children.size}")
            is ArgumentNode -> append("value=${node.valueToken.text}")
            is ErrorNode -> Unit
        } as Any

        builder.appendln(")")
    }
    fun recurse(builder: StringBuilder, node: ParseNode, indent: Int){
        val prefix = "  ".repeat(indent)
        builder.append(prefix)
        toLocalString(builder, node)

        if(node.children.any()) {
            builder.appendln("$prefix(")

            for (child in node.children) {
                recurse(builder, child, indent + 1)
            }

            builder.appendln("$prefix)")
        }
    }

    val builder = StringBuilder()

    recurse(builder, this, 0)

    return builder.toString()
}

internal data class CLINode(
        val commandName: String,
        override val children: List<ParseNode>,
        val config: SubcommandOptionConfigurationImpl<*>? = null,
        val opts: List<AbstractCommandLineOption<*>>
): ParseNode()

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
        val config: ObjectOrNullableObjectConfiguration<*>?,
        val arguments: ParseNode
): ParseNode(listOf(arguments))

internal data class ValueOptionNode(
        val preamble: Token,
        val optionName: Token,
        val config: ValueOrNullableValueConfiguration<*>?,
        val argumentNode: ParseNode
): ParseNode(listOf(argumentNode)) 

internal data class ArgumentListNode(
        val argumentNodes: List<ParseNode>
):ParseNode(argumentNodes)

internal data class ArgumentNode(
        val separator: Token,
        val config: AbstractCommandLineOption<*>,
        val valueToken: Token
): ParseNode()

internal object ErrorNode: ParseNode()

internal fun ParseNode.accept(visitor: ValueCreationVisitor): Unit = when(this){
    is CLINode ->       this.internalAccept(visitor, { visitEnter(it) }, { visitLeave(it) })
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