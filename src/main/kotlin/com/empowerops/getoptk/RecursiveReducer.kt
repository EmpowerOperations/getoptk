package com.empowerops.getoptk

import java.lang.reflect.Constructor
import kotlin.reflect.KClass

class RecursiveReducer<T: Any>(
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
                val argument = next() as? Argument ?: return null to tokens

                val wrapped = ErrorHandlingConverter(errorReporter, paramType as KClass<T>, converter)

                val (success, convertedValue) = wrapped.convert(argument)
                if ( ! success) return null to tokens

                arguments += convertedValue

                expect<SuperTokenSeparator>()
            }
        }

        val instance = constructor.newInstance(*arguments.toTypedArray())

        return instance to rest()
    }

    private fun <U: Any> recurse(converters: Converters, errorReporter: ErrorReporter, paramType: KClass<U>, tokens: List<Token>)
            : Pair<U?, List<Token>> {

        val reducer = RecursiveReducer(paramType, converters, errorReporter)
        return reducer.reduce(tokens)
    }
}