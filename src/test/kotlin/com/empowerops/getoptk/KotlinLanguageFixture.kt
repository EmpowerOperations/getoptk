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

    // so I thought about doing some things here around order of fields and property initializers,
    // IE: I wanted to find out if kotlin ensured that property initializers get called in order (it does),
    // but binding any functionality to that is rather obviously a bad idea:
    // how suprising would it be if re-ordering fields caused a change in functionality?
    // No, I think my best bet is to simply build a nice error reporting scheme and register an error on a name conflict.
}