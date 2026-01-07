package com.huskerdev.webidl.parser

import com.huskerdev.webidl.WebIDLParserException
import com.huskerdev.webidl.lexer.WebIDLLexer


internal val modifiers = setOf(
    "partial", "readonly", "attribute",
    "const", "static", "inherit", "optional"
)

internal open class Modifiers(
    val lexemes: List<WebIDLLexer.Lexeme>
) {
    object EMPTY: Modifiers(emptyList())

    init {
        lexemes.forEach { modifier ->
            val collected = lexemes.filter { it.content == modifier.content }
            if(collected.size > 1)
                throw WebIDLParserException(collected[1], "Duplicate modifier")
        }
    }

    fun assertAllowed(vararg allowed: String){
        val allowed = hashSetOf(*allowed)
        val remain = lexemes.filter { it.content !in allowed }
        if(remain.isNotEmpty())
            throw WebIDLParserException(remain[0], "Modifier '${remain[0]}' is not allowed here")
    }

    fun get(modifier: String, isAllowed: Boolean = true): Boolean {
        val result = lexemes.firstOrNull { it.content == modifier }
        if(result != null && !isAllowed)
            throw WebIDLParserException(result, "Modifier '${result.content}' is not allowed here")
        return result != null
    }
}