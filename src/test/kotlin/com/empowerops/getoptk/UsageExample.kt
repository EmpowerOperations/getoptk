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

        //assertbu
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

    @Test fun `when using data class embedded in command line should properly read`(){
        //setup
        val args = arrayOf("--thingy", "bob", "1.234")

        //act
        val instance = args.parsedAs { ObjectHolder() }

        //assert
        assertThat(instance.thingy).isEqualTo(Thingy("bob", 1.234))
    }
    class ObjectHolder: CLI {
        val thingy: Thingy by getObjectOpt()
    }
    data class Thingy(val name: String, val value: Double)

    @Test fun `when using tree nested data classes should properly project and read`(){
        //setup
        val args = arrayOf("--thingyParent", "bob", "1.234", "2")

        //act
        val instance = args.parsedAs { TreeHolder() }

        //assert
        assertThat(instance.thingyParent).isEqualTo(ThingyParent(Thingy("bob", 1.234), 2))
    }

    class TreeHolder: CLI{
        val thingyParent: ThingyParent by getObjectOpt()
    }
    data class ThingyParent(val thingy: Thingy, val factor: Int)


    @Test fun `when using list of domain objects should properly project and read`(){
        //setup
        val args = arrayOf("--things", "frodo", "8000", "sam", "9000")

        //act
        val instance = args.parsedAs { ListOfObjectsImpl() }

        //assert
        assertThat(instance.things).isEqualTo(listOf(Thingy("frodo", 8000.0), Thingy("sam", 9000.0)))
    }
    class ListOfObjectsImpl : CLI {
        val things: List<Thingy> by getListOpt()
    }

    @Test fun `when using a list of tree like domain objects should properly parse`(){
        //setup
        val args = arrayOf("--things", "frodo", "8000", "1", "sam", "9000", "2")

        //act
        val instance = args.parsedAs { ListOfTreeObjectsImpl() }

        //assert
        assertThat(instance.things).isEqualTo(listOf(ThingyParent(Thingy("frodo", 8000.0), 1), ThingyParent(Thingy("sam", 9000.0), 2)))
    }
    class ListOfTreeObjectsImpl: CLI {
        val things: List<ThingyParent> by getListOpt()
    }
}

