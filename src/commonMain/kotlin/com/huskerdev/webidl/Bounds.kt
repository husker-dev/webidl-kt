package com.huskerdev.webidl

import com.huskerdev.webidl.lexer.WebIDLLexer
import com.huskerdev.webidl.parser.IdlDefinition
import kotlinx.serialization.Serializable

const val LOCATION_NOT_SPECIFIED = -1

@Serializable
data class IdlElementBounds(
    val startOffset: Int = LOCATION_NOT_SPECIFIED,
    val endOffset: Int = LOCATION_NOT_SPECIFIED,
    val lineIndex: Int = LOCATION_NOT_SPECIFIED,
    val lineCharIndex: Int = LOCATION_NOT_SPECIFIED
) {
    constructor(lexeme: WebIDLLexer.Lexeme): this(
        startOffset = lexeme.bounds.startOffset,
        endOffset = lexeme.bounds.endOffset,
        lineIndex = lexeme.bounds.lineIndex,
        lineCharIndex = lexeme.bounds.lineCharIndex
    )
    constructor(from: WebIDLLexer.Lexeme, to: WebIDLLexer.Lexeme): this(
        startOffset = from.bounds.startOffset,
        endOffset = to.bounds.endOffset,
        lineIndex = from.bounds.lineIndex,
        lineCharIndex = from.bounds.lineCharIndex
    )
}

fun IdlDefinition.locateAt(
    startOffset: Int,
    lineIndex: Int,
    lineCharIndex: Int
) {
    bounds = IdlElementBounds(
        startOffset = startOffset,
        endOffset = bounds.endOffset,
        lineIndex = lineIndex,
        lineCharIndex = lineCharIndex,
    )
}

fun IdlDefinition.locateAt(lexeme: WebIDLLexer.Lexeme) =
    locateAt(lexeme.bounds.startOffset, lexeme.bounds.lineIndex, lexeme.bounds.lineCharIndex)

fun IdlDefinition.endAt(endOffset: Int) {
    bounds = IdlElementBounds(
        startOffset = bounds.startOffset,
        endOffset = endOffset,
        lineIndex = bounds.lineIndex,
        lineCharIndex = bounds.lineCharIndex,
    )
}