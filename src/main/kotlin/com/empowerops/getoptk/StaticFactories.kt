package com.empowerops.getoptk

import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.KVisibility



sealed class FactorySearchResult<out T>

object NullFactory: FactorySearchResult<Nothing>()

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

interface UnrolledAndUntypedFactory<out T>{
    val arity: Int
    fun make(args: List<String>): T
}

data class PremadeValue<out T>(val value: T): FactorySearchResult<T>(), UnrolledAndUntypedFactory<T> {

    override val arity = 0
    override fun make(args: List<String>): T{
        require(args.isEmpty())
        return value
    }
}

class CompositeUnrolledAndUntypedFactory<out T>(val members: List<UnrolledAndUntypedFactory<*>>, val ctor: KFunction<T>)
    : FactorySearchResult<T>(), UnrolledAndUntypedFactory<T>{

    override fun make(args: List<String>): T {
        require(args.size == arity)

        val argIterator = args.iterator()
        var ctorArguments: List<Any?> = emptyList()

        for((index, member) in members.withIndex()){
            val memberParams = argIterator.asSequence().take(member.arity)
            // TODO: if i use List<KParameter> instead of simply argCount, I can use callBy,
            // which is both more robust and will provide a really nice to-string

            val nextArg = try { member.make(memberParams.toList()) }
                    catch(ex: FactoryCreateFailed) { throw ex.apply { this.index += index } }
                    catch(ex: Exception) { throw FactoryCreateFailed(index, ex) }

            ctorArguments += nextArg
        }

        val result = ctor.call(*ctorArguments.toTypedArray())

        return result;
    }

    override val arity = members.sumBy { it.arity }

}

class ComponentUnrolledAndUntypedFactory<out T>(val factory: Converter<T>): FactorySearchResult<T>(), UnrolledAndUntypedFactory<T>{
    override fun make(args: List<String>): T = factory.invoke(args.single())
    override val arity = 1
}

class FactoryCreateFailed(var index: Int, ex: Exception): RuntimeException(ex)

class ConverterSet(private val converters: Map<KClass<*>, Converter<*>>)
    : Collection<Converter<*>> by converters.entries.map({ it.value }) {

    operator fun <T: Any> get(type: KClass<T>): Converter<T>?{
        val directConverter = converters[type]

        if(directConverter != null) @Suppress("UNCHECKED_CAST") return directConverter as Converter<T>

        val supertypeConverter = type.supertypes.asSequence()
                .map { it.classifier as? KClass<*>? }
                .filterNotNull()
                .map { converters[it] }
                .firstOrNull { it != null }

        if(supertypeConverter != null) @Suppress("UNCHECKED_CAST") return supertypeConverter as Converter<T>

        return null
    }
    operator fun <T: Any> plus(newConveter: Pair<KClass<T>, Converter<T?>>) = ConverterSet(converters + newConveter)
}

internal fun <T: Any> makeFactoryFor(desiredType: KClass<T>, converters: ConverterSet): FactorySearchResult<T>
         = makeFactoryFor(desiredType, converters, makeClosedDelegate = false)

internal fun <T: Any> makeProviderOf(desiredType: KClass<T>, converters: ConverterSet): FactorySearchResult<T>
        = makeFactoryFor(desiredType, converters, makeClosedDelegate = true)

// this is a full-tree search, which might get time-complexity problems.
// remember that the scala std-lib (and dexx in java) are able to get all of memory with only
// 7 layers of a tree with a branch factor of 32.
// here the branch factor is the total number of constructor parameters (ctors.flatMap { it.params }.size)
// which probably has a 3-sigma upperbound at around 20. That _could_ be a very large graph.
// in other words, if the caller is trying to parse some huge nasty old POKO,
// that itself has parameters that are huge nasty POKO's,
// then we might take a long time to return.
// practically speaking, this would mean the user is attempting to type a huge number of options at the command line
//   --or is using this library ins some manner I don't yet understand.
//
// in any event, I'll side-step this by simply having a max recursion depth that is reasonably shallow.
private fun <T: Any> makeFactoryFor(
        desiredType: KClass<T>,
        converters: ConverterSet,
        makeClosedDelegate: Boolean,
        ctorStack: List<KFunction<*>> = emptyList(),
        remainingDepth: Int = 5
): FactorySearchResult<T> {

    //baseCase 0 (hack): if we're in closed-delegate mode look for a default value
    if(makeClosedDelegate){
        val defaultValue = DefaultValues[desiredType]
        if(defaultValue != null){
            return PremadeValue(defaultValue)
        }
    }

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

    val constructors = desiredType.constructors
            .filter { it.visibility ?: KVisibility.PRIVATE < KVisibility.PUBLIC }
            .filter { it !in ctorStack }

    if(constructors.isEmpty()){
        return FactoryErrorList(mapOf(listOf(desiredType) to "No visible constructors available"), desiredType)
    }

    //recursive case: we need to find a constructor that fits the bill.
    ctors@ for(ctor: KFunction<T> in constructors){

        var errorsForThisCtor = FactoryErrorList(emptyMap(), desiredType)
        var subCtorsForThisCtor: List<UnrolledAndUntypedFactory<*>> = emptyList()

        for(param in ctor.parameters){
            val classifier = (param.type.classifier as? KClass<*>) ?: continue@ctors
            //TODO: add debug or trace statements here about why/which ctor we pick.

            val factoryOrErrors = makeFactoryFor(
                    classifier,
                    converters,
                    makeClosedDelegate,
                    ctorStack + ctor,
                    remainingDepth - 1
            )

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