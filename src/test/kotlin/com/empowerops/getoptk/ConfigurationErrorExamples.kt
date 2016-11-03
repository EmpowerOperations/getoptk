package com.empowerops.getoptk

import junit.framework.AssertionFailedError
import org.assertj.core.api.Assertions.*
import org.junit.Test

class ConfigurationErrorExamples {

    @Test fun `when two cli classes have args that map to the same name should get configuration error`(){

        //setup & act
        val ex = assertThrows<ConfigurationException> {
            emptyArray<String>().parsedAs { DuplicateInferredNamesArgBundle() }
        }

        //assert
        assertThat(ex.messages).containsExactly("Name collision: -e maps to all of '-e|--excess <String-arg>' and '-e|--extra <String-arg>'")
    }
    class DuplicateInferredNamesArgBundle : CLI {
        val extra: String by getValueOpt()
        val excess: String by getValueOpt()
    }

    @Test fun `when using the wrong type to destructure should generate unconsumed tokens warnings`(){

        //setup & act
        val ex = assertThrows<ParseFailedException>{
            arrayOf("--eh", "hello_world", "1.0").parsedAs { ConfusedTypeArgBundle() }
        }

        //assert
        assertThat(ex.messages).containsExactly("at '1.0': expected Int. (java.lang.NumberFormatException: For input string: \"1.0\")")
    }

    data class A(val name: String, val x: Int)
    data class B(val name: String, val x: Double)
    class ConfusedTypeArgBundle: CLI {
        val eh: A by getOpt()
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