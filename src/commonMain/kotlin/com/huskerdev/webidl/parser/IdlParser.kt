package com.huskerdev.webidl.parser

import com.huskerdev.webidl.WebIDLUnexpectedSymbolException
import com.huskerdev.webidl.WebIDLEnv
import com.huskerdev.webidl.WebIDLParserException
import com.huskerdev.webidl.WebIDLWrongSymbolException
import com.huskerdev.webidl.expectType
import com.huskerdev.webidl.lexer.WebIDLLexer



class IdlParser(
    iterator: Iterator<Char>,
    val consumer: IdlParserConsumer,
    types: Set<String> = WebIDLEnv.Default.builtinTypes.keys,
) {
    private val lexer = WebIDLLexer(iterator, types)

    fun parse() {
        walkDefinitionsBlock(
            IdlDefinitionRoot(),
            false
        ) { attributes, modifiers ->
            when(lexer.current.type) {

                // Definitions
                WebIDLLexer.LexemeType.KEYWORD -> when (lexer.current.content) {
                    "interface", -> parseInterface(attributes, modifiers)
                    "dictionary" -> parseDictionary(attributes, modifiers)
                    "namespace" -> parseNamespace(attributes, modifiers)
                    "enum" -> parseEnum(attributes, modifiers)
                    "typedef" -> parseTypeDef(attributes, modifiers)
                    "callback" -> when(lexer.next().type) {
                        WebIDLLexer.LexemeType.IDENTIFIER -> parseCallbackFunction(attributes, modifiers)
                        else -> parseInterface(attributes, modifiers, isCallback = true)
                    }
                    else -> throw WebIDLUnexpectedSymbolException(lexer.current, lexer.current.content)
                }

                // includes/implements
                WebIDLLexer.LexemeType.IDENTIFIER ->
                    parseImplements(modifiers)

                else -> throw WebIDLUnexpectedSymbolException(lexer.current, lexer.current.content)
            }
        }
    }

    private fun walkDefinitionsBlock(
        parent: IdlDefinitionContainer<*>,
        brackets: Boolean = true,
        onDefinition: (attributes: List<IdlExtendedAttribute>, modifiers: Modifiers) -> Unit
    ) {
        if(brackets) {
            expectType(lexer.current, WebIDLLexer.LexemeType.L_CURLY_BRACKET)
            lexer.next()
        }

        consumer.enter(parent)
        while (lexer.hasNext() && lexer.current.type != WebIDLLexer.LexemeType.R_CURLY_BRACKET) {
            val attributes = parseExtendedAttributes()
            val modifiers = parseModifiers()

            onDefinition(attributes, modifiers)

            expectType(lexer.current, WebIDLLexer.LexemeType.SEMICOLON)
            if(lexer.hasNext())
                lexer.next()
        }
        consumer.exit()

        if(lexer.hasNext())
            lexer.next()
    }

    private fun parseNamespace(
        attributes: List<IdlExtendedAttribute>,
        modifiers: Modifiers
    ) {
        modifiers.assertAllowed("partial")
        val isPartial = modifiers.get("partial")

        expectType(lexer.next(), WebIDLLexer.LexemeType.IDENTIFIER)
        val name = lexer.current.content
        lexer.next()

        walkDefinitionsBlock(
            IdlNamespace(name, isPartial, attributes)
        ) { attributes, modifiers ->
            consumer.consume(parseFieldOrOperation(
                attributes, modifiers,
                allowReadonly = true,
                allowAttribute = true
            ))
        }
    }

    private fun parseCallbackFunction(
        attributes: List<IdlExtendedAttribute>,
        modifiers: Modifiers
    ) {
        modifiers.assertAllowed()
        expectType(lexer.current, WebIDLLexer.LexemeType.IDENTIFIER)
        val name = lexer.current.content

        val equals = lexer.next()
        expectType(equals, WebIDLLexer.LexemeType.EQUALS)
        lexer.next()

        val operation = parseFieldOrOperation(attributes, Modifiers.EMPTY, allowAnonymous = true)

        if(operation !is IdlOperation || operation.name.isNotEmpty())
            throw WebIDLWrongSymbolException(equals, "Expected anonymous operation")

        consumer.consume(IdlCallbackFunction(name, operation, attributes))
    }

    private fun parseTypeDef(
        attributes: List<IdlExtendedAttribute>,
        modifiers: Modifiers
    ) {
        modifiers.assertAllowed()
        lexer.next()
        val type = parseType()

        expectType(lexer.current, WebIDLLexer.LexemeType.IDENTIFIER)
        val name = lexer.current.content
        lexer.next()

        consumer.consume(IdlTypeDef(name, type, attributes))
    }

    private fun parseEnum(
        attributes: List<IdlExtendedAttribute>,
        modifiers: Modifiers
    ) {
        modifiers.assertAllowed()
        expectType(lexer.next(), WebIDLLexer.LexemeType.IDENTIFIER)
        val name = lexer.current.content

        expectType(lexer.next(), WebIDLLexer.LexemeType.L_CURLY_BRACKET)
        lexer.next()

        consumer.enter(IdlEnum(name, attributes))
        while(lexer.current.type != WebIDLLexer.LexemeType.R_CURLY_BRACKET) {
            expectType(lexer.current, WebIDLLexer.LexemeType.STRING)

            consumer.consume(IdlEnumElement(lexer.current.content))

            if(lexer.next().type == WebIDLLexer.LexemeType.COMMA)
                lexer.next()
        }
        consumer.exit()
        lexer.next()
    }

    private fun parseDictionary(
        attributes: List<IdlExtendedAttribute>,
        modifiers: Modifiers
    ) {
        modifiers.assertAllowed("partial")
        val isPartial = modifiers.get("partial")

        expectType(lexer.next(), WebIDLLexer.LexemeType.IDENTIFIER)
        val name = lexer.current.content

        val implements: String? = if(lexer.next().type == WebIDLLexer.LexemeType.COLON) {
            expectType(lexer.next(), WebIDLLexer.LexemeType.IDENTIFIER)
            lexer.current.content.also {
                lexer.next()
            }
        } else null

        walkDefinitionsBlock(
            IdlDictionary(name, implements, isPartial, attributes)
        ) { attributes, modifiers ->
            if(attributes.isNotEmpty())
                throw WebIDLParserException(lexer.current, "Dictionary members can not have attributes")

            consumer.consume(parseFieldOrOperation(
                attributes, modifiers,
                allowOptional = true,
                allowRequired = true
            ))
        }
    }

    private fun parseInterface(
        attributes: List<IdlExtendedAttribute>,
        modifiers: Modifiers,
        isCallback: Boolean = false,
    ) {
        modifiers.assertAllowed("partial")
        val isPartial = modifiers.get("partial")

        val isMixin = if(lexer.next().content == "mixin"){
            if(isCallback)
                throw WebIDLParserException(lexer.current, "Can not use mixin with callback interface")
            lexer.next()
            true
        } else false

        expectType(lexer.current, WebIDLLexer.LexemeType.IDENTIFIER)
        val name = lexer.current.content

        val implements: String? = if(lexer.next().type == WebIDLLexer.LexemeType.COLON) {
            expectType(lexer.next(), WebIDLLexer.LexemeType.IDENTIFIER)
            lexer.current.content.also {
                lexer.next()
            }
        } else null

        walkDefinitionsBlock(
            IdlInterface(
                name,
                isPartial, isMixin, isCallback,
                implements, attributes
            )
        ) { attributes, modifiers ->
            consumer.consume(when(lexer.current.content) {
                "iterable" -> parseGeneric().run {
                    modifiers.assertAllowed()
                    IdlIterable(get(0), getOrNull(1))
                }
                "async_iterable" -> parseGeneric().run {
                    modifiers.assertAllowed()
                    IdlAsyncIterableLike(get(0), getOrNull(1))
                }
                "maplike" -> parseGeneric().run {
                    modifiers.assertAllowed("readonly")
                    IdlMapLike(this[0], this[1], modifiers.get("readonly"))
                }
                "setlike" -> parseGeneric().run {
                    modifiers.assertAllowed("readonly")
                    IdlSetLike(this[0], modifiers.get("readonly"))
                }
                "stringifier" -> {
                    modifiers.assertAllowed()
                    val firstLexeme = lexer.next()

                    val field = if(firstLexeme.type != WebIDLLexer.LexemeType.SEMICOLON) {
                        parseFieldOrOperation(
                            emptyList(), parseModifiers(),
                            allowInherit = true,
                            allowAttribute = true,
                            allowReadonly = true
                        )
                    } else null

                    if(field != null && field !is IdlField)
                        throw WebIDLParserException(firstLexeme, "Expected field")

                    IdlStringifier(field)
                }
                "getter" -> {
                    modifiers.assertAllowed()
                    val firstLexeme = lexer.next()

                    val operation = parseFieldOrOperation(
                        emptyList(), parseModifiers(),
                        allowAnonymous = true
                    )
                    if(operation !is IdlOperation)
                        throw WebIDLParserException(firstLexeme, "Expected operation")

                    IdlGetter(operation)
                }
                "setter" -> {
                    modifiers.assertAllowed()
                    val firstLexeme = lexer.next()

                    val operation = parseFieldOrOperation(
                        emptyList(), parseModifiers(),
                        allowAnonymous = true
                    )
                    if(operation !is IdlOperation)
                        throw WebIDLParserException(firstLexeme, "Expected operation")

                    IdlSetter(operation)
                }
                "constructor" -> {
                    modifiers.assertAllowed()
                    expectType(lexer.next(), WebIDLLexer.LexemeType.L_ROUND_BRACKET)
                    lexer.next()
                    IdlConstructor(parseArguments(), attributes)
                }
                else -> {
                    parseFieldOrOperation(
                        attributes, modifiers,
                        allowConst = true,
                        allowStatic = true,
                        allowInherit = true,
                        allowAttribute = true,
                        allowReadonly = true
                    )
                }
            })
        }
    }

    private fun parseImplements(modifiers: Modifiers){
        modifiers.assertAllowed()
        val identifier1 = lexer.current

        val action = lexer.next()
        if(action.content != "includes" && action.content != "implements")
            throw WebIDLWrongSymbolException(action, "includes' or 'implements")

        val identifier2 = lexer.next()
        expectType(identifier2, WebIDLLexer.LexemeType.IDENTIFIER)
        lexer.next()

        consumer.consume(when (action.content) {
            "includes" -> IdlIncludes(identifier1.content, identifier2.content)
            "implements" -> IdlImplements(identifier1.content, identifier2.content)
            else -> throw UnsupportedOperationException()
        })
    }

    private fun parseFieldOrOperation(
        attributes: List<IdlExtendedAttribute>,
        modifiers: Modifiers,

        allowStatic: Boolean = false,
        allowReadonly: Boolean = false,
        allowAttribute: Boolean = false,
        allowInherit: Boolean = false,
        allowConst: Boolean = false,
        allowOptional: Boolean = false,
        allowRequired: Boolean = false,
        allowVariadic: Boolean = false,
        allowAnonymous: Boolean = false
    ): IdlDefinition {
        modifiers.assertAllowed("static", "readonly", "attribute", "inherit", "const", "optional", "required")
        val isStatic = modifiers.get("static", allowStatic)
        val isReadonly = modifiers.get("readonly", allowReadonly)
        val isAttribute = modifiers.get("attribute", allowAttribute)
        val isInherit = modifiers.get("inherit", allowInherit)
        val isConst = modifiers.get("const", allowConst)
        val isOptional = modifiers.get("optional", allowOptional)
        val isRequired = modifiers.get("required", allowRequired)

        val type = parseType()

        val variadicLexeme = lexer.current
        val isVariadic = if(variadicLexeme.type == WebIDLLexer.LexemeType.ELLIPSIS) {
            lexer.next()
            true
        } else false
        if(isVariadic && !allowVariadic)   throw WebIDLParserException(variadicLexeme, "Variadic is not allowed here")

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

        // Field or Operation
        return if(lexer.current.type == WebIDLLexer.LexemeType.L_ROUND_BRACKET) {
            lexer.next()
            IdlOperation(name, type, parseArguments(), isStatic, attributes)
        } else {
            // value
            val value = if(lexer.current.type == WebIDLLexer.LexemeType.EQUALS) {
                when(lexer.next().type) {
                    WebIDLLexer.LexemeType.STRING -> IdlValue.StringValue(lexer.current.content)
                    WebIDLLexer.LexemeType.INTEGER -> IdlValue.IntValue(lexer.current.content)
                    WebIDLLexer.LexemeType.DECIMAL -> IdlValue.DecimalValue(lexer.current.content)
                    WebIDLLexer.LexemeType.TRUE -> IdlValue.BooleanValue(true)
                    WebIDLLexer.LexemeType.FALSE -> IdlValue.BooleanValue(false)
                    WebIDLLexer.LexemeType.NULL -> IdlValue.NullValue
                    WebIDLLexer.LexemeType.L_CURLY_BRACKET -> {
                        expectType(lexer.next(), WebIDLLexer.LexemeType.R_CURLY_BRACKET)
                        IdlValue.DictionaryInitValue
                    }
                    else -> throw WebIDLParserException(lexer.current, "Unsupported field value")
                }.also { lexer.next() }
            } else null

            IdlField(
                name, type, value,
                isAttribute, isStatic, isReadonly, isInherit,
                isOptional, isConst, isVariadic, isRequired,
                attributes
            )
        }
    }

    private fun parseArguments(): List<IdlField> = buildList {
        val firstLexeme = lexer.current
        while (lexer.current.type != WebIDLLexer.LexemeType.R_ROUND_BRACKET) {
            val attributes = parseExtendedAttributes()
            val modifiers = parseModifiers()
            val field = parseFieldOrOperation(attributes, modifiers,
                allowOptional = true,
                allowVariadic = true
            ) as? IdlField
                ?: throw WebIDLParserException(firstLexeme, "Expected field")
            add(field)

            if (lexer.current.type == WebIDLLexer.LexemeType.COMMA)
                lexer.next()
        }
        lexer.next()
    }

    private fun parseModifiers() = Modifiers(buildList {
        while(lexer.current.content in modifiers) {
            add(lexer.current)
            lexer.next()
        }
    })

    private fun parseType(): IdlType {
        fun isNullable() =
            lexer.current.type == WebIDLLexer.LexemeType.QUESTION

        val result = when (lexer.current.type) {

            // identifier
            WebIDLLexer.LexemeType.IDENTIFIER -> {
                val name = lexer.current.content
                lexer.next()
                IdlType.Default(name, isNullable())
            }

            // union type
            WebIDLLexer.LexemeType.L_ROUND_BRACKET -> {
                lexer.next()
                val types = buildList {
                    while (lexer.current.type != WebIDLLexer.LexemeType.R_ROUND_BRACKET) {
                        add(parseType())
                        if (lexer.current.content == "or")
                            lexer.next()
                    }
                }
                lexer.next()
                IdlType.Union(types, isNullable())
            }

            // builtin-types
            WebIDLLexer.LexemeType.TYPE -> {
                val firstLexeme = lexer.current
                when (lexer.next().type) {
                    // generics (sequence<T>, record<K, V>)
                    WebIDLLexer.LexemeType.L_ANGLE_BRACKET -> {
                        lexer.next()
                        val types = buildList {
                            while (lexer.current.type != WebIDLLexer.LexemeType.R_ANGLE_BRACKET) {
                                add(parseType())
                                if (lexer.current.type == WebIDLLexer.LexemeType.COMMA)
                                    lexer.next()
                            }
                        }
                        lexer.next()
                        IdlType.Default(firstLexeme.content, isNullable(),  types)
                    }

                    // long types
                    WebIDLLexer.LexemeType.TYPE -> {
                        val type = buildList {
                            add(firstLexeme.content)
                            add(lexer.current.content)
                            while(lexer.next().type == WebIDLLexer.LexemeType.TYPE)
                                add(lexer.current.content)
                        }.joinToString(" ")
                        IdlType.Default(type, isNullable())
                    }

                    // simple types
                    else -> IdlType.Default(firstLexeme.content, isNullable())
                }
            }
            else -> throw WebIDLWrongSymbolException(lexer.current, WebIDLLexer.LexemeType.TYPE.word)
        }
        if(isNullable())
            lexer.next()
        return result
    }

    private fun parseGeneric(): List<IdlType> {
        val list = arrayListOf<IdlType>()

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

    private fun parseExtendedAttributes(): List<IdlExtendedAttribute> = buildList {
        if (lexer.current.type != WebIDLLexer.LexemeType.L_SQUARE_BRACKET)
            return@buildList

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
                            IdlExtendedAttribute.Wildcard(name)
                        }

                        // [Reflect="popover"]
                        WebIDLLexer.LexemeType.STRING -> {
                            lexer.next()
                            IdlExtendedAttribute.StringValue(name, value.content)
                        }

                        // [ReflectDefault=2]
                        WebIDLLexer.LexemeType.INTEGER -> {
                            lexer.next()
                            IdlExtendedAttribute.IntegerValue(name, value.content.toInt())
                        }

                        // [ReflectDefault=2.0]
                        WebIDLLexer.LexemeType.DECIMAL -> {
                            lexer.next()
                            IdlExtendedAttribute.DecimalValue(name, value.content.toDouble())
                        }

                        WebIDLLexer.LexemeType.IDENTIFIER -> {
                            val identifier = lexer.current
                            lexer.next()

                            when (lexer.current.type) {
                                // [PutForwards=name]
                                WebIDLLexer.LexemeType.COMMA, WebIDLLexer.LexemeType.R_SQUARE_BRACKET ->
                                    IdlExtendedAttribute.IdentifierValue(name, identifier.content)

                                // [LegacyFactoryFunction=Image(DOMString src)]
                                WebIDLLexer.LexemeType.L_ROUND_BRACKET ->
                                    IdlExtendedAttribute.NamedArgList(name, identifier.content, parseArguments())

                                else -> throw WebIDLParserException(identifier, "Expected function or identifier")
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
                                        IdlExtendedAttribute.IntegerList(name, elements.map { it.content.toInt() })

                                    // [Exposed=(Window,Worker)]
                                    WebIDLLexer.LexemeType.IDENTIFIER ->
                                        IdlExtendedAttribute.IdentifierList(name, elements.map { it.content })

                                    else -> throw WebIDLParserException(elements[0], "Unsupported array type")
                                }
                            } else // // [Exposed=()]
                                IdlExtendedAttribute.IdentifierList(name, emptyList())
                        }
                        else -> throw WebIDLParserException(lexer.current, "Unsupported attribute value")
                    }
                }

                // [Constructor(double x, double y)]
                WebIDLLexer.LexemeType.L_ROUND_BRACKET -> {
                    lexer.next()
                    IdlExtendedAttribute.ArgList(name, parseArguments())
                }
                // [Replaceable]
                WebIDLLexer.LexemeType.COMMA, WebIDLLexer.LexemeType.R_SQUARE_BRACKET ->
                    IdlExtendedAttribute.NoArgs(name)

                else -> throw WebIDLParserException(lexer.current, "Unsupported attribute type")
            }
            if (lexer.current.type == WebIDLLexer.LexemeType.COMMA)
                lexer.next()
        }
        lexer.next()
    }
}