package com.empowerops.getoptk

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

/**
 * Created by Geoff on 2017-05-01.
 */

class ObjectSemanticsFixture {

    class TypicalCLI: CLI(){
        val first: Double by getValueOpt()

        val customer: SimpleCustomer by getOpt()
    }

    data class SimpleCustomer(val name: String, val age: Int)

    @Test fun `when trying to create instance without parsing CLI should be feasible`(){
        //setup
        val args = listOf("--first", "4.2", "-c", "bob", "27")
        val firstInstance = CLI.parse("prog", args) { TypicalCLI() }
        val secondInstance = CLI.parse("prog", args) { TypicalCLI() }

        //act & assert
        assertThat(firstInstance).isEqualTo(secondInstance)
        assertThat(firstInstance.hashCode()).isEqualTo(secondInstance.hashCode())
    }

    @Test fun `when calling toString on instance should produce reasonable toString`(){
        val instance = CLI.parse("prog", listOf("--first", "4.2", "-c", "bob", "27")) { TypicalCLI() }

        //act
        val result = instance.toString()

        //assert
        assertThat(result).isEqualTo("TypicalCLI(first=4.2, customer=SimpleCustomer(name=bob, age=27))")
    }
}
