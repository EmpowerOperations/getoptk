package com.empowerops.getoptk

import org.assertj.core.api.Assertions.*
import org.junit.Test
import java.lang.UnsupportedOperationException

class ConfigurationErrorHandlingFixture {

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

    @Test fun `when configuration throws exception should collect all problems and report them at the same time`(){
        val args = arrayOf("")

        //act
        val ex = assertThrows<ConfigurationException> { args.parsedAs("prog") { ExplosiveCLI() } }

        //assert
        assertThat(ex).isInstanceOf2<ConfigurationException>()
        assertThat(ex.message).isEqualTo("""CLI configuration errors:
                |specification for 'first' threw java.lang.NumberFormatException: For input string: "twenty-three"
                |specification for 'second' threw java.lang.NumberFormatException: For input string: "33.4"
                """.trimMargin()
        )
    }

    class ExplosiveCLI: CLI(){
        val first: Double by getOpt {
            default = "twenty-three".toDouble()
        }

        val second: Int by getOpt {
            default = "33.4".toInt()
        }
    }

}



