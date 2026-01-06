package com.huskerdev.webidl.parser

import com.huskerdev.webidl.WebIDLEnv
import com.huskerdev.webidl.lexer.WebIDLLexer


private val openBrackets = setOf(
    WebIDLLexer.LexemeType.L_ANGLE_BRACKET,
    WebIDLLexer.LexemeType.L_ROUND_BRACKET,
    WebIDLLexer.LexemeType.L_CURLY_BRACKET,
    WebIDLLexer.LexemeType.L_SQUARE_BRACKET,
)

private val closeBrackets = setOf(
    WebIDLLexer.LexemeType.L_ANGLE_BRACKET,
    WebIDLLexer.LexemeType.L_ROUND_BRACKET,
    WebIDLLexer.LexemeType.L_CURLY_BRACKET,
    WebIDLLexer.LexemeType.L_SQUARE_BRACKET,
)

private val modifiers = setOf(
    "partial", "readonly", "attribute",
    "const", "static", "inherit", "optional"
)

class WrongSymbolException(expected: String, got: String): Exception("Expected: $expected, but got: $got")
class UnexpectedSymbolException(content: String): Exception("Unexpected symbol: $content")

private fun expectType(
    lexeme: WebIDLLexer.Lexeme,
    type: WebIDLLexer.LexemeType,
    message: String = type.word
){
    if(lexeme.type != type)
        throw WrongSymbolException(message, "'${lexeme.content}'")
}


class WebIDLParser(
    iterator: Iterator<String>,
    types: Set<String> = WebIDLEnv.Default.builtinTypes.keys
) {
    private val lexer = WebIDLLexer(iterator, types)

    fun parse(): WebIDLDefinitionRoot {
        return WebIDLDefinitionRoot(walkDefinitionsBlock(false) { attributes, modifiers ->
            when(lexer.current.type) {

                // Definitions
                WebIDLLexer.LexemeType.KEYWORD -> when (lexer.current.content) {
                    "interface", -> parseInterface(attributes, modifiers)
                    "dictionary" -> parseDictionary(attributes, modifiers)
                    "namespace" -> parseNamespace(attributes, modifiers)
                    "enum" -> parseEnum(attributes)
                    "typedef" -> parseTypeDef(attributes)
                    "callback" -> when(lexer.next().type) {
                        WebIDLLexer.LexemeType.IDENTIFIER -> parseCallbackFunction(attributes)
                        else -> parseInterface(attributes, modifiers, isCallback = true)
                    }
                    else -> throw UnexpectedSymbolException(lexer.current.content)
                }

                // includes/implements
                WebIDLLexer.LexemeType.IDENTIFIER -> {
                    val identifier1 = lexer.current
                    val action = lexer.next()
                    val identifier2 = lexer.next()

                    expectType(action, WebIDLLexer.LexemeType.KEYWORD, "'includes' or 'implements'")
                    expectType(identifier2, WebIDLLexer.LexemeType.IDENTIFIER)
                    lexer.next()

                    when (action.content) {
                        "includes" -> WebIDLIncludesDef(identifier1.content, identifier2.content)
                        "implements" -> WebIDLImplementsDef(identifier1.content, identifier2.content)
                        else -> throw UnsupportedOperationException()
                    }
                }
                else -> throw UnexpectedSymbolException(lexer.current.content)
            }
        })
    }

    private fun walkDefinitionsBlock(
        brackets: Boolean = true,
        onlyOne: Boolean = false,
        onDefinition: (attributes: List<WebIDLExtendedAttributeDef>, modifiers: List<String>) -> WebIDLDefinition
    ): List<WebIDLDefinition> {
        if(brackets) {
            expectType(lexer.current, WebIDLLexer.LexemeType.L_CURLY_BRACKET)
            lexer.next()
        }

        return buildList {
            var curModifiers = arrayListOf<String>()
            var curAttributes = emptyList<WebIDLExtendedAttributeDef>()

            while (lexer.hasNext() && lexer.current.type != WebIDLLexer.LexemeType.R_CURLY_BRACKET) {
                when {
                    lexer.current.type == WebIDLLexer.LexemeType.L_SQUARE_BRACKET -> {
                        if (curModifiers.isNotEmpty())
                            throw UnexpectedSymbolException("'['")
                        curAttributes = parseExtendedAttributes()
                    }

                    lexer.current.content in modifiers -> {
                        curModifiers += lexer.current.content
                        lexer.next()
                    }

                    else -> {
                        this += onDefinition(curAttributes, curModifiers)

                        if(onlyOne)
                            return@buildList

                        expectType(lexer.current, WebIDLLexer.LexemeType.SEMICOLON)
                        if(lexer.hasNext())
                            lexer.next()

                        curModifiers = arrayListOf()
                        curAttributes = arrayListOf()
                    }
                }
            }
            if(lexer.hasNext())
                lexer.next()
        }
    }

    private fun parseType(): List<WebIDLLexer.Lexeme> = buildList {
        when (lexer.current.type) {

            // identifier
            WebIDLLexer.LexemeType.IDENTIFIER -> {
                add(lexer.current)
                lexer.next()
            }

            // union type
            WebIDLLexer.LexemeType.L_ROUND_BRACKET -> {
                add(lexer.current)  // (
                lexer.next()
                while(lexer.current.type != WebIDLLexer.LexemeType.R_ROUND_BRACKET) {
                    addAll(parseType())
                    add(lexer.current) // 'or' or ')'
                    if(lexer.current.content == "or")
                        lexer.next()
                }
                lexer.next()
            }

            // builtin-types
            WebIDLLexer.LexemeType.TYPE -> {
                add(lexer.current)
                lexer.next()

                when (lexer.current.type) {
                    // generics (sequence<T>, record<K, V>)
                    WebIDLLexer.LexemeType.L_ANGLE_BRACKET -> {
                        add(lexer.current)  // <
                        lexer.next()
                        while(lexer.current.type != WebIDLLexer.LexemeType.R_ANGLE_BRACKET) {
                            addAll(parseType())
                            add(lexer.current) // ',' or '>'
                            if(lexer.current.type == WebIDLLexer.LexemeType.COMMA)
                                lexer.next()
                        }
                        lexer.next()
                    }

                    // long types
                    WebIDLLexer.LexemeType.TYPE -> {
                        add(lexer.current)
                        while(lexer.next().type == WebIDLLexer.LexemeType.TYPE)
                            add(lexer.current)
                    }

                    // simple types
                    else -> Unit
                }
            }
            else -> expectType(lexer.current, WebIDLLexer.LexemeType.TYPE)
        }
        if(lexer.current.type == WebIDLLexer.LexemeType.QUESTION) {
            add(lexer.current)
            lexer.next()
        }
    }

    private fun parseNamespace(
        attributes: List<WebIDLExtendedAttributeDef>,
        modifiers: List<String>
    ): WebIDLNamespaceDef {
        val modifiers = modifiers.toMutableSet()
        val isPartial = modifiers.remove("partial")

        if(modifiers.isNotEmpty())
            throw UnsupportedOperationException("Unsupported or duplicate modifiers with namespace: $modifiers")

        expectType(lexer.next(), WebIDLLexer.LexemeType.IDENTIFIER)
        val name = lexer.current.content
        lexer.next()

        val definitions = walkDefinitionsBlock { attributes, modifiers ->
            parseFieldOrFunction(attributes, modifiers, allowReadonly = true, allowAttribute = true)
        }
        return WebIDLNamespaceDef(name, isPartial, definitions, attributes)
    }

    private fun parseCallbackFunction(
        attributes: List<WebIDLExtendedAttributeDef>,
    ): WebIDLCallbackFunctionDef {
        expectType(lexer.current, WebIDLLexer.LexemeType.IDENTIFIER)
        val name = lexer.current.content

        expectType(lexer.next(), WebIDLLexer.LexemeType.EQUALS)
        lexer.next()

        val function = parseFieldOrFunction(attributes, emptyList(), allowAnonymous = true)

        if(function !is WebIDLFunctionDef || function.name.isNotEmpty())
            throw UnsupportedOperationException("Expected anonymous function")

        return WebIDLCallbackFunctionDef(name, function, attributes)
    }

    private fun parseTypeDef(
        attributes: List<WebIDLExtendedAttributeDef>,
    ): WebIDLTypeDefDef {
        lexer.next()
        val type = parseType()

        expectType(lexer.current, WebIDLLexer.LexemeType.IDENTIFIER)
        val name = lexer.current.content
        lexer.next()

        return WebIDLTypeDefDef(
            name,
            type,
            attributes
        )
    }

    private fun parseEnum(
        attributes: List<WebIDLExtendedAttributeDef>,
    ): WebIDLEnumDef {
        expectType(lexer.next(), WebIDLLexer.LexemeType.IDENTIFIER)
        val name = lexer.current.content

        expectType(lexer.next(), WebIDLLexer.LexemeType.L_CURLY_BRACKET)
        lexer.next()

        val elements = arrayListOf<String>()

        while(lexer.current.type != WebIDLLexer.LexemeType.R_CURLY_BRACKET) {
            expectType(lexer.current, WebIDLLexer.LexemeType.STRING)

            elements += lexer.current.content

            if(lexer.next().type == WebIDLLexer.LexemeType.COMMA)
                lexer.next()
        }
        lexer.next()
        return WebIDLEnumDef(name, elements, attributes)
    }

    private fun parseDictionary(
        attributes: List<WebIDLExtendedAttributeDef>,
        modifiers: List<String>
    ): WebIDLDictionaryDef {
        val modifiers = modifiers.toMutableSet()
        val isPartial = modifiers.remove("partial")
        if(modifiers.isNotEmpty())
            throw UnsupportedOperationException("Unsupported or duplicate modifiers with maplike: $modifiers")

        expectType(lexer.next(), WebIDLLexer.LexemeType.IDENTIFIER)
        val name = lexer.current.content

        val implements: String? = if(lexer.next().type == WebIDLLexer.LexemeType.COLON) {
            expectType(lexer.next(), WebIDLLexer.LexemeType.IDENTIFIER)
            lexer.current.content.also {
                lexer.next()
            }
        } else null

        val definitions = walkDefinitionsBlock { attributes, modifiers ->
            if(attributes.isNotEmpty())
                throw UnsupportedOperationException("Dictionary members must not have modifiers")
            parseFieldOrFunction(
                attributes, modifiers,
                allowOptional = true,
                allowRequired = true
            )
        }
        return WebIDLDictionaryDef(name, implements, isPartial, definitions, attributes)
    }

    private fun parseInterface(
        attributes: List<WebIDLExtendedAttributeDef>,
        modifiers: List<String>,
        isCallback: Boolean = false,
    ): WebIDLInterfaceDef {
        val isMixin = if(lexer.next().content == "mixin"){
            if(isCallback)
                throw UnsupportedOperationException("Can not use mixin with callback interface")
            lexer.next()
            true
        } else false

        val modifiers = modifiers.toMutableSet()
        val isPartial = modifiers.remove("partial")

        if(modifiers.isNotEmpty())
            throw UnsupportedOperationException("Unsupported or duplicate modifiers with interface: $modifiers")

        expectType(lexer.current, WebIDLLexer.LexemeType.IDENTIFIER)
        val name = lexer.current.content

        val implements: String? = if(lexer.next().type == WebIDLLexer.LexemeType.COLON) {
            expectType(lexer.next(), WebIDLLexer.LexemeType.IDENTIFIER)
            lexer.current.content.also {
                lexer.next()
            }
        } else null

        val definitions = walkDefinitionsBlock { attributes, modifiers ->
            when(lexer.current.content) {
                "iterable" -> parseGeneric().run {
                    WebIDLIterableDef(get(0), getOrNull(1))
                }
                "async_iterable" -> parseGeneric().run {
                    WebIDLAsyncIterableLikeDef(get(0), getOrNull(1))
                }
                "maplike" -> parseGeneric().run {
                    val modifiers = modifiers.toMutableSet()
                    val isReadOnly = modifiers.remove("readonly")
                    if(modifiers.isNotEmpty())
                        throw UnsupportedOperationException("Unsupported or duplicate modifiers with maplike: $modifiers")

                    WebIDLMapLikeDef(this[0], this[1], isReadOnly)
                }
                "setlike" -> parseGeneric().run {
                    val modifiers = modifiers.toMutableSet()
                    val isReadOnly = modifiers.remove("readonly")
                    if(modifiers.isNotEmpty())
                        throw UnsupportedOperationException("Unsupported or duplicate modifiers with maplike: $modifiers")

                    WebIDLSetLikeDef(this[0], isReadOnly)
                }
                "stringifier" -> {
                    val field = if(lexer.next().type != WebIDLLexer.LexemeType.SEMICOLON) {
                        walkDefinitionsBlock(brackets = false, onlyOne = true) { attributes, modifiers ->
                            parseFieldOrFunction(
                                attributes, modifiers,
                                allowInherit = true,
                                allowAttribute = true,
                                allowReadonly = true
                            )
                        }[0]
                    } else null

                    if(field != null && field !is WebIDLFieldDef)
                        throw UnsupportedOperationException("Expected field")

                    WebIDLStringifierDef(field)
                }
                "getter" -> {
                    lexer.next()
                    val function = walkDefinitionsBlock(brackets = false, onlyOne = true) { attributes, modifiers ->
                        parseFieldOrFunction(
                            attributes, modifiers,
                            allowAnonymous = true
                        )
                    }[0]
                    if(function !is WebIDLFunctionDef)
                        throw UnsupportedOperationException("Expected function")

                    WebIDLGetterDef(function)
                }
                "setter" -> {
                    lexer.next()
                    val function = walkDefinitionsBlock(brackets = false, onlyOne = true) { attributes, modifiers ->
                        parseFieldOrFunction(
                            attributes, modifiers,
                            allowAnonymous = true
                        )
                    }[0]
                    if(function !is WebIDLFunctionDef)
                        throw UnsupportedOperationException("Expected function")

                    WebIDLSetterDef(function)
                }
                "constructor" -> {
                    if(lexer.next().type != WebIDLLexer.LexemeType.L_ROUND_BRACKET)
                        throw UnsupportedOperationException("Expected '('")
                    else lexer.next()

                    WebIDLConstructorDef(parseArguments(), attributes)
                }
                else -> {
                    parseFieldOrFunction(
                        attributes, modifiers,
                        allowConst = true,
                        allowStatic = true,
                        allowInherit = true,
                        allowAttribute = true,
                        allowReadonly = true
                    )
                }
            }
        }

        return WebIDLInterfaceDef(
            name,
            isPartial, isMixin, isCallback,
            implements,
            definitions,
            attributes
        )
    }

    private fun parseGeneric(): List<List<WebIDLLexer.Lexeme>> {
        val list = arrayListOf<List<WebIDLLexer.Lexeme>>()

        expectType(lexer.next(), WebIDLLexer.LexemeType.L_ANGLE_BRACKET)
        lexer.next()

        while(lexer.current.type != WebIDLLexer.LexemeType.R_ANGLE_BRACKET) {
            list += parseType()
            if(lexer.current.type == WebIDLLexer.LexemeType.COMMA)
                lexer.next()
        }

        expectType(lexer.current, WebIDLLexer.LexemeType.R_ANGLE_BRACKET)
        expectType(lexer.next(), WebIDLLexer.LexemeType.SEMICOLON)
        return list
    }

    private fun parseFieldOrFunction(
        attributes: List<WebIDLExtendedAttributeDef>,
        modifiers: List<String>,

        allowStatic: Boolean = false,
        allowReadonly: Boolean = false,
        allowAttribute: Boolean = false,
        allowInherit: Boolean = false,
        allowConst: Boolean = false,
        allowOptional: Boolean = false,
        allowRequired: Boolean = false,
        allowVariadic: Boolean = false,
        allowAnonymous: Boolean = false
    ): WebIDLDefinition {
        val modifiers = modifiers.toMutableSet()
        val isStatic = modifiers.remove("static")
        val isReadonly = modifiers.remove("readonly")
        val isAttribute = modifiers.remove("attribute")
        val isInherit = modifiers.remove("inherit")
        val isConst = modifiers.remove("const")
        val isOptional = modifiers.remove("optional")
        val isRequired = modifiers.remove("required")
        if(modifiers.isNotEmpty())
            throw UnsupportedOperationException("Unsupported or duplicate modifiers: $modifiers")

        if(isStatic && !allowStatic)       throw UnsupportedOperationException("'static' is not allowed here")
        if(isReadonly && !allowReadonly)   throw UnsupportedOperationException("'readonly' is not allowed here")
        if(isAttribute && !allowAttribute) throw UnsupportedOperationException("'attribute' is not allowed here")
        if(isInherit && !allowInherit)     throw UnsupportedOperationException("'inherit' is not allowed here")
        if(isConst && !allowConst)         throw UnsupportedOperationException("'const' is not allowed here")
        if(isOptional && !allowOptional)   throw UnsupportedOperationException("'optional' is not allowed here")
        if(isRequired && !allowRequired)   throw UnsupportedOperationException("'required' is not allowed here")

        val type = parseType()

        val isVariadic = if(lexer.current.type == WebIDLLexer.LexemeType.ELLIPSIS) {
            lexer.next()
            true
        } else false
        if(isVariadic && !allowVariadic)   throw UnsupportedOperationException("'...' is not allowed here")

        // Name
        if(lexer.current.type != WebIDLLexer.LexemeType.IDENTIFIER &&
            lexer.current.type != WebIDLLexer.LexemeType.KEYWORD &&
            !allowAnonymous
        ) expectType(lexer.current, WebIDLLexer.LexemeType.IDENTIFIER)

        val name = if(lexer.current.type != WebIDLLexer.LexemeType.L_ROUND_BRACKET) {
            lexer.current.content.also {
                lexer.next()
            }
        } else ""

        // Field or Function
        return if(lexer.current.type == WebIDLLexer.LexemeType.L_ROUND_BRACKET) {
            lexer.next()
            WebIDLFunctionDef(name, type, parseArguments(), isStatic, attributes)
        } else {
            // value
            val value = if(lexer.current.type == WebIDLLexer.LexemeType.EQUALS) {
                when(lexer.next().type) {
                    WebIDLLexer.LexemeType.STRING -> WebIDLFieldDef.StringValue(lexer.current.content)
                    WebIDLLexer.LexemeType.INTEGER -> WebIDLFieldDef.IntValue(lexer.current.content)
                    WebIDLLexer.LexemeType.DECIMAL -> WebIDLFieldDef.DecimalValue(lexer.current.content)
                    WebIDLLexer.LexemeType.TRUE -> WebIDLFieldDef.BooleanValue(true)
                    WebIDLLexer.LexemeType.FALSE -> WebIDLFieldDef.BooleanValue(false)
                    WebIDLLexer.LexemeType.NULL -> WebIDLFieldDef.NullValue
                    WebIDLLexer.LexemeType.L_CURLY_BRACKET -> {
                        expectType(lexer.next(), WebIDLLexer.LexemeType.R_CURLY_BRACKET)
                        WebIDLFieldDef.DictionaryInitValue
                    }
                    else -> throw UnsupportedOperationException("Unsupported field value")
                }.also { lexer.next() }
            } else null

            WebIDLFieldDef(
                name, type, value,
                isAttribute, isStatic, isReadonly,
                isInherit, isOptional, isConst, isVariadic,
                attributes
            )
        }
    }

    private fun parseArguments(): List<WebIDLFieldDef> = buildList {
        while (lexer.current.type != WebIDLLexer.LexemeType.R_ROUND_BRACKET) {
            this@buildList += walkDefinitionsBlock(brackets = false, onlyOne = true) { attributes, modifiers ->
                parseFieldOrFunction(attributes, modifiers,
                    allowOptional = true,
                    allowVariadic = true
                )
            }.map {
                it as? WebIDLFieldDef ?: throw UnsupportedOperationException("Expected field")
            }
            if (lexer.current.type == WebIDLLexer.LexemeType.COMMA)
                lexer.next()
        }
        lexer.next()
    }

    private fun parseExtendedAttributes(): List<WebIDLExtendedAttributeDef> = buildList {
        lexer.next()
        while (lexer.current.type != WebIDLLexer.LexemeType.R_SQUARE_BRACKET) {

            expectType(lexer.current, WebIDLLexer.LexemeType.IDENTIFIER)
            val name = lexer.current.content

            lexer.next()
            this@buildList += when (lexer.current.type) {
                WebIDLLexer.LexemeType.EQUALS -> {
                    lexer.next()
                    val value = lexer.current

                    when (lexer.current.type) {
                        // [Exposed=*]
                        WebIDLLexer.LexemeType.WILDCARD -> {
                            lexer.next()
                            WebIDLExtendedAttributeDefWildcard(name)
                        }

                        // [Reflect="popover"]
                        WebIDLLexer.LexemeType.STRING -> {
                            lexer.next()
                            WebIDLExtendedAttributeDefString(name, value.content)
                        }

                        // [ReflectDefault=2]
                        WebIDLLexer.LexemeType.INTEGER -> {
                            lexer.next()
                            WebIDLExtendedAttributeDefInteger(name, value.content.toInt())
                        }

                        // [ReflectDefault=2.0]
                        WebIDLLexer.LexemeType.DECIMAL -> {
                            lexer.next()
                            WebIDLExtendedAttributeDefDecimal(name, value.content.toDouble())
                        }

                        WebIDLLexer.LexemeType.IDENTIFIER -> {
                            val identifier = lexer.current
                            lexer.next()

                            when (lexer.current.type) {
                                // [PutForwards=name]
                                WebIDLLexer.LexemeType.COMMA, WebIDLLexer.LexemeType.R_SQUARE_BRACKET ->
                                    WebIDLExtendedAttributeDefIdent(name, identifier.content)

                                // [LegacyFactoryFunction=Image(DOMString src)]
                                else -> {
                                    val function = parseFieldOrFunction(emptyList(), emptyList(), allowAnonymous = true)
                                    if(function !is WebIDLFunctionDef || function.name.isNotEmpty())
                                        throw UnsupportedOperationException("Expected anonymous function")

                                    WebIDLExtendedAttributeDefNamedArgList(name, function.type, function.args)
                                }
                            }
                        }

                        WebIDLLexer.LexemeType.L_ROUND_BRACKET -> {
                            lexer.next()

                            val elements = arrayListOf<WebIDLLexer.Lexeme>()
                            while (lexer.current.type != WebIDLLexer.LexemeType.R_ROUND_BRACKET) {
                                elements += lexer.current
                                lexer.next()
                                if (lexer.current.type == WebIDLLexer.LexemeType.COMMA)
                                    lexer.next()
                            }
                            lexer.next()

                            if (elements.isNotEmpty()) {
                                when (elements[0].type) {
                                    // [ReflectRange=(2, 600)]
                                    WebIDLLexer.LexemeType.INTEGER ->
                                        WebIDLExtendedAttributeDefIntegerList(
                                            name,
                                            elements.map { it.content.toInt() }
                                        )

                                    // [Exposed=(Window,Worker)]
                                    WebIDLLexer.LexemeType.IDENTIFIER ->
                                        WebIDLExtendedAttributeDefIdentList(name, elements.map { it.content })

                                    else -> throw UnsupportedOperationException()
                                }
                            } else // // [Exposed=()]
                                WebIDLExtendedAttributeDefIdentList(name, emptyList())
                        }

                        else -> throw UnsupportedOperationException()
                    }
                }

                // [Constructor(double x, double y)]
                WebIDLLexer.LexemeType.L_ROUND_BRACKET -> {
                    lexer.next()
                    WebIDLExtendedAttributeDefArgList(name, parseArguments())
                }
                // [Replaceable]
                WebIDLLexer.LexemeType.COMMA, WebIDLLexer.LexemeType.R_SQUARE_BRACKET -> {
                    WebIDLExtendedAttributeDefNoArgs(name)
                }

                else -> throw UnsupportedOperationException()
            }

            if (lexer.current.type == WebIDLLexer.LexemeType.COMMA)
                lexer.next()
        }
        lexer.next()
    }
}

fun List<WebIDLLexer.Lexeme>.stringifyType(): String {
    if(size == 1)
        return get(0).content
    if(size == 2 && get(1).type == WebIDLLexer.LexemeType.QUESTION)
        return get(0).content + get(1).content

    if(get(0).type == WebIDLLexer.LexemeType.L_ROUND_BRACKET) {
        var brackets = 0
        var lastIndex = 0
        val builder = StringBuilder("(")
        forEachIndexed { index, lexeme ->
            if((lexeme.content == "or" || lexeme.type == WebIDLLexer.LexemeType.R_ROUND_BRACKET) && brackets == 0) {
                builder.append(subList(lastIndex, index).stringifyType())
                if(index != size)
                    builder.append(" or ")
                lastIndex = index
            }
            if(lexeme.type in openBrackets) brackets++
            if(lexeme.type in closeBrackets) brackets--
        }
        return builder.append(')').toString()
    }
    if(get(1).type == WebIDLLexer.LexemeType.L_ANGLE_BRACKET)
        return "${get(0).content}<${subList(2, lastIndex).stringifyType()}>"

    return joinToString(" ") { it.content }
}