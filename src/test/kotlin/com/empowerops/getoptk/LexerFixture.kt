package com.empowerops.getoptk

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

    @Test fun `when lexing a bunch of trickey args should generate correct tokens`(){
        //setup
        val args = arrayOf("-f", "val1", "--force", "val2", "/g", "val3", "val4", "/great", "val5", "/another=val6")

        //act
        val tokens = lexer.lex(args.asIterable())

        //assert
        assertThat(tokens).isEqualTo(listOf(
                ShortPreamble(0),
                ShortOptionName(text="f", index=1),
                SuperTokenSeparator(index=2),
                Argument(text="val1", index=3),
                SuperTokenSeparator(index=7),
                LongPreamble(index=8),
                LongOptionName(text="force", index=10),
                SuperTokenSeparator(index=15),
                Argument(text="val2", index=16),
                SuperTokenSeparator(index=20),
                WindowsPreamble(index=21),
                ShortOptionName(text="g", index=22),
                SuperTokenSeparator(index=23),
                Argument(text="val3", index=24),
                SuperTokenSeparator(index=28),
                Argument(text="val4", index=29),
                SuperTokenSeparator(index=33),
                WindowsPreamble(index=34),
                LongOptionName(text="great", index=35),
                SuperTokenSeparator(index=40),
                Argument(text="val5", index=41),
                SuperTokenSeparator(index=45),
                WindowsPreamble(index=46),
                LongOptionName(text="another", index=47),
                AssignmentSeparator(index=54),
                Argument(text="val6", index=55),
                SuperTokenSeparator(index=59)
        ))
    }

    @Test fun `when parsing lists should generate correct tokens`(){
        //setup
        val args = arrayOf("--csvList", "1,2,3", "--varargsList", "1", "2", "3")

        //act
        val tokens = lexer.lex(args.asIterable())

        //assert
        assertThat(tokens).isEqualTo(listOf(
                LongPreamble(index=0),
                LongOptionName(text="csvList", index=2),
                SuperTokenSeparator(index=9),
                Argument(text="1,2,3", index=10),
                SuperTokenSeparator(index=15),
                LongPreamble(index=16),
                LongOptionName(text="varargsList", index=18),
                SuperTokenSeparator(index=29),
                Argument(text="1", index=30),
                SuperTokenSeparator(index=31),
                Argument(text="2", index=32),
                SuperTokenSeparator(index=33),
                Argument(text="3", index=34),
                SuperTokenSeparator(index=35)
        ))

    }
}