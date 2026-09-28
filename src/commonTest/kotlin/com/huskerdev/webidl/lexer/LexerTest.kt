package com.huskerdev.webidl.lexer

import com.huskerdev.webidl.WebIDLEnv
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

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

    private fun lexemes(
        source: String,
        includeSkipped: Boolean = false,
        keywords: Set<String> = WebIDLLexer.keywords
    ): List<WebIDLLexer.Lexeme> {
        val result = ArrayList<WebIDLLexer.Lexeme>()
        val lexer = WebIDLLexer(
            source.asSequence().iterator(),
            keywords = keywords,
            includeSkipped = includeSkipped
        )
        if (lexer.current.type != WebIDLLexer.LexemeType.END) result.add(lexer.current)
        while (lexer.hasNext()) result.add(lexer.next())
        return result
    }

    @Test
    fun offsets() {
        val source = "foo bar = 42;"
        val tokens = lexemes(source, includeSkipped = true)
        var previousEnd = 0
        tokens.forEach {
            assertEquals(previousEnd, it.bounds.startOffset, it.content)
            assertEquals(previousEnd + it.content.length, it.bounds.endOffset, it.content)
            assertEquals(source.substring(it.bounds.startOffset, it.bounds.endOffset), it.content)
            previousEnd = it.bounds.endOffset
        }
        assertEquals(source.length, previousEnd)
    }

    @Test
    fun includeSkipped() {
        val source = "// c\nfoo /* b */ 42"
        val tokens = lexemes(source, includeSkipped = true)
        assertEquals(
            listOf(
                WebIDLLexer.LexemeType.LINE_COMMENT,
                WebIDLLexer.LexemeType.WHITE_SPACE,
                WebIDLLexer.LexemeType.IDENTIFIER,
                WebIDLLexer.LexemeType.WHITE_SPACE,
                WebIDLLexer.LexemeType.BLOCK_COMMENT,
                WebIDLLexer.LexemeType.WHITE_SPACE,
                WebIDLLexer.LexemeType.INTEGER
            ),
            tokens.map { it.type },
            tokens.joinToString { it.type.toString() + ':' + it.content }
        )
        assertEquals("// c", tokens[0].content)
        assertEquals("\n", tokens[1].content)
        assertEquals("foo", tokens[2].content)
        assertEquals(" ", tokens[3].content)
        assertEquals("/* b */", tokens[4].content)
        assertEquals(" ", tokens[5].content)
        assertEquals("42", tokens[6].content)

        var previousEnd = 0
        tokens.forEach {
            assertEquals(previousEnd, it.bounds.startOffset)
            previousEnd = it.bounds.endOffset
        }
        assertEquals(source.length, previousEnd)
    }

    @Test
    fun customKeywords() {
        val tokens = lexemes("foo or", keywords = setOf("foo"))
        assertEquals(WebIDLLexer.LexemeType.KEYWORD, tokens[0].type)
        assertEquals(WebIDLLexer.LexemeType.IDENTIFIER, tokens[1].type)
    }

    @Test
    fun skippedOnlyInput() {
        assertFalse(lexemes("   \n\t").isNotEmpty())
        assertFalse(lexemes("// x\n").isNotEmpty())
        assertFalse(lexemes("/* x */").isNotEmpty())
        assertFalse(lexemes("").isNotEmpty())
    }

    @Test
    fun unterminatedString() {
        val tokens = lexemes("\"abc")
        assertEquals(WebIDLLexer.LexemeType.STRING, tokens[0].type)
        assertEquals("abc", tokens[0].content)
        assertEquals(1, tokens.size)
    }

    @Test
    fun endLexemeOffsets() {
        val source = "foo"
        val lexer = WebIDLLexer(source.asSequence().iterator())
        lexemes(source)
        assertEquals("foo", lexer.current.content)
        val end = lexer.next()
        assertEquals(WebIDLLexer.LexemeType.END, end.type)
        assertEquals(3, end.bounds.startOffset)
        assertEquals(3, end.bounds.endOffset)
    }

    @Test
    fun linePositions() {
        val source = "foo\n   bar"
        val tokens = lexemes(source, includeSkipped = true)
        val foo = tokens.first { it.type == WebIDLLexer.LexemeType.IDENTIFIER }
        assertEquals(0, foo.bounds.lineIndex)
        assertEquals(0, foo.bounds.lineCharIndex)
        val bar = tokens.last { it.type == WebIDLLexer.LexemeType.IDENTIFIER }
        assertEquals(1, bar.bounds.lineIndex)
        assertEquals(3, bar.bounds.lineCharIndex)
    }
}