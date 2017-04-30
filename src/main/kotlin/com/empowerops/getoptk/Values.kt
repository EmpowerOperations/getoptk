package com.empowerops.getoptk

import java.nio.file.Path
import java.nio.file.Paths
import kotlin.reflect.KClass
import kotlin.reflect.full.companionObject

/**
 * Created by Geoff on 2017-04-29.
 */

object DefaultValues {

    @Suppress("UNCHECKED_CAST", "IMPLICIT_CAST_TO_ANY") //unfortunately I'm moving from dynamic back into static types here.
    //AFAIK there is no way to tell kotlin that if type == Int::class, then T == Int
    operator fun <T: Any> get(type: KClass<T>): T? = when {

        type == String::class -> ""
        type == Path::class -> Paths.get("")
        type == Int::class -> 0
        type == Long::class -> 0L
        type == Float::class -> 0.0F
        type == Double::class -> 0.0

//        type == Char::class -> '\u0000' ?
//        type.java.isEnum -> EnumConverter(type.java as Class<Nothing>) ?
        //big values and etc?

        else -> null
    } as T?
}