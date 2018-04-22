package com.empowerops.getoptk

import junit.framework.AssertionFailedError
import org.assertj.core.api.AbstractObjectAssert
import org.assertj.core.api.AbstractThrowableAssert


inline fun <reified X: Throwable> assertThrows(noinline callable: () -> Any): X {
    try {
        val result = callable()
        throw AssertionFailedError("expected $callable to throw ${X::class.qualifiedName}, but it returned normally with value $result")
    }
    catch(ex: Throwable){ when (ex) {
        is X -> return ex
        else -> throw ex
    }}
}

@Suppress("UNCHECKED_CAST") //'Self' type that kotlin's type system cant tolerate.
inline fun <reified C> AbstractThrowableAssert<*, out Throwable>.isInstanceOf2(): AbstractThrowableAssert<*, Throwable>
        = this.isInstanceOf(C::class.java) as AbstractThrowableAssert<*, Throwable>

@Suppress("UNCHECKED_CAST") //'Self' type that kotlin's type system cant tolerate.
inline fun <reified C> AbstractObjectAssert<*, *>.isInstanceOf(): AbstractObjectAssert<*, *> = this.isInstanceOf(C::class.java)