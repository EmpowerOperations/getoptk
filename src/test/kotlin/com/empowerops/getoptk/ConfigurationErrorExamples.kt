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
        val extra: String by getOpt()
        val excess: String by getOpt()
    }

    @Test fun `when using the wrong type to destructure should generate unconsumed tokens warnings`(){

        //setup & act
        val ex = assertThrows<ParseFailedException>{
            arrayOf("--eh", "hello_world", "1.0").parsedAs { ConfusedTypeArgBundle() }
        }

        //assert
        assertThat(ex.messages).containsExactly("blam!")
    }

    data class A(val name: String, val x: Int)
    data class B(val name: String, val x: Double)
    class ConfusedTypeArgBundle: CLI {
        val eh: A by getObjectOpt()
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
}