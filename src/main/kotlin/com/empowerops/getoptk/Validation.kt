package com.empowerops.getoptk

/**
 * Created by Geoff on 2017-03-04.
 */

internal fun ConfigErrorReporter.validateNewEntry(existingOptions: List<AbstractCommandLineOption<*>>, newOption: AbstractCommandLineOption<*>){

    // TODO: create exceptions. Note that this method is called from `getOpt()`,
    // meaning a stack trace here points to the specific line :D

    checkNames(existingOptions, newOption)
    checkConverter(newOption)
}

private fun ConfigErrorReporter.checkConverter(newOption: AbstractCommandLineOption<*>) {

    fun reportBadFactory(option: AbstractCommandLineOption<*>, factoryOrErrors: FactoryErrorList){
        reportConfigProblem("option '${option.toPropertyDescriptor()}' cannot be built:\n" + factoryOrErrors.toDescription())
    }
    fun reportBadConverter() = reportConfigProblem(
            "option '${newOption.toPropertyDescriptor()}' " +
            "does not have a valid converter; " +
            "getoptk does not know how to convert an argument string into a '${newOption.optionType.qualifiedName}' " +
            "and no converter was explicitly set in the configuration block"
    )

    val dk = when(newOption) {
        is ObjectOptionConfigurationImpl<*> -> with(newOption){
            if(factoryOrErrors is FactoryErrorList) reportBadFactory(newOption, factoryOrErrors as FactoryErrorList)
        }
        is NullableObjectOptionConfigurationImpl<*> -> with(newOption){
            if(factoryOrErrors is FactoryErrorList) reportBadFactory(newOption, factoryOrErrors as FactoryErrorList)
        }
        is ListOptionConfigurationImpl<*> -> with(newOption){
            if(parseMode is ImplicitObjects && factoryOrErrors is FactoryErrorList){
                reportBadFactory(newOption, factoryOrErrors as FactoryErrorList)
            }
            else if (parseMode is Varargs<*> && converter is InvalidConverter) reportBadConverter()
            else { }
        }
        is ValueOptionConfigurationImpl<*> -> {
            if(newOption.converter is InvalidConverter) reportBadConverter()
            else { }
        }
        is NullableValueOptionConfigurationImpl<*> -> {
            if(newOption.converter is InvalidConverter) reportBadConverter()
            else { }
        }
        is BooleanOptionConfigurationImpl -> {
            //no conversion on booleans needed
        }
        is SubcommandOptionConfigurationImpl -> {
            //TODO
        }
        AbstractCommandLineOption.Error -> TODO()
    }
}

private fun ConfigErrorReporter.checkNames(existingOptions: List<AbstractCommandLineOption<*>>, newOption: AbstractCommandLineOption<*>) {
    if (newOption.shortName.isEmpty()) {
        reportConfigProblem("option '${newOption.toPropertyDescriptor()}' has the empty string as its short name")
    }
    if (newOption.longName.isEmpty()) {
        reportConfigProblem("option '${newOption.toPropertyDescriptor()}' has the empty string as its long name")
    }

    val shortDuplicate = existingOptions.firstOrNull { it.shortName == newOption.shortName }
    if (shortDuplicate != null) {
        reportConfigProblem(
                "the options '${newOption.toPropertyDescriptor()}' " +
                "and '${shortDuplicate.toPropertyDescriptor()}' " +
                "have the same short name '${shortDuplicate.shortName}'."
        )
    }

    val longDuplicate = existingOptions.firstOrNull { it.longName == newOption.longName }
    if (longDuplicate != null) {
        reportConfigProblem(
                "the options ${newOption.toPropertyDescriptor()} " +
                "and '${longDuplicate.toPropertyDescriptor()}' " +
                "have the same long name '${longDuplicate.longName}'."
        )
    }
}


