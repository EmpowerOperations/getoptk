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

    @Test fun `when parsing thing with two options should properly parse`(){

        //setup
        val args = arrayOf("--helloString", "Hello_getoptk!", "-o", "weird")

        //act
        val instance = args.parsedAs { TwoFieldImpl() }
        val helloStringResult = instance.helloString
        val oddString = instance.anotherString

        //assert
        assertThat(helloStringResult).isEqualTo("Hello_getoptk!")
        assertThat(oddString).isEqualTo("weird")
    }

    class TwoFieldImpl(): CLI {
        val helloString: String by getOpt {}
        val anotherString: String by getOpt {
            longName = listOf("oddball", "o")
        }
    }

}