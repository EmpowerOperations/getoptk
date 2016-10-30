package com.empowerops.getoptk

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class KotlinLanguageFixture {

    @Test fun `when using pair syntax on a list should get back front elements`(){

        val (first, second) = makeStrings()

        assertThat(first).isEqualTo("A")
        assertThat(second).isEqualTo("B")

        //neat!
    }

    fun makeStrings() = listOf("A", "B", "C");
}