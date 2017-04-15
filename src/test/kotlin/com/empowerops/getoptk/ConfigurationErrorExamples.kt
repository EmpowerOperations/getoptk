package com.empowerops.getoptk

import junit.framework.AssertionFailedError
import org.assertj.core.api.Assertions.*
import org.junit.Test

class ConfigurationErrorExamples {

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
    class DuplicateInferredNamesArgBundle : CLI {
        val extra: String by getValueOpt<String>()
        val excess: String by getValueOpt()
    }

    @Test fun `when using the wrong type to destructure should generate unconsumed tokens warnings`(){

        //setup & act
        val ex = assertThrows<ParseFailedException>{
            arrayOf("--eh", "hello_world", "1.0").parsedAs("prog") { ConfusedTypeArgBundle() }
        }

        //assert
        assertThat(ex.messages.first()).isEqualTo(
                """Failed to parse value for val eh: A by getValueOpt()
                  |prog --eh hello_world 1.0
                  |at:       ~~~~~~~~~~~
                  |java.lang.NumberFormatException: For input string: "1.0"
                  """.trimMargin()
        )
    }

    data class A(val name: String, val x: Int)
    data class B(val name: String, val x: Double)

    class ConfusedTypeArgBundle: CLI {
        val eh: A by getOpt()
    }

    @Test fun `when pasring type with valueOf method`(){
        //setup
        val args = arrayOf("--name", "bob")

        //act
        val ex = assertThrows<ParseFailedException> { args.parsedAs("prog") { ValueOfAbleCLI() } }

        //assert
        assertThat(ex.message).isEqualTo(
                """Unrecognized option:
                  |unknown option
                  |prog --name bob
                  |at:    ~~~~
                  |java.lang.Exception
                  |expected '-' or '--' or '/'
                  |prog --name bob
                  |at:         ~~~
                  |java.lang.Exception
                  """.trimMargin()
        )
    }
    class ValueOfAbleCLI : CLI {
        val parsable: ValueOfAble by getValueOpt()
    }
    data class ValueOfAble(val name: String) {
        companion object {
            fun valueOf(str: String) = ValueOfAble(str + "_thingy")
        }
    }

    inline private fun <reified X: Throwable> assertThrows(noinline callable: () -> Any): X {
        try {
            val result = callable()
            throw AssertionFailedError("expected $callable to throw ${X::class.qualifiedName}, but it returned normally with value $result")
        }
        catch(ex: Throwable){ when (ex) {
            is X -> return ex
            else -> throw ex
        }}
    }

    //these are some notes I took I what I figured the error messages could/should look like
    val wantedMErrorMessages = """

whatever.exe --eh hello_world 1.0 -a another two 3.0
at:                           ~~~
expected Int for /ConfusedTypeArgBundle/eh[0]:A/x:Int
java.lang.NumberFormatException: For input string: "1.0"
	at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
	at sun.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:62)
	at sun.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)

whatever.exe --eh hello_world 1.0 -e another two 3.0
at:                                          ~~~
expected Int for /ConfusedTypeArgBundle/eh[1]:A/x:Int
java.lang.NumberFormatException: For input string: "two"
	at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
	at sun.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:62)
	at sun.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)

whatever.exe --eh hello_world 1.0 -e another two 3.0
at:                                              ~~~
unknown option '3.0'
    java.lang.Exception:
	at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
	at sun.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:62)
	at sun.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)[no ]

---


whatever.exe --eh hello_world 1.0 -e another two 3.0
at:                           ~~~            ~~~ ~~~
expected Int for /ConfusedTypeArgBundle/eh[0]:A/x:Int
java.lang.NumberFormatException: For input string: "1.0"

expected Int for /ConfusedTypeArgBundle/eh[1]:A/x:Int
java.lang.NumberFormatException: For input string: "two"

unknown option '3.0'
java.lang.Exception

"""
    }