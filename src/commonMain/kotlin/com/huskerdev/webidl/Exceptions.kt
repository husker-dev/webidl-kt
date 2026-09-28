package com.huskerdev.webidl

import com.huskerdev.webidl.lexer.WebIDLLexer

@Suppress("unused")
open class WebIDLErrorException(
    val bounds: IdlElementBounds,
    val errorTitle: String,
    val errorMessage: String
): Exception("$errorTitle | $errorMessage $bounds")

// Syntax error

open class WebIDLSyntaxErrorException(
    bounds: IdlElementBounds,
    errorMessage: String
): WebIDLErrorException(
    bounds = bounds,
    errorTitle = "Syntax error",
    errorMessage = errorMessage
) {
    constructor(
        lexeme: WebIDLLexer.Lexeme,
        errorMessage: String
    ): this(
        lexeme.bounds,
        errorMessage = errorMessage
    )
}

class WebIDLWrongSymbolException(
    lexeme: WebIDLLexer.Lexeme,
    expected: String
): WebIDLSyntaxErrorException(lexeme, "Expected '$expected' but found: '${lexeme.content}'.")

class WebIDLUnexpectedSymbolException(
    lexeme: WebIDLLexer.Lexeme,
    content: String
): WebIDLSyntaxErrorException(lexeme, "Unexpected symbol: $content.")

internal fun expectType(
    lexeme: WebIDLLexer.Lexeme,
    type: WebIDLLexer.LexemeType,
    typeString: String = type.word
){
    if(lexeme.type != type)
        throw WebIDLWrongSymbolException(lexeme, typeString)
}

// Type error

open class WebIDLTypeErrorException(
    bounds: IdlElementBounds,
    message: String
): WebIDLErrorException(
    bounds = bounds,
    errorTitle = "Type error",
    errorMessage = message
)

open class WebIDLUnresolvedReferenceException(
    bounds: IdlElementBounds,
    name: String
): WebIDLTypeErrorException(
    bounds = bounds,
    message = "Unresolved reference '$name'."
)