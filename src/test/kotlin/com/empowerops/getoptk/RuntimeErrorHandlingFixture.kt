package com.empowerops.getoptk

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import java.lang.UnsupportedOperationException

class RuntimeErrorHandlingFixture {

    class Thingy: CLI() {
        val required by getOpt<Int>(){
            isRequired = true
        }
        val notRequired by getNullableOpt<Int>(){
            isRequired = false
        }
    }

    @Test fun `when parsing arguments that dont include required argument should raise error`(){
        //setup
        val args = emptyArray<String>()

        //act
        val ex = assertThrows<MissingOptionsException> { args.parsedAs("prog.exe") { Thingy() } }

        //assert
        assertThat(ex.message!!.trimLineEnds()).isEqualTo("""Missing required options: 'required'
                |usage: prog.exe
                | -r,--required <int>
                | -n,--notRequired <int>
                """.trimMargin()
        )
    }

    class ListableThingy: CLI(){
        val someList by getListOpt<Double>()
    }
    @Test fun `when parsing empty command line for list should get back empty list`(){
        //setup
        val args = emptyArray<String>()

        //act
        val result = args.parsedAs("program") { ListableThingy() }

        //assert
        assertThat(result.someList).isEqualTo(emptyList<Double>())
    }


    @Test fun `when using the wrong type to destructure should generate unconsumed tokens warnings`(){

        //setup & act
        val ex = assertThrows<ParseFailedException>{
            arrayOf("--eh", "hello_world", "1.0").parsedAs("prog") { ConfusedTypeArgBundle() }
        }

        //assert
        assertThat(ex.message!!.trimLineEnds()).isEqualTo(
                """Failure in command line:
                  |
                  |Failed to parse value for val eh: A by getOpt()
                  |prog --eh hello_world 1.0
                  |at:                   ~~~
                  |java.lang.NumberFormatException: For input string: "1.0"
                  |
                  |usage: prog
                  | -e,--eh <value>
                  """.trimMargin()
        )
        assertThat(ex.cause).isInstanceOf(NumberFormatException::class.java)
    }

    data class A(val name: String, val x: Int)
    data class B(val name: String, val x: Double)

    class ConfusedTypeArgBundle: CLI(){
        val eh: A by getOpt()
    }

    @Test fun `when attempting to use the wrong option name should generate nice error messages`(){
        //setup
        val args = arrayOf("--name", "bob")

        //act
        val ex = assertThrows<ParseFailedException> { args.parsedAs("prog") { ValueOfAbleCLI() } }

        //assert
        assertThat(ex.message!!.trimLineEnds()).isEqualTo(
                """Failure in command line:
                  |
                  |unknown option 'name', expected 'parsable', 'help'
                  |prog --name bob
                  |at:    ~~~~
                  |
                  |usage: prog
                  | -p,--parsable <value>
                  """.trimMargin()
        )
    }
    class ValueOfAbleCLI : CLI(){
        val parsable: ValueOfAble by getValueOpt()
    }
    data class ValueOfAble(val name: String) {
        companion object {
            fun valueOf(str: String) = ValueOfAble(str + "_thingy")
        }
    }

    @Test fun `when custom converter throws should recover and display proper error message`(){
        //setup
        val args = arrayOf("--problem", "sam")

        //act
        val ex = assertThrows<ParseFailedException> { args.parsedAs("prog") { BadConvertingCLI() }}

        //assert
        assertThat(ex.message!!.trimLineEnds()).isEqualTo(
                """Failure in command line:
                  |
                  |Failed to parse value for val problem: String by getValueOpt()
                  |prog --problem sam
                  |at:            ~~~
                  |java.lang.UnsupportedOperationException: no sam's allowed!
                  |
                  |usage: prog
                  | -p,--problem <value>
                  """.trimMargin()
        )
        assertThat(ex.cause).isInstanceOf(UnsupportedOperationException::class.java)
    }

    class BadConvertingCLI: CLI(){
        val problem: String by getValueOpt {
            converter = { when(it){
                "sam" -> throw UnsupportedOperationException("no sam's allowed!")
                "bob" -> "bobbo"
                else -> TODO()
            }}
        }
    }

    @Test fun `when command line fails to specify an argument should properly format`(){
        val args = arrayOf("--req1", "val1")

        //act
        val ex = assertThrows<MissingOptionsException> { args.parsedAs("prog") { TwoRequiredFieldCLI() } }

        //assert
        assertThat(ex)
                .isInstanceOf2<MissingOptionsException>()
                .hasMessageContaining("Missing required options: 'req2', 'req3'")
    }
    class TwoRequiredFieldCLI: CLI(){
        val req1: String by getValueOpt()
        val req2: String by getValueOpt {
            shortName = "r2"
        }
        val req3: String by getValueOpt {
            shortName = "r3"
        }
    }

    @Test fun `when using nonnull not required option should throw exception on value retrieval`(){
        //setup
        val args = emptyArray<String>()

        //act
        val result = args.parsedAs("program") { OneNonnullOptionalFieldCLI() }
        val ex = assertThrows<UninitializedPropertyAccessException> { result.optional1 }

        //assert
        assertThat(ex.message).isEqualTo("val optional1: NonDefaulted by getOpt() has not been initialized")
    }
    class OneNonnullOptionalFieldCLI: CLI(){
        val optional1: NonDefaulted by getOpt() {
            isRequired = false
        }
    }
    class NonDefaulted(val arg: String)


}
