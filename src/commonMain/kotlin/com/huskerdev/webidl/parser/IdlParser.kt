package com.huskerdev.webidl.parser

import com.huskerdev.webidl.IdlElementBounds
import com.huskerdev.webidl.WebIDLUnexpectedSymbolException
import com.huskerdev.webidl.WebIDLEnv
import com.huskerdev.webidl.WebIDLSyntaxErrorException
import com.huskerdev.webidl.WebIDLWrongSymbolException
import com.huskerdev.webidl.endAt
import com.huskerdev.webidl.expectType
import com.huskerdev.webidl.lexer.WebIDLLexer
import com.huskerdev.webidl.locateAt


class IdlParser(
    iterator: Iterator<Char>,
    val consumer: IdlParserConsumer,
    types: Set<String> = WebIDLEnv.Default.builtinTypes.keys,
) {
    private val lexer = WebIDLLexer(iterator, types)

    fun parse() {
        try {
            walkDefinitionsBlock(
                IdlRoot(),
                false
            ) { attributes, modifiers ->
                when (lexer.current.type) {

                    // Definitions
                    WebIDLLexer.LexemeType.KEYWORD -> when (lexer.current.content) {
                        "interface", -> parseInterface(attributes, modifiers)
                        "dictionary" -> parseDictionary(attributes, modifiers)
                        "namespace" -> parseNamespace(attributes, modifiers)
                        "enum" -> parseEnum(attributes, modifiers)
                        "typedef" -> parseTypeDef(attributes, modifiers)
                        "callback" -> when (lexer.next().type) {
                            WebIDLLexer.LexemeType.IDENTIFIER -> parseCallbackFunction(attributes, modifiers)
                            else -> parseInterface(attributes, modifiers, isCallback = true)
                        }
                        else -> throw WebIDLUnexpectedSymbolException(lexer.current, lexer.current.content)
                    }

                    // includes/implements
                    WebIDLLexer.LexemeType.IDENTIFIER ->
                        parseImplements(modifiers)

                    else -> {
                        val found = lexer.current
                        lexer.next()
                        throw WebIDLUnexpectedSymbolException(found, found.content)
                    }
                }
            }
        } catch (e: WebIDLSyntaxErrorException) {
            consumer.error(e)
        }

        // Pass lexer errors
        lexer.errors.forEach { consumer.error(it) }
    }

    private fun walkDefinitionsBlock(
        parent: IdlContainer,
        brackets: Boolean = true,
        onDefinition: (attributes: IdlAttributes?, modifiers: Modifiers) -> IdlDefinition
    ): IdlDefinition {
        if(brackets) {
            if(lexer.current.type != WebIDLLexer.LexemeType.L_CURLY_BRACKET) {
                val found = lexer.current
                lexer.next()
                throw WebIDLWrongSymbolException(found, WebIDLLexer.LexemeType.L_CURLY_BRACKET.word)
            }
            lexer.next()
        }

        consumer.enter(parent)
        try {
            while (!brackets || lexer.current.type != WebIDLLexer.LexemeType.R_CURLY_BRACKET) {
                if(!lexer.hasNext())
                    break

                val firstLexeme = lexer.current

                val attributes = parseExtendedAttributes()
                val modifiers = parseModifiers()

                try {
                    val definition = onDefinition(attributes, modifiers)
                    definition.locateAt(firstLexeme)
                    definition.endAt(lexer.current.bounds.endOffset)

                    expectType(lexer.current, WebIDLLexer.LexemeType.SEMICOLON)
                    if(lexer.hasNext())
                        lexer.next()
                } catch (e: WebIDLSyntaxErrorException) {
                    consumer.error(e)

                    while(lexer.hasNext()) {
                        if(lexer.current.type == WebIDLLexer.LexemeType.SEMICOLON) {
                            lexer.next()
                            break
                        }
                        if(lexer.current.type == WebIDLLexer.LexemeType.R_CURLY_BRACKET ||
                            lexer.current.type == WebIDLLexer.LexemeType.TYPE ||
                            lexer.current.type == WebIDLLexer.LexemeType.IDENTIFIER
                        ) break
                        lexer.next()
                    }
                }
            }
            parent.endAt(lexer.current.bounds.endOffset)
        } finally {
            consumer.exit()
        }

        if(lexer.hasNext())
            lexer.next()
        return parent
    }

    private fun parseNamespace(
        attributes: IdlAttributes?,
        modifiers: Modifiers
    ): IdlDefinition {
        modifiers.assertAllowed("partial")
        val isPartial = modifiers.get("partial")

        expectType(lexer.next(), WebIDLLexer.LexemeType.IDENTIFIER)
        val nameLexeme = lexer.current
        lexer.next()

        return walkDefinitionsBlock(
            IdlNamespace(
                name = IdlName(nameLexeme, isReference = false),
                isPartial = isPartial,
                attributes = attributes
            )
        ) { attributes, modifiers ->
            parseFieldOrOperation(
                attributes = attributes,
                modifiers = modifiers,
                isOperationArgument = false,
                allowReadonly = true,
                allowAttribute = true
            ).also(consumer::consume)
        }
    }

    private fun parseCallbackFunction(
        attributes: IdlAttributes?,
        modifiers: Modifiers
    ): IdlDefinition {
        modifiers.assertAllowed()
        expectType(lexer.current, WebIDLLexer.LexemeType.IDENTIFIER)
        val nameLexeme = lexer.current

        val equals = lexer.next()
        expectType(equals, WebIDLLexer.LexemeType.EQUALS)
        val firstLexeme = lexer.next()

        val operation = parseFieldOrOperation(
            attributes = attributes,
            modifiers = Modifiers.EMPTY,
            isOperationArgument = false,
            allowAnonymous = true
        )

        operation.locateAt(firstLexeme)

        if(operation !is IdlOperation || operation.name.text.isNotEmpty())
            throw WebIDLWrongSymbolException(equals, "Expected anonymous operation")

        return IdlCallbackFunction(
            name = IdlName(nameLexeme, isReference = false),
            operation = operation,
            attributes = attributes
        ).also(consumer::consume)
    }

    private fun parseTypeDef(
        attributes: IdlAttributes?,
        modifiers: Modifiers
    ): IdlDefinition {
        modifiers.assertAllowed()
        lexer.next()
        val type = parseType()

        expectType(lexer.current, WebIDLLexer.LexemeType.IDENTIFIER)
        val nameLexeme = lexer.current
        lexer.next()

        return IdlTypeDef(
            name = IdlName(nameLexeme, isReference = false),
            type = type,
            attributes = attributes
        ).also(consumer::consume)
    }

    private fun parseEnum(
        attributes: IdlAttributes?,
        modifiers: Modifiers
    ): IdlDefinition {
        modifiers.assertAllowed()
        expectType(lexer.next(), WebIDLLexer.LexemeType.IDENTIFIER)
        val nameLexeme = lexer.current

        val brace = lexer.next()
        if(brace.type != WebIDLLexer.LexemeType.L_CURLY_BRACKET) {
            lexer.next()
            throw WebIDLWrongSymbolException(brace, WebIDLLexer.LexemeType.L_CURLY_BRACKET.word)
        }
        lexer.next()

        val enum = IdlEnum(
            name = IdlName(nameLexeme, isReference = false),
            attributes = attributes
        )
        consumer.enter(enum)
        try {
            while (lexer.hasNext() && lexer.current.type != WebIDLLexer.LexemeType.R_CURLY_BRACKET) {
                try {
                    expectType(lexer.current, WebIDLLexer.LexemeType.STRING)

                    consumer.consume(
                        IdlEnumElement(
                            name = IdlName(lexer.current, isReference = false),
                            bounds = lexer.current.bounds
                        )
                    )
                    lexer.next()
                } catch (e: WebIDLSyntaxErrorException) {
                    consumer.error(e)

                    while (lexer.hasNext() &&
                        lexer.current.type != WebIDLLexer.LexemeType.COMMA &&
                        lexer.current.type != WebIDLLexer.LexemeType.R_CURLY_BRACKET &&
                        lexer.current.type != WebIDLLexer.LexemeType.STRING
                    ) lexer.next()
                }

                if (lexer.current.type == WebIDLLexer.LexemeType.COMMA)
                    lexer.next()
                else
                    expectType(lexer.current, WebIDLLexer.LexemeType.R_CURLY_BRACKET)
            }
        } catch (e: WebIDLSyntaxErrorException) {
            consumer.error(e)
        }
        consumer.exit()
        lexer.next()
        return enum
    }

    private fun parseDictionary(
        attributes: IdlAttributes?,
        modifiers: Modifiers
    ): IdlDefinition {
        modifiers.assertAllowed("partial")
        val isPartial = modifiers.get("partial")

        expectType(lexer.next(), WebIDLLexer.LexemeType.IDENTIFIER)
        val nameLexeme = lexer.current

        var implements: IdlName? = null
        if(lexer.next().type == WebIDLLexer.LexemeType.COLON) {
            expectType(lexer.next(), WebIDLLexer.LexemeType.IDENTIFIER)

            implements = IdlName(lexer.current)
            lexer.next()
        }

        return walkDefinitionsBlock(
            IdlDictionary(
                name = IdlName(nameLexeme, isReference = false),
                implements = implements,
                isPartial = isPartial,
                attributes = attributes
            )
        ) { attributes, modifiers ->
            if(attributes != null)
                throw WebIDLSyntaxErrorException(lexer.current, "Dictionary members can not have attributes.")

            parseFieldOrOperation(
                attributes = attributes,
                modifiers = modifiers,
                isOperationArgument = false,
                allowOptional = true,
                allowRequired = true
            ).also(consumer::consume)
        }
    }

    private fun parseInterface(
        attributes: IdlAttributes?,
        modifiers: Modifiers,
        isCallback: Boolean = false,
    ): IdlDefinition {
        modifiers.assertAllowed("partial")
        val isPartial = modifiers.get("partial")

        val isMixin = if(lexer.next().content == "mixin"){
            if(isCallback)
                throw WebIDLSyntaxErrorException(lexer.current, "Can not use mixin with callback interface.")
            lexer.next()
            true
        } else false

        expectType(lexer.current, WebIDLLexer.LexemeType.IDENTIFIER)
        val nameLexeme = lexer.current

        var implements: IdlName? = null
        if(lexer.next().type == WebIDLLexer.LexemeType.COLON) {
            expectType(lexer.next(), WebIDLLexer.LexemeType.IDENTIFIER)

            implements = IdlName(lexer.current)
            lexer.next()
        }

        return walkDefinitionsBlock(
            IdlInterface(
                name = IdlName(nameLexeme, isReference = false),
                isPartial = isPartial,
                isMixin = isMixin,
                isCallback = isCallback,
                implements = implements,
                attributes = attributes
            )
        ) { attributes, modifiers ->
            when(lexer.current.content) {
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
                            attributes = null,
                            modifiers = parseModifiers(),
                            isOperationArgument = false,
                            allowInherit = true,
                            allowAttribute = true,
                            allowReadonly = true
                        )
                    } else null

                    if(field != null && field !is IdlField)
                        throw WebIDLSyntaxErrorException(firstLexeme, "Expected field.")

                    field?.locateAt(firstLexeme)

                    IdlStringifier(field)
                }
                "getter" -> {
                    modifiers.assertAllowed()
                    val firstLexeme = lexer.next()

                    val operation = parseFieldOrOperation(
                        attributes = null,
                        modifiers = parseModifiers(),
                        isOperationArgument = false,
                        allowAnonymous = true
                    )
                    if(operation !is IdlOperation)
                        throw WebIDLSyntaxErrorException(firstLexeme, "Expected operation.")

                    operation.locateAt(firstLexeme)

                    IdlGetter(operation)
                }
                "setter" -> {
                    modifiers.assertAllowed()
                    val firstLexeme = lexer.next()

                    val operation = parseFieldOrOperation(
                        attributes = null,
                        modifiers = parseModifiers(),
                        isOperationArgument = false,
                        allowAnonymous = true
                    )
                    if(operation !is IdlOperation)
                        throw WebIDLSyntaxErrorException(firstLexeme, "Expected operation.")

                    operation.locateAt(firstLexeme)

                    IdlSetter(operation)
                }
                "constructor" -> {
                    modifiers.assertAllowed()
                    val firstLexeme = lexer.current
                    expectType(lexer.next(), WebIDLLexer.LexemeType.L_ROUND_BRACKET)
                    lexer.next()
                    IdlConstructor(
                        args = parseArguments(),
                        header = IdlName(firstLexeme, isReference = false),
                        attributes = attributes
                    )
                }
                else -> {
                    parseFieldOrOperation(
                        attributes = attributes,
                        modifiers = modifiers,
                        isOperationArgument = false,
                        allowConst = true,
                        allowStatic = true,
                        allowInherit = true,
                        allowAttribute = true,
                        allowReadonly = true
                    )
                }
            }.also(consumer::consume)
        }
    }

    private fun parseImplements(modifiers: Modifiers): IdlDefinition {
        modifiers.assertAllowed()
        val identifier1 = lexer.current

        val action = lexer.next()
        if(action.content != "includes" && action.content != "implements")
            throw WebIDLWrongSymbolException(action, "includes' or 'implements")

        val identifier2 = lexer.next()
        expectType(identifier2, WebIDLLexer.LexemeType.IDENTIFIER)
        lexer.next()

        return when (action.content) {
            "includes" -> IdlIncludes(
                target = IdlName(identifier1),
                source = IdlName(identifier2)
            )
            "implements" -> IdlImplements(
                target = IdlName(identifier1),
                source = IdlName(identifier2)
            )
            else -> throw UnsupportedOperationException()
        }.also(consumer::consume)
    }

    private fun parseFieldOrOperation(
        attributes: IdlAttributes?,
        modifiers: Modifiers,
        isOperationArgument: Boolean,

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
        if(isVariadic && !allowVariadic) throw WebIDLSyntaxErrorException(variadicLexeme, "Variadic is not allowed here.")

        // Name
        if(lexer.current.type != WebIDLLexer.LexemeType.IDENTIFIER &&
            lexer.current.type != WebIDLLexer.LexemeType.KEYWORD &&
            !allowAnonymous
        ) expectType(lexer.current, WebIDLLexer.LexemeType.IDENTIFIER)

        var nameText = ""
        var nameBounds = IdlElementBounds()
        if(lexer.current.type != WebIDLLexer.LexemeType.L_ROUND_BRACKET) {
            nameText = lexer.current.content
            nameBounds = lexer.current.bounds
            lexer.next()
        }

        // Field or Operation
        return if(lexer.current.type == WebIDLLexer.LexemeType.L_ROUND_BRACKET) {
            lexer.next()
            val args = parseArguments()
            IdlOperation(
                name = IdlName(
                    text = nameText,
                    isReference = false,
                    isBuiltin = false,
                    bounds = nameBounds
                ),
                type = type,
                args = args,
                isStatic = isStatic,
                attributes = attributes
            ).apply {
                endAt(lexer.current.bounds.startOffset)
            }
        } else {
            // value
            val (value, endOffset) = if(lexer.current.type == WebIDLLexer.LexemeType.EQUALS) {
                when(lexer.next().type) {
                    WebIDLLexer.LexemeType.STRING -> IdlValue.StringValue(lexer.current.content, lexer.current.bounds)
                    WebIDLLexer.LexemeType.INTEGER -> IdlValue.IntValue(lexer.current.content, lexer.current.bounds)
                    WebIDLLexer.LexemeType.DECIMAL -> IdlValue.DecimalValue(lexer.current.content, lexer.current.bounds)
                    WebIDLLexer.LexemeType.TRUE -> IdlValue.BooleanValue(true, lexer.current.bounds)
                    WebIDLLexer.LexemeType.FALSE -> IdlValue.BooleanValue(false, lexer.current.bounds)
                    WebIDLLexer.LexemeType.NULL -> IdlValue.NullValue
                    WebIDLLexer.LexemeType.L_CURLY_BRACKET -> {
                        expectType(lexer.next(), WebIDLLexer.LexemeType.R_CURLY_BRACKET)
                        IdlValue.DictionaryInitValue
                    }
                    else -> throw WebIDLSyntaxErrorException(lexer.current, "Unsupported field value.")
                }.run { this to lexer.current.bounds.endOffset }.also { lexer.next() }
            } else null to lexer.current.bounds.startOffset

            IdlField(
                name = IdlName(
                    text = nameText,
                    isReference = false,
                    isBuiltin = false,
                    bounds = nameBounds
                ),
                type = type,
                value = value,
                isOperationArgument = isOperationArgument,
                isAttribute = isAttribute,
                isStatic = isStatic,
                isReadOnly = isReadonly,
                isInherit = isInherit,
                isOptional = isOptional,
                isConst = isConst,
                isVariadic = isVariadic,
                isRequired = isRequired,
                attributes = attributes
            ).apply {
                endAt(endOffset)
            }
        }
    }

    private fun parseArguments(): List<IdlField> = buildList {
        while (lexer.hasNext() && lexer.current.type != WebIDLLexer.LexemeType.R_ROUND_BRACKET) {
            try {
                val firstLexeme = lexer.current
                val attributes = parseExtendedAttributes()
                val modifiers = parseModifiers()
                val field = parseFieldOrOperation(
                    attributes = attributes,
                    modifiers = modifiers,
                    isOperationArgument = true,
                    allowOptional = true,
                    allowVariadic = true
                ) as? IdlField
                    ?: throw WebIDLSyntaxErrorException(firstLexeme, "Expected field.")
                add(field)

                field.locateAt(firstLexeme)
            } catch (e: WebIDLSyntaxErrorException) {
                consumer.error(e)
                while(lexer.hasNext() &&
                    lexer.current.type != WebIDLLexer.LexemeType.R_ROUND_BRACKET &&
                    lexer.current.type != WebIDLLexer.LexemeType.COMMA
                ) lexer.next()
            }

            if (lexer.current.type == WebIDLLexer.LexemeType.COMMA)
                lexer.next()
        }
        lexer.next()
    }

    private fun parseModifiers() = Modifiers(buildList {
        while(lexer.hasNext() && lexer.current.content in modifiers) {
            add(lexer.current)
            lexer.next()
        }
    })

    private fun parseType(): IdlType {
        fun readNullable(prevLexeme: WebIDLLexer.Lexeme) =
            if(lexer.current.type == WebIDLLexer.LexemeType.QUESTION)
                true to lexer.current.also { lexer.next() }
            else false to prevLexeme

        val result = when (lexer.current.type) {

            // identifier
            WebIDLLexer.LexemeType.IDENTIFIER -> {
                val nameLexeme = lexer.current
                lexer.next()

                val (isNullable, lastLexeme) = readNullable(nameLexeme)
                IdlType.Default(
                    name = IdlName(nameLexeme),
                    bounds = IdlElementBounds(nameLexeme, lastLexeme),
                    isNullable = isNullable
                )
            }

            // union type
            WebIDLLexer.LexemeType.L_ROUND_BRACKET -> {
                val fromLexeme = lexer.current
                lexer.next()
                val types = buildList {
                    while (lexer.current.type != WebIDLLexer.LexemeType.R_ROUND_BRACKET) {
                        add(parseType())
                        if (lexer.current.content == "or")
                            lexer.next()
                    }
                }
                val lastValidLexeme = lexer.current
                lexer.next()

                val (isNullable, lastLexeme) = readNullable(lastValidLexeme)
                IdlType.Union(
                    types = types,
                    isNullable = isNullable,
                    bounds = IdlElementBounds(fromLexeme, lastLexeme)
                )
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
                        val lastValidLexeme = lexer.current
                        lexer.next()

                        val (isNullable, lastLexeme) = readNullable(lastValidLexeme)
                        IdlType.Default(
                            name = IdlName(firstLexeme),
                            bounds = IdlElementBounds(firstLexeme, lastLexeme),
                            isNullable = isNullable,
                            parameters = types
                        )
                    }

                    // long types
                    WebIDLLexer.LexemeType.TYPE -> {
                        val collected = buildList {
                            add(firstLexeme)
                            add(lexer.current)
                            while(lexer.next().type == WebIDLLexer.LexemeType.TYPE)
                                add(lexer.current)
                        }
                        val name = IdlName(
                            text = collected.joinToString(" ") { it.content },
                            bounds = IdlElementBounds(collected.first(), collected.last())
                        )

                        val (isNullable, lastLexeme) = readNullable(collected.last())
                        IdlType.Default(
                            name = name,
                            bounds = IdlElementBounds(firstLexeme, lastLexeme),
                            isNullable = isNullable
                        )
                    }

                    // simple types
                    else -> {
                        val (isNullable, lastLexeme) = readNullable(firstLexeme)
                        IdlType.Default(
                            name = IdlName(firstLexeme),
                            bounds = IdlElementBounds(firstLexeme, lastLexeme),
                            isNullable = isNullable
                        )
                    }
                }
            }
            else -> throw WebIDLWrongSymbolException(lexer.current, WebIDLLexer.LexemeType.TYPE.word)
        }
        if(lexer.current.type == WebIDLLexer.LexemeType.L_ANGLE_BRACKET)
            throw WebIDLUnexpectedSymbolException(lexer.current, "<")
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

    private fun parseExtendedAttributes(): IdlAttributes? {
        if (lexer.current.type != WebIDLLexer.LexemeType.L_SQUARE_BRACKET)
            return null

        val firstLexeme = lexer.current
        lexer.next()

        val children = buildList {
            while (lexer.current.type != WebIDLLexer.LexemeType.R_SQUARE_BRACKET) {

                expectType(lexer.current, WebIDLLexer.LexemeType.IDENTIFIER)
                val nameLexeme = lexer.current

                lexer.next()
                this@buildList += when (lexer.current.type) {
                    WebIDLLexer.LexemeType.EQUALS -> {
                        lexer.next()
                        val value = lexer.current

                        when (value.type) {
                            // [Exposed=*]
                            WebIDLLexer.LexemeType.WILDCARD -> {
                                lexer.next()
                                IdlExtendedAttribute.Wildcard(
                                    name = IdlName(nameLexeme, isReference = false),
                                    bounds = IdlElementBounds(nameLexeme, value)
                                )
                            }

                            // [Reflect="popover"]
                            WebIDLLexer.LexemeType.STRING -> {
                                lexer.next()
                                IdlExtendedAttribute.StringValue(
                                    name = IdlName(nameLexeme, isReference = false),
                                    value = value.content,
                                    bounds = IdlElementBounds(nameLexeme, value)
                                )
                            }

                            // [ReflectDefault=2]
                            WebIDLLexer.LexemeType.INTEGER -> {
                                lexer.next()
                                IdlExtendedAttribute.IntegerValue(
                                    name = IdlName(nameLexeme, isReference = false),
                                    value = value.content.toInt(),
                                    bounds = IdlElementBounds(nameLexeme, value)
                                )
                            }

                            // [ReflectDefault=2.0]
                            WebIDLLexer.LexemeType.DECIMAL -> {
                                lexer.next()
                                IdlExtendedAttribute.DecimalValue(
                                    name = IdlName(nameLexeme, isReference = false),
                                    value = value.content.toDouble(),
                                    bounds = IdlElementBounds(nameLexeme, value)
                                )
                            }

                            WebIDLLexer.LexemeType.IDENTIFIER -> {
                                lexer.next()

                                when (lexer.current.type) {
                                    // [PutForwards=name]
                                    WebIDLLexer.LexemeType.COMMA, WebIDLLexer.LexemeType.R_SQUARE_BRACKET -> {
                                        IdlExtendedAttribute.IdentifierValue(
                                            name = IdlName(nameLexeme, isReference = false),
                                            identifier = IdlName(value),
                                            bounds = IdlElementBounds(nameLexeme, value)
                                        )
                                    }

                                    // [LegacyFactoryFunction=Image(DOMString src)]
                                    WebIDLLexer.LexemeType.L_ROUND_BRACKET -> {
                                        lexer.next()
                                        val args = parseArguments()
                                        IdlExtendedAttribute.NamedArgList(
                                            name = IdlName(nameLexeme, isReference = false),
                                            identifier = IdlName(value, isReference = false),
                                            args = args,
                                            bounds = IdlElementBounds(
                                                startOffset = nameLexeme.bounds.startOffset,
                                                endOffset = lexer.current.bounds.startOffset,
                                                lineIndex = nameLexeme.bounds.lineCharIndex,
                                                lineCharIndex = nameLexeme.bounds.lineCharIndex
                                            )
                                        )
                                    }

                                    else -> throw WebIDLSyntaxErrorException(value, "Expected function or identifier.")
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
                                val lastLexeme = lexer.current
                                lexer.next()

                                if (elements.isNotEmpty()) {
                                    when (elements[0].type) {
                                        // [ReflectRange=(2, 600)]
                                        WebIDLLexer.LexemeType.INTEGER -> {
                                            IdlExtendedAttribute.IntegerList(
                                                name = IdlName(nameLexeme, isReference = false),
                                                array = elements.map { it.content.toInt() },
                                                bounds = IdlElementBounds(nameLexeme, lastLexeme)
                                            )
                                        }

                                        // [Exposed=(Window,Worker)]
                                        WebIDLLexer.LexemeType.IDENTIFIER -> {
                                            IdlExtendedAttribute.IdentifierList(
                                                name = IdlName(nameLexeme, isReference = false),
                                                identifiers = elements.map { IdlName(it) },
                                                bounds = IdlElementBounds(nameLexeme, lastLexeme)
                                            )
                                        }

                                        else -> throw WebIDLSyntaxErrorException(elements[0], "Unsupported array type.")
                                    }
                                } else // [Exposed=()]
                                    IdlExtendedAttribute.IdentifierList(
                                        name = IdlName(nameLexeme, isReference = false),
                                        identifiers = emptyList(),
                                        bounds = IdlElementBounds(nameLexeme, lastLexeme)
                                    )
                            }

                            else -> throw WebIDLSyntaxErrorException(lexer.current, "Unsupported attribute value.")
                        }
                    }

                    // [Constructor(double x, double y)]
                    WebIDLLexer.LexemeType.L_ROUND_BRACKET -> {
                        lexer.next()
                        val args = parseArguments()
                        IdlExtendedAttribute.ArgList(
                            name = IdlName(nameLexeme, isReference = false),
                            args = args,
                            bounds = IdlElementBounds(
                                startOffset = nameLexeme.bounds.startOffset,
                                endOffset = lexer.current.bounds.startOffset,
                                lineIndex = nameLexeme.bounds.lineCharIndex,
                                lineCharIndex = nameLexeme.bounds.lineCharIndex
                            )
                        )
                    }
                    // [Replaceable]
                    WebIDLLexer.LexemeType.COMMA, WebIDLLexer.LexemeType.R_SQUARE_BRACKET -> {
                        IdlExtendedAttribute.NoArgs(
                            name = IdlName(nameLexeme, isReference = false),
                            bounds = nameLexeme.bounds
                        )
                    }

                    else -> throw WebIDLSyntaxErrorException(lexer.current, "Unsupported attribute type.")
                }
                if (lexer.current.type == WebIDLLexer.LexemeType.COMMA)
                    lexer.next()
            }
        }
        val lastLexeme = lexer.current
        lexer.next()

        return IdlAttributes(
            list = children.toMutableList(),
            bounds = IdlElementBounds(firstLexeme, lastLexeme)
        )
    }
}