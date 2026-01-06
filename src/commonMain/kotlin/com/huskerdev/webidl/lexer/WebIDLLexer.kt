package com.huskerdev.webidl.lexer

import com.huskerdev.webidl.WebIDLEnv

class WebIDLLexer(
    val iterator: Iterator<String>,
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
        val type: LexemeType
    )

    val types = types.flatMap { it.split(" ") }.toSet()

    private var hasNext = iterator.hasNext()
    private var line = iterator.next()
    private var index = 0

    lateinit var current: Lexeme
        private set

    init {
        if(hasNext) {
            skipComments()
            skipSpaces()
            next()
        }
    }

    private fun nextChar(
        ignoreComment: Boolean = false
    ): Char {
        if(index == line.lastIndex) {
            if(!iterator.hasNext()) {
                line = "\n"
                index = 0
                hasNext = false
                return '\n'
            }
            line = iterator.next()
            index = 0
        } else
            index++

        if(!ignoreComment)
            skipComments()
        return currentChar()
    }

    private fun currentChar(): Char =
        line[index]

    @Suppress("UnusedExpression")
    private fun skipComments(){
        while(currentChar() == '/') {
            nextChar(true)
            when (currentChar()) {
                '/' -> while(currentChar() != '\n')
                    nextChar(true)
                '*' -> while(
                    nextChar(true) != '*' ||
                    nextChar(true) != '/'
                ) Unit
            }
            nextChar(true)
        }
    }

    private fun skipSpaces(){
        while(hasNext && currentChar() in spaces)
            nextChar()
    }

    private fun readString(builder: StringBuilder){
        builder.clear()
        while (currentChar() != '\"' || builder.lastOrNull() == '\\') {
            builder.append(currentChar())
            nextChar()

            // Check escape sequence
            while(currentChar() == '\\') {
                nextChar()
                builder.append(when(currentChar()) {
                    'n' -> '\n'
                    '\"' -> '"'
                    '\\' -> '\\'
                    'r' -> '\r'
                    't' -> '\t'
                    'b' -> '\b'
                    else -> throw UnsupportedOperationException("Unsupported escape sequence (${currentChar()})")
                })
                nextChar()
            }
        }
        nextChar()
    }

    override fun hasNext(): Boolean = hasNext

    override fun next(): Lexeme {
        val char = currentChar()
        nextChar()

        val builder = StringBuilder()
        builder.append(char)

        val type = when (char) {

            // Numbers (except 'Infinity' and 'NaN', but with '-Infinity')
            // Also contains ellipsis
            in digits, '-', '.' -> {
                if(char == '.' && currentChar() !in digits) {
                    // Ellipsis '...'
                    if(currentChar() != '.')
                        throw UnsupportedOperationException("Expected '...'")
                    if(nextChar() != '.')
                        throw UnsupportedOperationException("Expected '...'")
                    nextChar()
                    builder.append("..")
                    LexemeType.ELLIPSIS
                } else {
                    // Number
                    while (currentChar() == '.' || currentChar() !in splitters) {
                        builder.append(currentChar())
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
                while (currentChar() !in splitters) {
                    builder.append(currentChar())
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
            in splitters -> when (char) {
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
                else -> throw UnsupportedOperationException("$char")
            }
            else -> throw UnsupportedOperationException("Unexpected letter")
        }
        skipSpaces()

        current = Lexeme(builder.toString(), type)
        return current
    }
}