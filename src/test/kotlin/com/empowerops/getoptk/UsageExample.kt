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
    class SimpleImpl: CLI {
        val helloString by getOpt<String>()
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
    class TwoFieldImpl: CLI {
        val helloString: String by getOpt()
        val anotherString: String by getOpt {
            longName = "oddball"
            shortName = "o"
        }
    }

    @Test fun `when parsing two options with assignment syntax should properly parse`(){
        //setup
        val args = arrayOf("--helloString=Hello_getoptk!", "-o=weird")

        //act
        val instance = args.parsedAs { TwoFieldImpl() }
        val helloStringResult = instance.helloString
        val oddString = instance.anotherString

        //assert
        assertThat(helloStringResult).isEqualTo("Hello_getoptk!")
        assertThat(oddString).isEqualTo("weird")
    }

    @Test fun `when parsing lists should properly split and parse`(){
        //setup
        val args = arrayOf("--ints", "1,2,3")

        //act
        val instance = args.parsedAs { ListImpl() }

        //assert
        assertThat(instance.ints).isEqualTo(listOf(1, 2, 3))
    }
    class ListImpl: CLI {
        val ints: List<Int> by getListOpt()
    }

    @Test fun `when using varargs should properly split`(){
        //setup
        val args = arrayOf("--items", "first", "second", "third", "fourth")

        //act
        val instance = args.parsedAs { AnotherListImpl() }

        //assert
        assertThat(instance.items).isEqualTo(listOf("first", "second", "third", "fourth"))
    }
    class AnotherListImpl: CLI {
        val items: List<String> by getListOpt {
            parseMode = ListSpreadMode.varargs
        }
    }

    @Test fun `when using flag options should properly set values`(){
        //setup
        val args = arrayOf("-f")

        //act
        val instance = args.parsedAs { FlagImpl() }

        //assert
        assertThat(instance.silent).isFalse()
        assertThat(instance.force).isTrue()
    }
    class FlagImpl: CLI {
        val silent by getFlagOpt()
        val force by getFlagOpt()
    }

}

