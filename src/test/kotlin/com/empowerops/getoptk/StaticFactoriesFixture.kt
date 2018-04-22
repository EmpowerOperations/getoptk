package com.empowerops.getoptk

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class StaticFactoriesFixture {

    @Test fun `when looking for string provider should provide emptyString`(){
        TODO()
    }

    @Test fun `when looking at data class constructor should create factory`(){
        TODO()
    }

    @Test fun `when looking at nested data class constructor should create factory`(){
        TODO()
    }

    @Test fun `when looking at recursive constructor should return null`(){
        val result = makeFactoryFor(RecursivelyInstantiated::class, ConverterSet(emptyMap()))

        assertThat(result).isInstanceOf<FactoryErrorList>()
    }
    class RecursivelyInstantiated {
        constructor(recursive: RecursivelyInstantiated){}
    }

    @Test fun `when looking at private constructor should return null`(){
        val result = makeFactoryFor(PrivatelyInstantiated::class, ConverterSet(emptyMap()))

        assertThat(result).isInstanceOf<FactoryErrorList>()
    }
    class PrivatelyInstantiated {
        private constructor(){}
    }
}
