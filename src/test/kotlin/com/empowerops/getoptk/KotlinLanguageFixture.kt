package com.empowerops.getoptk

import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.fail
import org.junit.Test
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

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


    @Test fun `when calling property initializer that throws stacktrace should contain reference to problem property`(){
        val ex = try { Thingy(); fail("ctor didnt throw?"); }
        catch (ex: UnsupportedOperationException){ ex }

        val stackFrameValue = ex.stackTrace[1].toString()
        assertThat(stackFrameValue).startsWith("com.empowerops.getoptk.KotlinLanguageFixture${"$"}Thingy.<init>(KotlinLanguageFixture.kt:")
    }

    fun fail(message: String): Nothing { Assertions.fail(message); TODO() }

    class Thingy {
        val x by ExplosiveProp()
    }

    class ExplosiveProp: ReadOnlyProperty<Any, Int>{

        operator fun provideDelegate(thisRef: Any, prop: KProperty<*>): ExplosiveProp {
            throw UnsupportedOperationException()
        }

        override fun getValue(thisRef: Any, property: KProperty<*>): Int {
            return 4;
        }
    }

    @Test fun `lazys do not implement equality`(){
        val first = lazy { 42 }
        val second = lazy { 42 }

        assertThat(first).isNotEqualTo(second)
    }
}