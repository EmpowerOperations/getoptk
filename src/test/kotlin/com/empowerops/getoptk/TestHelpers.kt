package com.empowerops.getoptk

import junit.framework.AssertionFailedError



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