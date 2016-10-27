package com.empowerops.getoptk

import com.empowerops.getoptk._sample.CLI
import com.empowerops.getoptk._sample.getOpt
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

/**
 * Created by Geoff on 2016-10-26.
 */
class UsageExample {

    @Test fun `when using usage example created initially should parse properly`(){
        //setup
        val instance = SimpleImpl(arrayOf("--complexThing", "Hello_getoptk!"))

        //act
        val result = instance.complexThing

        //assert
        assertThat(result).isEqualTo("Hello_getoptk!")
    }

    class SimpleImpl(override val args: Array<String>) : CLI {

        val complexThing: String by getOpt {}
    }

}