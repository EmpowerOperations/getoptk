package com.empowerops.getoptk

import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

/**
 * Created by Geoff on 2016-11-03.
 */
class LexerFixture{

    val lexer = Lexer

    @Test fun `when lexing a couple simple args should generate the right list`(){
        //setup
        val args = arrayOf("--first", "second")

        //act
        val tokens = lexer.lex(args.asIterable())

        //assert
        assertThat(tokens).isEqualTo(listOf(
                LongPreamble(0),
                LongOptionName("first", 2),
                SuperTokenSeparator(7),
                Argument("second", 8),
                SuperTokenSeparator(14)
        ))
    }

    @Test fun `when lexing args with windows and assignment syntax should generate right tokens`(){
        //setup
        val args = arrayOf("/first=second")

        //act
        val tokens = lexer.lex(args.asIterable())

        //assert
        assertThat(tokens).isEqualTo(listOf(
                WindowsPreamble(0),
                LongOptionName("first", 1),
                AssignmentSeparator(6),
                Argument("second", 7),
                SuperTokenSeparator(13)
        ))
    }

    //hmm, because of my dynamic lexing strategy, I cant test the list stuff. NFG.
}