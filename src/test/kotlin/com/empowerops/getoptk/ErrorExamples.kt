package com.empowerops.getoptk

import junit.framework.AssertionFailedError
import org.assertj.core.api.Assertions.*
import org.junit.Test
import java.lang.UnsupportedOperationException

class ErrorExamples {

    @Test fun `when two cli classes have args that map to the same name should get configuration error`(){

        //setup & act
        val ex = assertThrows<ConfigurationException> {
            emptyArray<String>().parsedAs("prog") { DuplicateInferredNamesArgBundle() }
        }

        //assert
        assertThat(ex.messages.single().message).isEqualTo(
                "the options 'val excess: String by getValueOpt()' and 'val extra: String by getValueOpt()' have the same short name 'e'."
        )
    }
    class DuplicateInferredNamesArgBundle : CLI(){
        val extra: String by getValueOpt<String>()
        val excess: String by getValueOpt()
    }

    @Test fun `when using the wrong type to destructure should generate unconsumed tokens warnings`(){

        //setup & act
        val ex = assertThrows<ParseFailedException>{
            arrayOf("--eh", "hello_world", "1.0").parsedAs("prog") { ConfusedTypeArgBundle() }
        }

        //assert
        assertThat(ex.message).isEqualTo(
                """Failed to parse value for val eh: A by getValueOpt()
                  |prog --eh hello_world 1.0
                  |at:                   ~~~
                  |java.lang.NumberFormatException: For input string: "1.0"
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
        assertThat(ex.message).isEqualTo(
                """unknown option 'name', expected 'parsable', 'help'
                  |prog --name bob
                  |at:    ~~~~
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
        assertThat(ex.message).isEqualTo(
                """Failed to parse value for val problem: String by getValueOpt()
                  |prog --problem sam
                  |at:            ~~~
                  |java.lang.UnsupportedOperationException: no sam's allowed!
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
}


