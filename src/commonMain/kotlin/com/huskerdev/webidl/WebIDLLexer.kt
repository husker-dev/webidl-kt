package com.huskerdev.webidl


class WebIDLLexer(
    val iterator: Iterator<String>
): Iterator<WebIDLLexer.Lexeme> {

    companion object {
        private val spaces = setOf(' ', '\t', '\n', '\r')

        private val splitters = spaces + setOf(
            ';', ':', '{', '}', '(', ')', '[', ']', '=', ',', '\"', '<', '>', '?', '*'
        )

        private val keywords = setOf(
            "interface", "dictionary", "enum", "callback", "typedef",
            "implements", "partial", "readonly", "attribute", "const", "static",
            "inherit", "iterable", "maplike", "setlike", "includes", "namespace", "or"
        )
    }

    enum class LexemeType {
        IDENTIFIER,
        KEYWORD,
        STRING,
        INTEGER, DECIMAL,
        TRUE, FALSE,
        NULL,

        L_CURLY_BRACKET,  R_CURLY_BRACKET,
        L_ROUND_BRACKET,  R_ROUND_BRACKET,
        L_SQUARE_BRACKET, R_SQUARE_BRACKET,
        L_ANGLE_BRACKET,  R_ANGLE_BRACKET,

        COMMA, SEMICOLON, COLON,
        EQUALS, QUESTION, ELLIPSIS,
        WILDCARD
    }

    data class Lexeme(
        val content: String,
        val type: LexemeType
    )

    private var hasNext = iterator.hasNext()
    private var line = iterator.next()
    private var index = 0


    lateinit var current: Lexeme
        private set

    init {
        if(hasNext) {
            skipComments()
            skipSpaces()
        }
    }

    private fun skipComments(){
        while(currentChar() == '/') {
            nextChar(true)
            when (currentChar()) {
                '/' -> {
                    while(currentChar() != '\n')
                        nextChar(true)
                }
                '*' -> {
                    while(
                        nextCharAndGet(true) != '*' ||
                        nextCharAndGet(true) != '/'
                    ) Unit
                }
            }
            nextChar(true)
        }
    }

    private fun nextChar(
        ignoreComment: Boolean = false
    ): Boolean {
        if(index == line.lastIndex) {
            if(!iterator.hasNext()) {
                index++
                hasNext = false
                return false
            }
            line = iterator.next()
            index = 0
        } else
            index++

        if(!ignoreComment)
            skipComments()
        return true
    }

    private fun nextCharAndGet(ignoreComment: Boolean = false): Char {
        nextChar(ignoreComment)
        return currentChar()
    }

    private fun currentChar(): Char =
        line[index]

    private fun skipSpaces(){
        while(
            index != line.length &&
            currentChar() in spaces
        ) nextChar()
    }

    override fun next(): Lexeme {
        val char = currentChar()
        nextChar()

        val builder = StringBuilder()
        builder.append(char)

        val type = when (char) {
            !in splitters -> {
                // Default chars

                while (currentChar() !in splitters) {
                    builder.append(currentChar())
                    nextChar()
                }

                when (builder.toString()) {
                    in keywords -> LexemeType.KEYWORD
                    "true" -> LexemeType.TRUE
                    "false" -> LexemeType.FALSE
                    "null" -> LexemeType.NULL
                    else -> {
                        if (char.isDigit()) {
                            if("." in builder)
                                LexemeType.DECIMAL
                            else LexemeType.INTEGER
                        } else LexemeType.IDENTIFIER
                    }
                }
            }

            '.' -> {
                repeat(2) {
                    if (currentChar() != '.')
                        throw UnsupportedOperationException("Expected '...'")
                    builder.append(".")
                    nextChar()
                }
                LexemeType.ELLIPSIS
            }

            else -> {
                // Special chars

                when (char) {
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
                        builder.clear()
                        while (currentChar() != '\"') {
                            builder.append(currentChar())
                            nextChar()
                        }
                        nextChar()
                        LexemeType.STRING
                    }

                    else -> throw UnsupportedOperationException()
                }
            }
        }

        skipSpaces()

        current = Lexeme(builder.toString(), type)
        return current
    }

    override fun hasNext(): Boolean =
        hasNext
}