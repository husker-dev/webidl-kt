package com.huskerdev.webidl.lexer

import com.huskerdev.webidl.WebIDLEnv
import kotlin.test.Test
import kotlin.test.assertEquals

class LexerTest {

    @Test
    fun test(){
        WebIDLLexer("""
            // line-comment
            /* multi-line comment */
            
            // types
            identifier 
            interface
            "interface"
            123
            123.123
            true
            false
            null
            {}
            ()
            []
            <>
            ,
            ;
            :
            =
            ?
            ...
            *
            
            // escape sequence
            "text\b\"\r\t\\\n"
            
            // keywords
            ${WebIDLLexer.keywords.joinToString(" ")}
            
            // types
            ${WebIDLEnv.Default.builtinTypes.keys.joinToString(" ")}
            
            // integers
            -123
            0xFF
            0XFF
            0o777
            0O777
            0b1010
            0B1010
            
            // floats
            -0.5
            .5
            -.5
            1.6e23
            1.6E-23
            -1.6e23
            -1.6E-23
            Infinity
            -Infinity
            NaN
            
        """.trimIndent().asSequence().iterator()).apply {
            mapOf(
                WebIDLLexer.LexemeType.IDENTIFIER to "identifier",
                WebIDLLexer.LexemeType.KEYWORD to "interface",
                WebIDLLexer.LexemeType.STRING to "interface",
                WebIDLLexer.LexemeType.INTEGER to "123",
                WebIDLLexer.LexemeType.DECIMAL to "123.123",
                WebIDLLexer.LexemeType.TRUE to "true",
                WebIDLLexer.LexemeType.FALSE to "false",
                WebIDLLexer.LexemeType.NULL to "null",
                WebIDLLexer.LexemeType.L_CURLY_BRACKET to "{",
                WebIDLLexer.LexemeType.R_CURLY_BRACKET to "}",
                WebIDLLexer.LexemeType.L_ROUND_BRACKET to "(",
                WebIDLLexer.LexemeType.R_ROUND_BRACKET to ")",
                WebIDLLexer.LexemeType.L_SQUARE_BRACKET to "[",
                WebIDLLexer.LexemeType.R_SQUARE_BRACKET to "]",
                WebIDLLexer.LexemeType.L_ANGLE_BRACKET to "<",
                WebIDLLexer.LexemeType.R_ANGLE_BRACKET to ">",
                WebIDLLexer.LexemeType.COMMA to ",",
                WebIDLLexer.LexemeType.SEMICOLON to ";",
                WebIDLLexer.LexemeType.COLON to ":",
                WebIDLLexer.LexemeType.EQUALS to "=",
                WebIDLLexer.LexemeType.QUESTION to "?",
                WebIDLLexer.LexemeType.ELLIPSIS to "...",
                WebIDLLexer.LexemeType.WILDCARD to "*"
            ).forEach {
                assertEquals(it.key, current.type, current.content)
                assertEquals(it.value, current.content, current.content)
                next()
            }

            // string
            assertEquals(WebIDLLexer.LexemeType.STRING, current.type)
            assertEquals("text\b\"\r\t\\\n", current.content)
            next()


            WebIDLLexer.keywords.forEach {
                assertEquals(WebIDLLexer.LexemeType.KEYWORD, current.type, current.content)
                assertEquals(it, current.content)
                next()
            }

            WebIDLEnv.Default.builtinTypes.keys
                .flatMap { it.split(" ") }
                .forEach {
                    assertEquals(WebIDLLexer.LexemeType.TYPE, current.type, current.content)
                    assertEquals(it, current.content)
                    next()
                }

            setOf(
                "-123",
                "0xFF",
                "0XFF",
                "0o777",
                "0O777",
                "0b1010",
                "0B1010"
            ).forEach {
                assertEquals(WebIDLLexer.LexemeType.INTEGER, current.type, current.content)
                assertEquals(it, current.content, current.content)
                next()
            }

            setOf(
                "-0.5",
                ".5",
                "-.5",
                "1.6e23",
                "1.6E-23",
                "-1.6e23",
                "-1.6E-23",
                "Infinity",
                "-Infinity",
                "NaN"
            ).forEach {
                assertEquals(WebIDLLexer.LexemeType.DECIMAL, current.type, current.content)
                assertEquals(it, current.content, current.content)
                if(hasNext())
                    next()
            }
        }
    }
}