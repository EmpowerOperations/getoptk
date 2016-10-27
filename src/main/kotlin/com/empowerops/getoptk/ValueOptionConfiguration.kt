package com.empowerops.getoptk

import kotlin.reflect.KClass
import kotlin.reflect.KProperty

class ValueOptionConfiguration<T: Any>(source: CLI, optionType: KClass<T>)
: CommandLineOption<T>, ReflectivelyInitialized {

    init { RegisteredOptions.optionProperties += source to this }

    // Indicates the strategy to convert the "arg" in --opt arg into a T,
    // defaults to things like "Double.ParseDouble" etc.
    // problem: multiple arity should bump this from a Funcion1 to a Funcion2 (ie `(String, String) -> T`).
    // How to do this elegantly?
    var parser: (String) -> T = Parsers.getDefaultFor(optionType)

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

    override fun reduce(tokens: List<Token>): List<Token> {

        //TODO: a combinator here gives us:
        // 1. a more slick grammar
        // 2. better error reporting --maybe return Either<ErrorMessage, List<Token>>?
        if(tokens[0] is OptionPreambleToken
                && tokens[1].let { it is OptionName && it.text in names }
                && tokens[2].let { it is SuperTokenSeparator }
                && tokens[3].let { it is Argument }
                && tokens[4].let { it is SuperTokenSeparator }){

            _value = parser((tokens[3] as Argument).text)
            initialized = true

            return tokens.subList(5, tokens.size)
        }
        else return tokens
    }
}