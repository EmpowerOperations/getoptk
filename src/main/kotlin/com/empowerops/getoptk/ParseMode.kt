package com.empowerops.getoptk

interface ParseMode {

    companion object {
        //indicate that a list arg is --list x,y,z
        val CSV: ParseMode = separator(",")

        //indicate that a list arg is --list x --list y --list z
        val iteratively: ParseMode = IterativeParseMode

        fun separator(separator: String): ParseMode = TODO()
        fun regex(regex: Regex, captureGroupName: String = "item"): ParseMode = TODO()
    }

    fun spread(argumentText: String): List<String>
}

class SeparatorParseMode(val separator: String): ParseMode {
    override fun spread(argumentText: String): List<String> = argumentText.split(separator)
}

object IterativeParseMode: ParseMode {
    override fun spread(argumentText: String): List<String> = listOf(argumentText)
}

