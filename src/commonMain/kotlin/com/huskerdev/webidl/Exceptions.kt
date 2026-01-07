package com.huskerdev.webidl

import com.huskerdev.webidl.lexer.WebIDLLexer
import kotlin.math.max


open class WebIDLParserException(
    lineIndex: Int,
    line: CharSequence,
    lineCharIndex: Int,
    @Suppress("unused") val errorMessage: String
): Exception(createString(lineIndex, line, lineCharIndex, errorMessage)) {

    constructor(
        lexeme: WebIDLLexer.Lexeme,
        errorMessage: String
    ): this(lexeme.lineIndex, lexeme.line, lexeme.lineCharIndex+1, errorMessage)

    companion object {
        private fun createString(
            lineIndex: Int,
            line: CharSequence,
            lineCharIndex: Int,
            message: String?
        ): String = buildString {
            append("Syntax error at line ")
            append(lineIndex+1)
            append(":\n")
            append(line)
            append('\n')
            append(" ".repeat(max(0, lineCharIndex-1)))
            append("^ ")
            append(message)
            append('\n')
        }
    }
}

class WebIDLWrongSymbolException(
    lexeme: WebIDLLexer.Lexeme,
    expected: String
): WebIDLParserException(lexeme, "Expected '$expected' but found: '${lexeme.content}'")

class WebIDLUnexpectedSymbolException(
    lexeme: WebIDLLexer.Lexeme,
    content: String
): WebIDLParserException(lexeme, "Unexpected symbol: $content")


internal fun expectType(
    lexeme: WebIDLLexer.Lexeme,
    type: WebIDLLexer.LexemeType,
    typeString: String = type.word
){
    if(lexeme.type != type)
        throw WebIDLWrongSymbolException(lexeme, typeString)
}