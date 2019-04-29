package com.empowerops.getoptk

import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KClass
import kotlin.reflect.KProperty


//region provideDelegate
// these methods must be public & discoverable by kotlin compiler!
// do not try to reduce duplication with inheritance

operator fun <T: Any> ValueOptionConfiguration<T>.provideDelegate(thisRef: CLI, prop: KProperty<*>)
        = provideDelegateImpl(this, thisRef, prop)

operator fun <T: Any> NullableValueOptionConfiguration<T>.provideDelegate(thisRef: CLI, prop: KProperty<*>)
        = provideDelegateImpl(this, thisRef, prop)

operator fun <T: Any> ObjectOptionConfiguration<T>.provideDelegate(thisRef: CLI, prop: KProperty<*>)
        = provideDelegateImpl(this, thisRef, prop)

operator fun <T: Any> NullableObjectOptionConfiguration<T>.provideDelegate(thisRef: CLI, prop: KProperty<*>)
        = provideDelegateImpl(this, thisRef, prop)

operator fun <T: Any> ListOptionConfiguration<T>.provideDelegate(thisRef: CLI, prop: KProperty<*>)
        = provideDelegateImpl(this, thisRef, prop)

operator fun BooleanOptionConfiguration.provideDelegate(thisRef: CLI, prop: KProperty<*>)
        = provideDelegateImpl(this, thisRef, prop)

operator fun <T: CLI> SubcommandOptionConfiguration<T>.provideDelegate(thisRef: CLI, prop: KProperty<*>)
        = provideDelegateImpl(this, thisRef, prop)


//endregion

internal fun <T, E> provideDelegateImpl(host: T, thisRef: CLI, prop: KProperty<*>): T
        where T: ReadOnlyProperty<CLI, E> {
    (host as AbstractCommandLineOption<*>).provideDelegateImpl(thisRef, prop)
    return host
}

internal val AbstractCommandLineOption<*>.factoryOrErrors: FactorySearchResult<*> get() = when(this){
    is ObjectOptionConfigurationImpl<*> -> this.factoryOrErrors
    is NullableObjectOptionConfigurationImpl<*> -> this.factoryOrErrors
    else -> TODO()
}

internal object NO_DEFAULT_AVAILABLE

internal fun AbstractCommandLineOption<*>.names() = listOf(shortName, longName)
internal fun AbstractCommandLineOption<*>.toPropertyDescriptor(): String {
    val valOrVarPrefix = if(property.isFinal) "val" else "var"

    val name = property.name
    val type = (property.returnType.classifier as? KClass<*>)?.simpleName ?: "<unknown-type>"

    val getOptFlavour = when(this){
        is ValueOptionConfigurationImpl<*> -> "getValueOpt"
        is NullableValueOptionConfigurationImpl<*> -> "getNullableValueOpt"
        is BooleanOptionConfigurationImpl -> "getFlagOpt"
        is ListOptionConfigurationImpl<*> -> "getListOpt"
        is ObjectOptionConfigurationImpl<*> -> "getOpt"
        is NullableObjectOptionConfigurationImpl<*> -> "getNullableOpt"
        is SubcommandOptionConfigurationImpl<*> -> "getCommandOpt"
        AbstractCommandLineOption.Error -> TODO()
    }

    return "$valOrVarPrefix $name: $type by $getOptFlavour()"
}

internal fun makeHelpOption(otherOptions: List<AbstractCommandLineOption<*>>) = BooleanOptionConfigurationImpl({
    longName = if(otherOptions.any { it.longName == "help" }) "" else "help"
    shortName = if(otherOptions.any { it.shortName == "h" }) "" else "h"
    isHelp = true
    isRequired = false
})

