package com.empowerops.getoptk

import kotlin.reflect.KClass
import kotlin.reflect.KProperty

class ListOptionConfiguration<T: Any>(source: CLI, optionType: KClass<T>)
: CommandLineOption<T>, OptionCombinator {

    init { RegisteredOptions.optionProperties += source to this }

    //name change to avoid confusion, user might want a "parse the whole value as a list"
    var elementConverter: Converter<T> = Converters.getDefaultFor(optionType)

    override var description: String = ""
    override var names: List<String> = CommandLineOption.INFER_NAMES

    //can this always be inferred?
    //should we support `prog --list 1 2 3` as being varags for a list with 3 elements?
    //what if you want a list of multi-arity (the obvious example being a map) then you get --list "hello" 1 "world" 2.
    var arity: Int = 1

    var parseMode: ParseMode = ParseMode.CSV

    override operator fun getValue(thisRef: CLI, property: KProperty<*>): T {
        TODO()
    }

    override fun finalizeInit(hostingProperty: KProperty<*>) {
        if(description == "") description = Inferred.generateInferredDescription(hostingProperty)
        if(names === CommandLineOption.INFER_NAMES) names = Inferred.generateInferredNames(hostingProperty)
    }

    override fun reduce(tokens: List<Token>): List<Token> {
        TODO()
    }

}