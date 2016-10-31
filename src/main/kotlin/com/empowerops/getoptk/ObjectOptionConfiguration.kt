package com.empowerops.getoptk

import java.lang.reflect.Constructor
import kotlin.reflect.KClass
import kotlin.reflect.KProperty

class ObjectOptionConfiguration<T: Any>(
        val objectType: KClass<T>,
        val converters: Converters,
        override val errorReporter: ErrorReporter,
        val userConfig: ObjectOptionConfiguration<T>.() -> Unit
) : CommandLineOption<T>(), OptionParser, ErrorReporting {

    private val reducer = Reducer(objectType, converters, errorReporter)
    lateinit var value: T

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

        val (instance, reducedTokens) = reducer.reduce(rest());

        resetTo(reducedTokens)

        value = objectType.java.cast(instance)

        expect<SuperTokenSeparator>()

        return rest()
    }

    override lateinit var description: String
    override lateinit var shortName: String
    override lateinit var longName: String

    override fun toTokenGroupDescriptor(): String {
        throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    private class Reducer<T: Any>(
            val objectType: KClass<T>,
            val converters: Converters,
            override val errorReporter: ErrorReporter
    ): ErrorReporting {

        private lateinit var constructor: Constructor<T>
        private lateinit var paramTypes: List<KClass<*>>

        init {
            val constructor = objectType.java.constructors.singleOrNull()

            if(constructor == null) {
                errorReporter.reportConfigProblem("unable to find suitable constructor for ${objectType.qualifiedName}, it must have exactly 1 public constructor")
            }
            else{
                this.constructor = @Suppress("UNCHECKED_CAST")(constructor as Constructor<T>)
                paramTypes = constructor.parameterTypes.map { it.kotlin }
            }
        }

        fun reduce(tokens: List<Token>): Pair<T?, List<Token>> = analyzing(tokens){

            var arguments = emptyList<Any?>()

            for(paramType in paramTypes){
                val converter = converters.tryFindingConverterFor(paramType)

                if(converter == null){

                    val (result, updatedTokens) = recurse(converters, errorReporter, paramType, rest())

                    resetTo(updatedTokens)

                    arguments += result ?: return null to tokens
                }
                else {
                    if( ! nextIs(SeparatorToken::class)) return null to tokens
                    val argument = next() as? Argument ?: return null to tokens

                    val wrapped = ErrorHandlingConverter(errorReporter, paramType as KClass<T>, converter)

                    val (success, convertedValue) = wrapped.convert(argument)
                    if ( ! success) return null to tokens

                    arguments += convertedValue
                }
            }

            val instance = constructor.newInstance(*arguments.toTypedArray())

            return instance to rest()
        }

        private fun <U: Any> recurse(converters: Converters, errorReporter: ErrorReporter, paramType: KClass<U>, tokens: List<Token>)
                : Pair<U?, List<Token>> {

            val reducer = Reducer(paramType, converters, errorReporter)
            return reducer.reduce(tokens)
        }
    }
}