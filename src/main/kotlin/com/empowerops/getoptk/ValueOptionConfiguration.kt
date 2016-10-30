package com.empowerops.getoptk

import kotlin.reflect.KClass
import kotlin.reflect.KProperty

class ValueOptionConfiguration<T: Any>(
        source: CLI,
        optionType: KClass<T>,
        private val userConfig: ValueOptionConfiguration<T>.() -> Unit
) : CommandLineOption<T>, OptionCombinator {

    init { RegisteredOptions.optionProperties += source to this }

    var converter: Converter<T> = Converters.getDefaultFor(optionType)

    override lateinit var shortName: String
    override lateinit var longName: String
    override lateinit var description: String

    internal var initialized = false
    internal var _value: T? = null

    override operator fun getValue(thisRef: CLI, property: KProperty<*>): T{
        require(initialized) { "TODO: nice error message" }
        return _value!! //uhh, how do I make this nullable iff user specified T as "String?" or some such?
    }

    override fun finalizeInit(hostingProperty: KProperty<*>) {
        description = Inferred.generateInferredDescription(hostingProperty)
        longName = Inferred.generateInferredLongName(hostingProperty)
        shortName = Inferred.generateInferredShortName(hostingProperty)

        userConfig()
    }

    override fun reduce(tokens: List<Token>): List<Token> = with(Marker(tokens)){

        //TODO: a (proper) combinator here would give us:
        // 1. a more slick grammar
        // 2. better error reporting --maybe return Either<ErrorMessage, List<Token>>?
        if(next<OptionPreambleToken>()
                && next<OptionName> { it.text in longName }
                && next<SuperTokenSeparator>()
                && next<Argument>()
                && next<SuperTokenSeparator>()){

            _value = converter.convert(marked().filterIsInstance<Argument>().single().text)
            initialized = true

            println("reduced to size = ${rest().size}")
            return rest()
        }
        else {
            println("failed to reduce")
            return tokens
        }
    }
}

class Marker(val tokens: List<Token>){

    var index = 0;
    val iterator = tokens.iterator()

    fun next(): Token{
        index += 1
        return iterator.next()
    }

    inline fun <reified T: Token> next(noinline condition: (T) -> Boolean = { true })
            = (next() as? T)?.run(condition) ?: false

    fun marked(): List<Token> = tokens.subList(0, (index+1).coerceAtMost(tokens.size))
    fun rest(): List<Token> = tokens.subList(index, tokens.size)
}