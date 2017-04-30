package com.empowerops.getoptk

import java.beans.Visibility
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.KVisibility



sealed class FactorySearchResult<out T>

data class FactoryErrorList(val errors: Map<List<KClass<*>>, String>, val node: KClass<*>)
    : FactorySearchResult<Nothing>(), Collection<String> by errors.values {

    operator fun plus(newSubErrors: FactoryErrorList) = when(node) {
        newSubErrors.node -> FactoryErrorList(this.errors + newSubErrors.errors, node)
        else -> FactoryErrorList(errors + newSubErrors.errors.mapKeys { listOf(node) + it.key }, node)
    }

    fun toDescription(): String = errors.entries.joinToString(separator = "\n", prefix = "error building ") {
        it.key.joinToString(separator = " via ") { it.simpleName ?: "[no class name]" } + ":" + it.value
    }
}

interface UnrolledAndUntypedFactory<out T> {
    val argCount: Int
    fun make(args: List<String>): T
}

class CompositeUnrolledAndUntypedFactory<out T>(val members: List<UnrolledAndUntypedFactory<*>>, val ctor: KFunction<T>)
    : FactorySearchResult<T>(), UnrolledAndUntypedFactory<T>{

    override fun make(args: List<String>): T {
        require(args.size == argCount)

        val argIterator = args.iterator()
        var ctorArguments: List<Any?> = emptyList()

        for((index, member) in members.withIndex()){
            val memberParams = argIterator.asSequence().take(member.argCount)

            val nextArg = try { member.make(memberParams.toList()) }
                    catch(ex: FactoryCreateFailed) { throw ex.apply { this.index += index } }
                    catch(ex: Exception) { throw FactoryCreateFailed(index, ex) }

            ctorArguments += nextArg
        }

        val result = ctor.call(*ctorArguments.toTypedArray())

        return result;
    }

    override val argCount = members.sumBy { it.argCount }

}

class ComponentUnrolledAndUntypedFactory<out T>(val factory: Converter<T>): FactorySearchResult<T>(), UnrolledAndUntypedFactory<T>{
    override fun make(args: List<String>): T = factory.invoke(args.single())
    override val argCount = 1;
}

class FactoryCreateFailed(var index: Int, ex: Exception): RuntimeException(ex)

class ConverterSet(private val converters: Map<KClass<*>, Converter<*>>)
    : Collection<Converter<*>> by converters.entries.map({ it.value }) {

    operator fun <T: Any> get(type: KClass<T>): Converter<T>?{
        val directConveter = converters[type]

        if(directConveter != null) @Suppress("UNCHECKED_CAST") return directConveter as Converter<T>

        val supertypeConverter = type.supertypes.asSequence()
                .map { it.classifier as? KClass<*>? }
                .filterNotNull()
                .map { this[it] }
                .firstOrNull { it != null }

        if(supertypeConverter != null) @Suppress("UNCHECKED_CAST") return supertypeConverter as Converter<T>

        return null
    }
    operator fun <T: Any> plus(newConveter: Pair<KClass<T>, Converter<T?>>) = ConverterSet(converters + newConveter)
}
// this is a full-tree search, which might get time-complexity problems.
// remember that the scala std-lib (and dexx in java) are able to get all of memory with only
// 7 layers of a tree with a branch factor of 32.
// here the branch factor is the total number of constructor parameters (ctors.flatMap { it.params }.size)
// which probably has a 3-sigma upperbound at around 20. That _could_ be a very large graph.
fun <T: Any> makeFactoryFor(desiredType: KClass<T>, converters: ConverterSet, remainingDepth: Int = 5)
        : FactorySearchResult<T> {

    //base case 1: we can convert this type directly
    val converter = converters[desiredType] ?: DefaultConverters[desiredType]
    if(converter != null){
        return ComponentUnrolledAndUntypedFactory(converter)
    }

    //base case 2: we're out of search space
    if(remainingDepth == 0) {
        return FactoryErrorList(mapOf(listOf(desiredType) to "Constructor dependency graph is too deep."), desiredType)
    }

    var errors = FactoryErrorList(emptyMap(), desiredType)

    val allConstructors = desiredType.constructors
    val constructors = allConstructors.filter { it.visibility ?: KVisibility.PRIVATE >= KVisibility.PUBLIC }

    if(constructors.isEmpty()){
        return FactoryErrorList(mapOf(listOf(desiredType) to "No visible constructors available"), desiredType)
    }

    ctors@ for(ctor in constructors){

        var errorsForThisCtor = FactoryErrorList(emptyMap(), desiredType)
        var subCtorsForThisCtor: List<UnrolledAndUntypedFactory<*>> = emptyList()

        for(param in ctor.parameters){
            val classifier = (param.type.classifier as? KClass<*>) ?: continue@ctors

            val factoryOrErrors = makeFactoryFor(classifier, converters, remainingDepth - 1)

            when(factoryOrErrors){
                is FactoryErrorList -> errorsForThisCtor += factoryOrErrors
                is UnrolledAndUntypedFactory<*> -> subCtorsForThisCtor += factoryOrErrors
            }

            if(errorsForThisCtor.any()) {
                errors += errorsForThisCtor
                continue@ctors
            }
        }

        return CompositeUnrolledAndUntypedFactory(subCtorsForThisCtor, ctor)
    }

    return errors
}

operator fun KVisibility.compareTo(right: KVisibility): Int = this.ordinal.compareTo(right.ordinal)