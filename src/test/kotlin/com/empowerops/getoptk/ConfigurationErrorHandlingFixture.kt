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
        assertThat(ex.message).isEqualTo("""CLI configuration errors:
            |  the options 'val excess: String by getValueOpt()' and 'val extra: String by getValueOpt()' have the same short name 'e'.
            """.trimMargin()
        )
    }
    class DuplicateInferredNamesArgBundle : CLI(){
        val extra: String by getValueOpt<String>()
        val excess: String by getValueOpt()
    }

    @Test fun `when configuration throws exception should collect all problems and report them at the same time`(){
        val args = arrayOf("")

        //act
        val ex = assertThrows<ConfigurationException> { args.parsedAs<ExplosiveCLI>("prog") }

        //assert
        assertThat(ex).isInstanceOf2<ConfigurationException>()
        assertThat(ex.message).isEqualTo("""CLI configuration errors:
                |  specification for 'first' threw java.lang.NumberFormatException: For input string: "twenty-three"
                |  specification for 'second' threw java.lang.Exception: blam!!
                """.trimMargin()
        )
    }

    class ExplosiveCLI: CLI(){
        val first: Double by getOpt {
            default = "twenty-three".toDouble()
        }

        val second: Int by getOpt {
            default = throw Exception("blam!!")
        }
    }

    @Test fun `when attempting to use class without default constructor should get good message`(){
        val args = emptyArray<String>()

        val result = args.tryParsedAs<UnfrinedlyCLIType>("prog")

        val resultActual = result as ConfigurationFailure
        assertThat(result.configurationProblems.map { it.message }).isEqualTo(listOf("Failed instantiate CLI instance"))
    }

    class UnfrinedlyCLIType: CLI {
        constructor(someVar: String): super()

        val first: Double by getOpt()
    }
}



