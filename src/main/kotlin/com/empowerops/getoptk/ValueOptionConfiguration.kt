package com.empowerops.getoptk

import kotlin.reflect.KClass
import kotlin.reflect.KProperty

class ValueOptionConfiguration<T: Any>(source: CLI, optionType: KClass<T>)
: CommandLineOption<T>, OptionCombinator {

    init { RegisteredOptions.optionProperties += source to this }

    // Indicates the strategy to convert the "arg" in --opt arg into a T,
    // defaults to things like "Double.ParseDouble" etc.
    // problem: multiple arity should bump this from a Funcion1 to a Funcion2 (ie `(String, String) -> T`).
    // How to do this elegantly?
    var converter: Converter<T> = Converters.getDefaultFor(optionType)

    //description to be supplied by a --help
    override var description: String = ""

    //aliases that you can use to indicate this param at the command line
    // if you had 'var bigness: Double by getOpt { names = listOf("-b", "--big", "--BN") }
    // then you could write "yourProgram --big 4.0" or "yourProgram --BN 4.5"
    // problem: how to tolerate windows style (ie yourProgram.exe /big 4.0)?
    override var names: List<String> = CommandLineOption.INFER_NAMES

    //defines the number of values at the command line this thing has associated with it
    //for example, if you had a field that was `val thing: Pair<String, Double> by getOpt { arity = 2 }`
    // what you would probably want is for --thing "key" 4.567 to produce `thing == Pair("Key", 4.567)`
    // in this example you would change airty to '2'.
    var arity: Int = 1

    internal var initialized = false
    internal var _value: T? = null

    override operator fun getValue(thisRef: CLI, property: KProperty<*>): T{
        require(initialized) { "TODO: nice error message" }
        return _value!! //uhh, how do I make this nullable iff user specified T as "String?" or some such?
    }

    override fun finalizeInit(hostingProperty: KProperty<*>) {
        if(description == "") description = Inferred.generateInferredDescription(hostingProperty)
        if(names === CommandLineOption.INFER_NAMES) names = Inferred.generateInferredNames(hostingProperty)
    }

    override fun reduce(tokens: List<Token>): List<Token> = with(Marker(tokens)){

        //TODO: a (proper) combinator here would give us:
        // 1. a more slick grammar
        // 2. better error reporting --maybe return Either<ErrorMessage, List<Token>>?
        if(next<OptionPreambleToken>()
                && next<OptionName> { it.text in names }
                && next<SuperTokenSeparator>()
                && next<Argument>()
                && next<SuperTokenSeparator>()){

            _value = converter.convert(marked().filterIsInstance<Argument>().single().text)
            initialized = true

            return rest()
        }
        else return tokens
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