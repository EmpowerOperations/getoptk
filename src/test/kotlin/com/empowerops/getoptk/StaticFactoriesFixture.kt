package com.empowerops.getoptk

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import kotlin.reflect.KClass

private val EmptyConverterSet = ConverterSet(emptyMap())

class StaticFactoriesFixture {

    @Test fun `when looking for int provider should provide 0`()= runPrimativeTest(Int::class, 0)
    @Test fun `when looking for double provider should provide 0p0`()= runPrimativeTest(Double::class, 0.0)
    @Test fun `when looking for string provider should provide empty string`() = runPrimativeTest(String::class, "")

    private fun <T: Any> runPrimativeTest(desiredType: KClass<T>, expected: T) {
        //act
        val resultProvider = makeProviderOf(desiredType, EmptyConverterSet)
        val result = (resultProvider as? PremadeValue)?.value

        //assert
        assertThat(resultProvider).isInstanceOf<PremadeValue<*>>()
        assertThat(result).isEqualTo(expected)
    }

    @Test fun `when looking at data class constructor should create factory`(){
        //act
        val factory = makeFactoryFor(SimplePOKO::class, EmptyConverterSet)
        val knownFactory = (factory as? UnrolledAndUntypedFactory<*>)
        val result = knownFactory?.make(listOf("asdf"))

        //assert
        assertThat(factory).isInstanceOf<UnrolledAndUntypedFactory<*>>()
        assertThat(knownFactory).isNotNull()
        assertThat(result).isEqualTo(SimplePOKO("asdf"))
    }
    data class SimplePOKO(val arg: String)

    @Test fun `when looking at nested data class constructor should create factory`(){
        //act
        val factory = makeFactoryFor(OuterPOKO::class, EmptyConverterSet)
        val knownFactory = (factory as? UnrolledAndUntypedFactory<*>)
        val result = knownFactory?.make(listOf("asdf", "jkl;"))

        //assert
        assertThat(factory).isInstanceOf<UnrolledAndUntypedFactory<*>>()
        assertThat(knownFactory).isNotNull()
        assertThat(result).isEqualTo(OuterPOKO("asdf", InnerPOKO(("jkl;"))))
    }
    data class OuterPOKO(val arg: String, val inner: InnerPOKO)
    data class InnerPOKO(val anotherArg: String)

    @Test fun `when looking at recursive constructor should return null`(){
        val result = makeFactoryFor(RecursivelyInstantiated::class, EmptyConverterSet)

        assertThat(result).isInstanceOf<FactoryErrorList>()
    }
    class RecursivelyInstantiated {
        constructor(recursive: RecursivelyInstantiated){}
    }

    @Test fun `when looking at private constructor should return null`(){
        val result = makeFactoryFor(PrivatelyInstantiated::class, EmptyConverterSet)

        assertThat(result).isInstanceOf<FactoryErrorList>()
    }
    class PrivatelyInstantiated {
        private constructor(){}
    }
}
