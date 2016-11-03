package com.empowerops.getoptk

import kotlin.reflect.KClass
import kotlin.reflect.KProperty

class ObjectOptionConfiguration<T: Any>(
        val objectType: KClass<T>,
        val converters: Converters,
        val configErrorReporter: ConfigErrorReporter,
        val userConfig: ObjectOptionConfiguration<T>.() -> Unit
) : CommandLineOption<T>(), OptionParser, ErrorReporting {

    override lateinit var errorReporter: ParseErrorReporter

    internal lateinit var value: T

    override fun finalizeInit(hostingProperty: KProperty<*>) {
        description = Inferred.generateInferredDescription(hostingProperty)
        shortName = Inferred.generateInferredShortName(hostingProperty)
        longName = Inferred.generateInferredLongName(hostingProperty)

        userConfig()
    }

    override fun getValue(thisRef: CLI, property: KProperty<*>): T = value

    override fun reduce(tokens: List<Token>): List<Token> = analyzing(tokens){

        if( ! nextIs<OptionPreambleToken>()) return tokens
        if( ! nextIs<OptionName> { it.text in names() }) return tokens
        if( ! nextIs(SeparatorToken::class)) return tokens

        val reducer = RecursiveReducer(objectType, converters, ConfigErrorReporter.Default, errorReporter)
        val (instance, reducedTokens) = reducer.reduce(rest())

        resetTo(reducedTokens)

        if(instance != null) {
            value = objectType.java.cast(instance)
        }

        return rest()
    }

    override lateinit var description: String
    override lateinit var shortName: String
    override lateinit var longName: String

    override fun toTokenGroupDescriptor(): String {
        throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

}