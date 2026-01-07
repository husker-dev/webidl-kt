package com.huskerdev.webidl.lexer

import com.huskerdev.webidl.WebIDLEnv
import com.huskerdev.webidl.WebIDLParserException

class WebIDLLexer(
    val chars: Iterator<Char>,
    types: Set<String> = WebIDLEnv.Default.builtinTypes.keys
): Iterator<WebIDLLexer.Lexeme> {

    companion object {
        private val spaces = setOf(' ', '\t', '\n', '\r')

        private val splitters = spaces + setOf(
            ';', ':', '{', '}', '(', ')', '[', ']', '=', ',', '\"', '<', '>', '?', '*', '.'
        )

        private val digits = setOf(
            '0', '1', '2', '3', '4', '5', '6', '7', '8', '9'
        )

        val keywords = setOf(
            "interface", "dictionary", "enum", "callback", "typedef",
            "implements", "partial", "readonly", "attribute", "const", "static",
            "inherit", "iterable", "maplike", "setlike", "includes", "namespace", "or"
        )
    }

    enum class LexemeType(
        val word: String
    ) {
        IDENTIFIER("identifier"),
        TYPE("type"),
        KEYWORD("keyword"),
        STRING("string"),
        INTEGER("integer"),
        DECIMAL("decimal"),
        TRUE("true"),
        FALSE("false"),
        NULL("null"),

        L_CURLY_BRACKET("{"),  R_CURLY_BRACKET("}"),
        L_ROUND_BRACKET("("),  R_ROUND_BRACKET(")"),
        L_SQUARE_BRACKET("["), R_SQUARE_BRACKET("]"),
        L_ANGLE_BRACKET("<"),  R_ANGLE_BRACKET(">"),

        COMMA(","),
        SEMICOLON(";"),
        COLON(":"),
        EQUALS("="),
        QUESTION("?"),
        ELLIPSIS("..."),
        WILDCARD("*")
    }

    data class Lexeme(
        val content: String,
        val type: LexemeType,
        val line: String,
        val lineIndex: Int,
        val lineCharIndex: Int
    )

    val types = types.flatMap { it.split(" ") }.toSet()

    private var hasNext = chars.hasNext()
    private var char = if(hasNext) chars.next() else '\n'

    private val cachedLine = StringBuilder().append(char)
    private var lineIndex = 0

    lateinit var current: Lexeme
        private set

    init {
        if(hasNext) {
            skipComments()
            skipSpaces()
            next()
        }
    }

    private fun throwException(message: String): Nothing =
        throw WebIDLParserException(lineIndex, cachedLine, cachedLine.length, message)

    private fun nextChar(
        ignoreComment: Boolean = false
    ): Char {
        if(char == ';' || char == '\n')
            cachedLine.clear()
        if(char == '\n')
            lineIndex++

        if(!chars.hasNext()) {
            char = '\n'
            hasNext = false
            return '\n'
        }
        char = chars.next()
        cachedLine.append(char)

        if(!ignoreComment)
            skipComments()
        return char
    }

    @Suppress("UnusedExpression")
    private fun skipComments(){
        while(char == '/') {
            nextChar(true)
            when (char) {
                '/' -> while(char != '\n')
                    nextChar(true)
                '*' -> while(
                    nextChar(true) != '*' ||
                    nextChar(true) != '/'
                ) Unit
            }
            cachedLine.clear()
            nextChar(true)
        }
    }

    private fun skipSpaces(){
        while(hasNext && char in spaces)
            nextChar()
    }

    private fun readString(builder: StringBuilder){
        builder.clear()
        while (char != '\"' || builder.lastOrNull() == '\\') {
            builder.append(char)
            nextChar()

            // Check escape sequence
            while(char == '\\') {
                nextChar()
                builder.append(when(char) {
                    'n' -> '\n'
                    '\"' -> '"'
                    '\\' -> '\\'
                    'r' -> '\r'
                    't' -> '\t'
                    'b' -> '\b'
                    else -> throwException("Unsupported escape sequence")
                })
                nextChar()
            }
        }
        nextChar()
    }

    override fun hasNext(): Boolean = hasNext

    override fun next(): Lexeme {
        val firstCharIndex = cachedLine.length-1
        val firstChar = char
        nextChar()

        val builder = StringBuilder()
        builder.append(firstChar)

        val type = when (firstChar) {

            // Numbers (except 'Infinity' and 'NaN', but with '-Infinity')
            // Also contains ellipsis
            in digits, '-', '.' -> {
                if(firstChar == '.' && char !in digits) {
                    // Ellipsis '...'
                    if(char != '.' || nextChar() != '.')
                        throwException("Expected '...'")
                    nextChar()
                    builder.append("..")
                    LexemeType.ELLIPSIS
                } else {
                    // Number
                    while (char == '.' || char !in splitters) {
                        builder.append(char)
                        nextChar()
                    }
                    if ('.' in builder || builder.contentEquals("-Infinity"))
                        LexemeType.DECIMAL
                    else
                        LexemeType.INTEGER
                }
            }

            // Long words
            !in splitters -> {
                while (char !in splitters) {
                    builder.append(char)
                    nextChar()
                }
                when (builder.toString()) {
                    in keywords -> LexemeType.KEYWORD
                    in types -> LexemeType.TYPE
                    "true" -> LexemeType.TRUE
                    "false" -> LexemeType.FALSE
                    "null" -> LexemeType.NULL
                    "Infinity" -> LexemeType.DECIMAL
                    "NaN" -> LexemeType.DECIMAL
                    else -> LexemeType.IDENTIFIER
                }
            }

            // Single-letter
            in splitters -> when (firstChar) {
                '<' -> LexemeType.L_ANGLE_BRACKET
                '>' -> LexemeType.R_ANGLE_BRACKET
                '(' -> LexemeType.L_ROUND_BRACKET
                ')' -> LexemeType.R_ROUND_BRACKET
                '{' -> LexemeType.L_CURLY_BRACKET
                '}' -> LexemeType.R_CURLY_BRACKET
                '[' -> LexemeType.L_SQUARE_BRACKET
                ']' -> LexemeType.R_SQUARE_BRACKET
                ',' -> LexemeType.COMMA
                ';' -> LexemeType.SEMICOLON
                ':' -> LexemeType.COLON
                '=' -> LexemeType.EQUALS
                '?' -> LexemeType.QUESTION
                '*' -> LexemeType.WILDCARD
                '\"' -> {
                    readString(builder)
                    LexemeType.STRING
                }
                else -> throwException("Unexpected char")
            }
            else -> throwException("Unexpected token")
        }
        skipSpaces()

        current = Lexeme(builder.toString(), type, cachedLine.toString(), lineIndex, firstCharIndex)
        return current
    }
}