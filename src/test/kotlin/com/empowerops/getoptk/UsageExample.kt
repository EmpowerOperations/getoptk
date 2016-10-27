package com.empowerops.getoptk

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

/**
 * Created by Geoff on 2016-10-26.
 */
class UsageExample {

    @Test fun `when using usage example created initially should parse properly`(){
        //setup
        val args = arrayOf("--helloString", "Hello_getoptk!")

        //act
        val instance = args.parsedAs { SimpleImpl() }
        val result = instance.helloString

        //assert
        assertThat(result).isEqualTo("Hello_getoptk!")
    }

    class SimpleImpl() : CLI {
        val helloString: String by getOpt {}
    }

}