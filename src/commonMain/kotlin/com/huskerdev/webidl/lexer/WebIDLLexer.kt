package com.huskerdev.webidl.lexer

import com.huskerdev.webidl.IdlElementBounds
import com.huskerdev.webidl.WebIDLEnv
import com.huskerdev.webidl.WebIDLSyntaxErrorException

class WebIDLLexer(
    val chars: Iterator<Char>,
    types: Set<String> = WebIDLEnv.Default.builtinTypes.keys,
    private val keywords: Set<String> = WebIDLLexer.keywords,
    val includeSkipped: Boolean = false
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
        WILDCARD("*"),

        WHITE_SPACE(" "),
        LINE_COMMENT("//"),
        BLOCK_COMMENT("/*"),
        UNKNOWN("unknown"),
        END("EOF")
    }

    data class Lexeme(
        val content: String,
        val type: LexemeType,
        val bounds: IdlElementBounds
    )

    private val collectedErrors = arrayListOf<WebIDLSyntaxErrorException>()
    val errors: List<WebIDLSyntaxErrorException> = collectedErrors

    val types = types.flatMap { it.split(" ") }.toSet()

    private var front: Char? = null
    private var rear: Char? = null
    private var fetchedAll = false
    private var frontOffset = 0
    private var lineIndex = 0
    private var lineStartOffset = 0

    private var pending: Lexeme? = null

    lateinit var current: Lexeme
        private set

    init {
        pending = readLexeme()
        streamNext()
    }

    override fun hasNext(): Boolean = pending != null

    override fun next(): Lexeme = streamNext()

    private fun streamNext(): Lexeme {
        val token = pending ?: endLexeme()
        current = token
        pending = readLexeme()
        return token
    }

    private fun pullChar(): Char? =
        if (chars.hasNext()) chars.next() else {
            fetchedAll = true
            null
        }

    private fun ensureFront() {
        if (front == null && !fetchedAll) front = pullChar()
    }

    private fun peek(): Char? {
        ensureFront()
        return front
    }

    private fun ahead(): Char? {
        ensureFront()
        if (rear == null && !fetchedAll) rear = pullChar()
        return rear
    }

    private fun read(): Char? {
        ensureFront()
        if (front == null) return null
        val c = front
        front = rear
        rear = null
        if (c == '\n') {
            lineIndex++
            lineStartOffset = frontOffset + 1
        }
        frontOffset++
        return c
    }

    private inline fun readWhile(builder: StringBuilder, predicate: (Char) -> Boolean) {
        while (true) {
            val c = peek() ?: break
            if (!predicate(c)) break
            builder.append(read())
        }
    }

    private fun endLexeme(): Lexeme = Lexeme(
        content = "EOF",
        type = LexemeType.END,
        bounds = IdlElementBounds(frontOffset, frontOffset, 0, 0, )
    )

    private fun readLexeme(): Lexeme? {
        val builder = StringBuilder()
        while (true) {
            val start = frontOffset
            val startLineIndex = lineIndex
            val startLineCharIndex = start - lineStartOffset
            builder.setLength(0)
            val type = scanTrivia(builder) ?: break
            if (includeSkipped) {
                return makeLexeme(
                    start, frontOffset, type, builder.toString(),
                    startLineIndex, startLineCharIndex
                )
            }
        }
        return if (peek() == null) null else readToken()
    }

    private fun scanTrivia(builder: StringBuilder): LexemeType? {
        val c = peek() ?: return null
        return when (c) {
            in spaces -> {
                readWhile(builder) { it in spaces }
                LexemeType.WHITE_SPACE
            }
            '/' -> when (ahead()) {
                '/' -> {
                    builder.append(read())
                    readWhile(builder) { it != '\n' }
                    LexemeType.LINE_COMMENT
                }

                '*' -> {
                    builder.append(read())
                    builder.append(read())
                    while (true) {
                        val ch = peek() ?: break
                        if (ch == '*' && ahead() == '/') {
                            builder.append(read())
                            builder.append(read())
                            break
                        }
                        builder.append(read())
                    }
                    LexemeType.BLOCK_COMMENT
                }

                else -> null
            }
            else -> null
        }
    }

    private fun readToken(): Lexeme {
        val start = frontOffset
        val startLineIndex = lineIndex
        val startLineCharIndex = start - lineStartOffset
        val builder = StringBuilder()
        val firstChar = read()!!
        builder.append(firstChar)

        val type = try {
            when (firstChar) {

                in digits, '-', '.' -> {
                    if (firstChar == '.' && (peek() ?: '.') !in digits) {
                        val d2 = read()
                        if (peek() != '.') throw newException("Expected '...'.")
                        val d3 = read()
                        builder.append(d2).append(d3)
                        LexemeType.ELLIPSIS
                    } else {
                        readWhile(builder) { it == '.' || it !in splitters }
                        if ('.' in builder || builder.contentEquals("-Infinity"))
                            LexemeType.DECIMAL
                        else
                            LexemeType.INTEGER
                    }
                }

                !in splitters -> {
                    readWhile(builder) { it !in splitters }
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
                    else -> throw Exception() // Never throws
                }
                else -> throw newException("Unexpected token.")
            }
        } catch (e: WebIDLSyntaxErrorException) {
            collectedErrors += e
            LexemeType.UNKNOWN
        }

        return makeLexeme(
            start, frontOffset, type, builder.toString(),
            startLineIndex, startLineCharIndex
        )
    }

    private fun readString(builder: StringBuilder) {
        builder.clear()
        while (true) {
            val c = peek() ?: break
            if (c == '\"' && builder.isNotEmpty() && builder.last() != '\\') break
            builder.append(read())
            while (true) {
                val b = peek()
                if (b == null || b != '\\') break
                read()
                val escaped = peek() ?: break
                builder.append(when (escaped) {
                    'n' -> '\n'
                    '\"' -> '\"'
                    '\\' -> '\\'
                    'r' -> '\r'
                    't' -> '\t'
                    'b' -> '\b'
                    else -> collectedErrors += newException("Unsupported escape sequence.")
                })
                read()
            }
        }
        if (peek() == '\"') read()
    }

    private fun makeLexeme(
        start: Int,
        end: Int,
        type: LexemeType,
        content: String,
        startLineIndex: Int,
        startLineCharIndex: Int
    ): Lexeme = Lexeme(
        content,
        type,
        bounds = IdlElementBounds(
            startOffset = start,
            endOffset = end,
            lineIndex = startLineIndex,
            lineCharIndex = startLineCharIndex
        )
    )

    private fun newException(message: String) = WebIDLSyntaxErrorException(
        bounds = IdlElementBounds(
            startOffset = frontOffset,
            endOffset = frontOffset + 1,
            lineIndex = lineIndex,
            lineCharIndex = (frontOffset - lineStartOffset).coerceAtLeast(0)
        ),
        errorMessage = message
    )
}