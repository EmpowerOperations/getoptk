package com.empowerops.getoptk

import junit.framework.AssertionFailedError
import org.assertj.core.api.Assertions.*
import org.junit.Test

class ConfigurationErrorExamples {

    @Test fun `when two cli classes have args that map to the same name should get configuration error`(){

        //setup & act
        val ex = assertThrows<ConfigurationException> {
            emptyArray<String>().parsedAs { DoubleName() }
        }

        //assert
        assertThat(ex.messages).containsExactly("Name collision: -e maps to all of '-e|--extra <String-arg>' and '-e|--excess <String-arg>'")
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

    class DoubleName: CLI {
        val extra: String by getOpt()
        val excess: String by getOpt()
    }
}