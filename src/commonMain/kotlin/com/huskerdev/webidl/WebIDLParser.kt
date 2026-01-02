package com.huskerdev.webidl

import com.huskerdev.webidl.def.IdlExtendedAttributeDef
import com.huskerdev.webidl.def.IdlExtendedAttributeDefArgList
import com.huskerdev.webidl.def.IdlExtendedAttributeDefDecimal
import com.huskerdev.webidl.def.IdlExtendedAttributeDefIdent
import com.huskerdev.webidl.def.IdlExtendedAttributeDefIdentList
import com.huskerdev.webidl.def.IdlExtendedAttributeDefInteger
import com.huskerdev.webidl.def.IdlExtendedAttributeDefIntegerList
import com.huskerdev.webidl.def.IdlExtendedAttributeDefNamedArgList
import com.huskerdev.webidl.def.IdlExtendedAttributeDefNoArgs
import com.huskerdev.webidl.def.IdlExtendedAttributeDefString
import com.huskerdev.webidl.def.IdlExtendedAttributeDefWildcard
import com.huskerdev.webidl.def.IdlAsyncIterableLikeDef
import com.huskerdev.webidl.def.IdlCallbackFunctionDef
import com.huskerdev.webidl.def.IdlConstructorDef
import com.huskerdev.webidl.def.IdlDefinition
import com.huskerdev.webidl.def.IdlDefinitionRoot
import com.huskerdev.webidl.def.IdlDictionaryDef
import com.huskerdev.webidl.def.IdlEnumDef
import com.huskerdev.webidl.def.IdlFieldDef
import com.huskerdev.webidl.def.IdlFunctionDef
import com.huskerdev.webidl.def.IdlImplementsDef
import com.huskerdev.webidl.def.IdlIncludesDef
import com.huskerdev.webidl.def.IdlInterfaceDef
import com.huskerdev.webidl.def.IdlIterableDef
import com.huskerdev.webidl.def.IdlMapLikeDef
import com.huskerdev.webidl.def.IdlNamespaceDef
import com.huskerdev.webidl.def.IdlSetLikeDef
import com.huskerdev.webidl.def.IdlTypeDefDef


class WebIDLParser private constructor(
    iterator: Iterator<String>
) {
    companion object {
        fun parse(iterator: Iterator<String>) =
            WebIDLParser(iterator).parse()

        fun parse(text: String) =
            WebIDLParser(listOf(text).iterator()).parse()

        fun parse(lineSequence: Sequence<String>) =
            WebIDLParser(lineSequence.iterator()).parse()

        fun parse(lines: Iterable<String>) =
            WebIDLParser(lines.iterator()).parse()


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
    }

    private val lexer = WebIDLLexer(iterator)

    private lateinit var buffered: IdlDefinitionRoot


    fun parse(): IdlDefinitionRoot {
        if(::buffered.isInitialized)
            return buffered

        val definitions = buildList {
            var attributes = arrayListOf<IdlExtendedAttributeDef>()

            while (lexer.hasNext()) {
                lexer.next()

                when (lexer.current.type) {
                    WebIDLLexer.LexemeType.L_SQUARE_BRACKET -> {
                        attributes += parseExtendedAttributes()
                    }
                    WebIDLLexer.LexemeType.KEYWORD -> {
                        this@buildList += when (lexer.current.content) {
                            "interface", -> parseInterface(attributes)
                            "dictionary" -> parseDictionary(attributes)
                            "namespace" -> parseNamespace(attributes)
                            "enum" -> parseEnum(attributes)
                            "typedef" -> parseTypeDef(attributes)
                            "partial" -> when(lexer.next().content) {
                                "interface" -> parseInterface(attributes, isPartial = true)
                                "dictionary" -> parseDictionary(attributes, isPartial = true)
                                "namespace" -> parseNamespace(attributes, isPartial = true)
                                else -> throw UnsupportedOperationException()
                            }
                            "callback" -> when(lexer.next().type) {
                                WebIDLLexer.LexemeType.IDENTIFIER -> parseCallbackFunction(attributes)
                                else -> parseInterface(attributes, isCallback = true)
                            }
                            else -> throw UnsupportedOperationException("Unexpected symbol: ${lexer.current.content}")
                        }
                        if(lexer.current.type != WebIDLLexer.LexemeType.SEMICOLON &&
                            lexer.next().type != WebIDLLexer.LexemeType.SEMICOLON
                        ) throw UnsupportedOperationException("Expected ';'")
                        attributes = arrayListOf()
                    }
                    WebIDLLexer.LexemeType.IDENTIFIER -> {
                        val identifier1 = lexer.current
                        val action = lexer.next()
                        val identifier2 = lexer.next()

                        if (action.type != WebIDLLexer.LexemeType.KEYWORD)
                            throw UnsupportedOperationException("Expected 'includes' or 'implements' (${action})")
                        if (identifier2.type != WebIDLLexer.LexemeType.IDENTIFIER)
                            throw UnsupportedOperationException("Expected identifier")
                        if (lexer.next().type != WebIDLLexer.LexemeType.SEMICOLON)
                            throw UnsupportedOperationException("Expected ';'")

                        this@buildList += when (action.content) {
                            "includes" -> IdlIncludesDef(identifier1.content, identifier2.content)
                            "implements" -> IdlImplementsDef(identifier1.content, identifier2.content)
                            else -> throw UnsupportedOperationException()
                        }
                    }
                    else -> throw UnsupportedOperationException("Unexpected symbol: ${lexer.current.content}")
                }
            }
        }
        buffered = IdlDefinitionRoot(definitions)
        return buffered
    }

    private fun parseNamespace(
        attributes: List<IdlExtendedAttributeDef>,
        isPartial: Boolean = false
    ): IdlNamespaceDef {
        if(lexer.next().type != WebIDLLexer.LexemeType.IDENTIFIER)
            throw UnsupportedOperationException("Expected identifier")

        val name = lexer.current.content

        if(lexer.next().type != WebIDLLexer.LexemeType.L_CURLY_BRACKET)
            throw UnsupportedOperationException("Expected '{'")
        lexer.next()

        val definitions = arrayListOf<IdlDefinition>()

        while(lexer.current.type != WebIDLLexer.LexemeType.R_CURLY_BRACKET) {
            definitions += parseFieldOrFunction(
                allowReadonly = true,
                allowAttribute = true
            )

            if(lexer.current.type != WebIDLLexer.LexemeType.SEMICOLON)
                throw UnsupportedOperationException("Expected ';' (${lexer.current.content})")
            else lexer.next()
        }
        return IdlNamespaceDef(name, isPartial, definitions, attributes)
    }

    private fun parseAnonymousFunction(
        firstLexeme: WebIDLLexer.Lexeme? = null
    ): IdlFunctionDef {
        val parts = arrayListOf<WebIDLLexer.Lexeme>()
        var brackets = 0

        fun check(lexeme: WebIDLLexer.Lexeme){
            if(lexeme.type in openBrackets) brackets++
            if(lexeme.type in closeBrackets) brackets--
            parts += lexeme
        }

        if(firstLexeme != null)
            check(firstLexeme)

        while(true) {
            if(lexer.current.type == WebIDLLexer.LexemeType.L_ROUND_BRACKET && brackets == 0)
                break

            check(lexer.current)
            lexer.next()
        }
        lexer.next()

        return IdlFunctionDef("", parseType(parts), parseArguments())
    }

    private fun parseCallbackFunction(
        attributes: List<IdlExtendedAttributeDef>,
    ): IdlCallbackFunctionDef {
        if(lexer.current.type != WebIDLLexer.LexemeType.IDENTIFIER)
            throw UnsupportedOperationException("Expected identifier")

        val name = lexer.current.content

        if(lexer.next().type != WebIDLLexer.LexemeType.EQUALS)
            throw UnsupportedOperationException("Expected '='")
        lexer.next()

        val function = parseAnonymousFunction()

        return IdlCallbackFunctionDef(name, function.type, function.args, attributes)
    }

    private fun parseTypeDef(
        attributes: List<IdlExtendedAttributeDef>,
    ): IdlTypeDefDef {
        val parts = arrayListOf<WebIDLLexer.Lexeme>()

        while(lexer.next().type != WebIDLLexer.LexemeType.SEMICOLON) {
            parts += lexer.current
        }

        return IdlTypeDefDef(
            parts.removeLast().content,
            parseType(parts),
            attributes
        )
    }

    private fun parseEnum(
        attributes: List<IdlExtendedAttributeDef>,
    ): IdlEnumDef {
        if(lexer.next().type != WebIDLLexer.LexemeType.IDENTIFIER)
            throw UnsupportedOperationException("Expected identifier")

        val name = lexer.current.content

        if(lexer.next().type != WebIDLLexer.LexemeType.L_CURLY_BRACKET)
            throw UnsupportedOperationException("Expected '{'")
        lexer.next()

        val elements = arrayListOf<String>()

        while(lexer.current.type != WebIDLLexer.LexemeType.R_CURLY_BRACKET) {
            if(lexer.current.type != WebIDLLexer.LexemeType.STRING)
                throw UnsupportedOperationException("Expected string element")

            elements += lexer.current.content

            if(lexer.next().type == WebIDLLexer.LexemeType.COMMA)
                lexer.next()
        }
        return IdlEnumDef(name, elements, attributes)
    }

    private fun parseDictionary(
        attributes: List<IdlExtendedAttributeDef>,
        isPartial: Boolean = false
    ): IdlDictionaryDef {
        if(lexer.next().type != WebIDLLexer.LexemeType.IDENTIFIER)
            throw UnsupportedOperationException("Expected identifier")

        val name = lexer.current.content

        val implements: String? = if(lexer.next().type == WebIDLLexer.LexemeType.COLON) {
            if(lexer.next().type != WebIDLLexer.LexemeType.IDENTIFIER)
                throw UnsupportedOperationException("Expected identifier")

            lexer.current.content.also {
                lexer.next()
            }
        } else null

        if(lexer.current.type != WebIDLLexer.LexemeType.L_CURLY_BRACKET)
            throw UnsupportedOperationException("Expected '{'")
        lexer.next()

        val definitions = arrayListOf<IdlDefinition>()

        while(lexer.current.type != WebIDLLexer.LexemeType.R_CURLY_BRACKET) {
            definitions += parseFieldOrFunction(
                allowOptional = true,
                allowRequired = true
            )

            if(lexer.current.type != WebIDLLexer.LexemeType.SEMICOLON)
                throw UnsupportedOperationException("Expected ';' (${lexer.current.content})")
            else lexer.next()
        }
        return IdlDictionaryDef(name, implements, isPartial, definitions, attributes)
    }

    private fun parseInterface(
        attributes: List<IdlExtendedAttributeDef>,
        isPartial: Boolean = false,
        isCallback: Boolean = false,
    ): IdlInterfaceDef {

        if(lexer.current.content != "interface")
            throw UnsupportedOperationException("Expected interface")

        val isMixin = if(lexer.next().content == "mixin"){
            if(isCallback)
                throw UnsupportedOperationException("Can not use mixin with callback interface")
            lexer.next()
            true
        } else false

        if(lexer.current.type != WebIDLLexer.LexemeType.IDENTIFIER)
            throw UnsupportedOperationException("Expected identifier")

        val name = lexer.current.content

        val implements: String? = if(lexer.next().type == WebIDLLexer.LexemeType.COLON) {
            if(lexer.next().type != WebIDLLexer.LexemeType.IDENTIFIER)
                throw UnsupportedOperationException("Expected identifier")

            lexer.current.content.also {
                lexer.next()
            }
        } else null

        if(lexer.current.type != WebIDLLexer.LexemeType.L_CURLY_BRACKET)
            throw UnsupportedOperationException("Expected '{'")
        lexer.next()

        val definitions = arrayListOf<IdlDefinition>()

        while(lexer.current.type != WebIDLLexer.LexemeType.R_CURLY_BRACKET) {
            definitions += when(lexer.current.content) {
                "iterable" ->
                    IdlIterableDef(parseSingleGeneric())
                "async_iterable" ->
                    IdlAsyncIterableLikeDef(parseSingleGeneric())
                "maplike" -> parseDoubleGeneric().run {
                    IdlMapLikeDef(first, second)
                }
                "setlike" ->
                    IdlSetLikeDef(parseSingleGeneric())
                "stringifier" -> {
                    // todo: implement with field
                    throw UnsupportedOperationException()
                }
                "constructor" -> {
                    if(lexer.next().type != WebIDLLexer.LexemeType.L_ROUND_BRACKET)
                        throw UnsupportedOperationException("Expected '('")
                    else lexer.next()

                    IdlConstructorDef(parseArguments())
                }
                else -> {
                    parseFieldOrFunction(
                        allowConst = true,
                        allowStatic = true,
                        allowInherit = true,
                        allowAttribute = true,
                        allowReadonly = true
                    )
                }
            }
            if(lexer.current.type != WebIDLLexer.LexemeType.SEMICOLON)
                throw UnsupportedOperationException("Expected ';' (${lexer.current.content})")
            else lexer.next()
        }
        return IdlInterfaceDef(
            name,
            isPartial, isMixin, isCallback,
            implements,
            definitions,
            attributes
        )
    }

    private fun parseSingleGeneric(): String {
        if(lexer.next().type != WebIDLLexer.LexemeType.L_ANGLE_BRACKET)
            throw UnsupportedOperationException("Expected '<'")
        if(lexer.next().type != WebIDLLexer.LexemeType.IDENTIFIER)
            throw UnsupportedOperationException("Expected identifier")

        val result = lexer.current.content
        if(lexer.next().type != WebIDLLexer.LexemeType.R_ANGLE_BRACKET)
            throw UnsupportedOperationException("Expected '>'")
        if(lexer.next().type != WebIDLLexer.LexemeType.SEMICOLON)
            throw UnsupportedOperationException("Expected ';'")
        return result
    }

    private fun parseDoubleGeneric(): Pair<String, String> {
        if(lexer.next().type != WebIDLLexer.LexemeType.L_ANGLE_BRACKET)
            throw UnsupportedOperationException("Expected '<'")
        if(lexer.next().type != WebIDLLexer.LexemeType.IDENTIFIER)
            throw UnsupportedOperationException("Expected identifier")

        val result1 = lexer.current.content
        if(lexer.next().type != WebIDLLexer.LexemeType.COMMA)
            throw UnsupportedOperationException("Expected ','")
        if(lexer.next().type != WebIDLLexer.LexemeType.IDENTIFIER)
            throw UnsupportedOperationException("Expected identifier")

        val result2 = lexer.current.content
        if(lexer.next().type != WebIDLLexer.LexemeType.R_ANGLE_BRACKET)
            throw UnsupportedOperationException("Expected '>'")
        if(lexer.next().type != WebIDLLexer.LexemeType.SEMICOLON)
            throw UnsupportedOperationException("Expected ';'")
        return result1 to result2
    }

    private fun parseType(parts: List<WebIDLLexer.Lexeme>): String {
        if(parts.size == 1)
            return parts[0].content

        return if(
            parts[0].type == WebIDLLexer.LexemeType.IDENTIFIER &&
            parts[1].type == WebIDLLexer.LexemeType.IDENTIFIER
        ) parts.joinToString(" ") { it.content }
        else parts.joinToString("") { it.content }
    }

    private fun parseFieldOrFunction(
        allowStatic: Boolean = false,
        allowReadonly: Boolean = false,
        allowAttribute: Boolean = false,
        allowInherit: Boolean = false,
        allowConst: Boolean = false,
        allowOptional: Boolean = false,
        allowRequired: Boolean = false,
    ): IdlDefinition {

        val parts = arrayListOf<WebIDLLexer.Lexeme>()

        var brackets = 0
        while(true) {
            when (lexer.current.type) {
                WebIDLLexer.LexemeType.L_ROUND_BRACKET,
                WebIDLLexer.LexemeType.L_ANGLE_BRACKET -> brackets++
                WebIDLLexer.LexemeType.R_ROUND_BRACKET,
                WebIDLLexer.LexemeType.R_ANGLE_BRACKET -> brackets--
                else -> Unit
            }
            // Trying to detect ending (name)
            if(lexer.current.type == WebIDLLexer.LexemeType.IDENTIFIER && brackets == 0) {
                parts += lexer.current
                if(lexer.next().content in setOf("(", ")", ",", ";", "="))
                    break
                else continue
            }

            parts += lexer.current
            lexer.next()
        }

        val isStatic = parts.removeIf { it.content == "static" }
        val isReadonly = parts.removeIf { it.content == "readonly" }
        val isAttribute = parts.removeIf { it.content == "attribute" }
        val isInherit = parts.removeIf { it.content == "inherit" }
        val isConst = parts.removeIf { it.content == "const" }
        val isOptional = parts.removeIf { it.content == "optional" }
        val isRequired = parts.removeIf { it.content == "required" }

        if(isStatic && !allowStatic)       throw UnsupportedOperationException("'static' is not allowed here")
        if(isReadonly && !allowReadonly)   throw UnsupportedOperationException("'readonly' is not allowed here")
        if(isAttribute && !allowAttribute) throw UnsupportedOperationException("'attribute' is not allowed here")
        if(isInherit && !allowInherit)     throw UnsupportedOperationException("'inherit' is not allowed here")
        if(isConst && !allowConst)         throw UnsupportedOperationException("'const' is not allowed here")
        if(isOptional && !allowOptional)   throw UnsupportedOperationException("'optional' is not allowed here")
        if(isRequired && !allowRequired)   throw UnsupportedOperationException("'required' is not allowed here")

        val name = parts.removeLast().content
        val type = parseType(parts)

        return if(lexer.current.type == WebIDLLexer.LexemeType.L_ROUND_BRACKET) {
            lexer.next()
            IdlFunctionDef(name, type, parseArguments())
        } else {
            val value = if(lexer.current.type == WebIDLLexer.LexemeType.EQUALS)
                lexer.next().also { lexer.next() }
            else null

            IdlFieldDef(
                name, type, value,
                isAttribute, isStatic, isReadonly,
                isInherit, isOptional, isConst,
            )
        }
    }

    private fun parseArguments(): List<IdlFieldDef> = buildList {
        while (lexer.current.type != WebIDLLexer.LexemeType.R_ROUND_BRACKET) {
            this@buildList += parseFieldOrFunction(
                allowOptional = true
            ) as? IdlFieldDef ?: throw UnsupportedOperationException("Expected field")

            if (lexer.current.type == WebIDLLexer.LexemeType.COMMA)
                lexer.next()
        }
        lexer.next()
    }

    private fun parseExtendedAttributes(): List<IdlExtendedAttributeDef> = buildList {
        lexer.next()
        while (lexer.current.type != WebIDLLexer.LexemeType.R_SQUARE_BRACKET) {

            if (lexer.current.type != WebIDLLexer.LexemeType.IDENTIFIER)
                throw UnsupportedOperationException("Expected identifier (${lexer.current})")

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
                            IdlExtendedAttributeDefWildcard(name)
                        }

                        // [Reflect="popover"]
                        WebIDLLexer.LexemeType.STRING -> {
                            lexer.next()
                            IdlExtendedAttributeDefString(name, value.content)
                        }

                        // [ReflectDefault=2]
                        WebIDLLexer.LexemeType.INTEGER -> {
                            lexer.next()
                            IdlExtendedAttributeDefInteger(name, value.content.toInt())
                        }

                        // [ReflectDefault=2.0]
                        WebIDLLexer.LexemeType.DECIMAL -> {
                            lexer.next()
                            IdlExtendedAttributeDefDecimal(name, value.content.toDouble())
                        }

                        WebIDLLexer.LexemeType.IDENTIFIER -> {
                            val identifier = lexer.current
                            lexer.next()

                            when (lexer.current.type) {
                                // [PutForwards=name]
                                WebIDLLexer.LexemeType.COMMA, WebIDLLexer.LexemeType.R_SQUARE_BRACKET ->
                                    IdlExtendedAttributeDefIdent(name, identifier.content)

                                // [LegacyFactoryFunction=Image(DOMString src)]
                                else -> {
                                    val function = parseAnonymousFunction(identifier)
                                    IdlExtendedAttributeDefNamedArgList(name, function.type, function.args)
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
                                        IdlExtendedAttributeDefIntegerList(
                                            name,
                                            elements.map { it.content.toInt() }
                                        )

                                    // [Exposed=(Window,Worker)]
                                    WebIDLLexer.LexemeType.IDENTIFIER ->
                                        IdlExtendedAttributeDefIdentList(name, elements.map { it.content })

                                    else -> throw UnsupportedOperationException()
                                }
                            } else // // [Exposed=()]
                                IdlExtendedAttributeDefIdentList(name, emptyList())
                        }

                        else -> throw UnsupportedOperationException()
                    }
                }

                // [Constructor(double x, double y)]
                WebIDLLexer.LexemeType.L_ROUND_BRACKET -> {
                    lexer.next()
                    IdlExtendedAttributeDefArgList(name, parseArguments())
                }
                // [Replaceable]
                WebIDLLexer.LexemeType.COMMA, WebIDLLexer.LexemeType.R_SQUARE_BRACKET -> {
                    IdlExtendedAttributeDefNoArgs(name)
                }

                else -> throw UnsupportedOperationException()
            }

            if (lexer.current.type == WebIDLLexer.LexemeType.COMMA)
                lexer.next()
        }
    }
}

fun main(){
    val ast = WebIDL("""
        /* === Extended attributes === */
        
        [Replaceable]
        interface A1 {
        };
        
        [Constructor(double x, double y)]
        interface A2 {
        };
        
        [LegacyFactoryFunction=Image(DOMString src)]
        interface A3 {
        };
        
        [PutForwards=name]
        interface A4 {
        };
        
        [Reflect="popover"]
        interface A5 {
        };
        
        [ReflectDefault=2]
        interface A6 {
        };
        
        [ReflectDefault=2.0]
        interface A7 {
        };
        
        [ReflectRange=(2, 600)]
        interface A8 {
        };
        
        [Exposed=(Window,Worker)]
        interface A9 {
        };
        
        [Exposed=*]
        interface A10 {
        };
        
        [Exposed=(Window,Worker), SecureContext, LegacyUnenumerableNamedProperties]
        interface Window : EventTarget {
        
          /* === Constants === */
          const unsigned short VERSION = 1;
        
          /* === Attributes === */
          readonly attribute DOMString name;
          attribute long length;
          static readonly attribute boolean supported;
          inherit attribute any inheritedValue;
        
          /* === Constructor === */
          constructor(optional DOMString title = "default");
        
          /* === Operations === */
          void alert(DOMString message);
          boolean confirm(optional DOMString message);
        
          /* overload */
          long compute(long x);
          double compute(double x);
        
          /* variadic + optional + default */
          void log(optional DOMString prefix = "", any... data);
        
          /* === Iterable / maplike / setlike === */
          iterable<DOMString>;
          maplike<DOMString, long>;
          setlike<DOMString>;
        
          /* === Promise / sequence / union / nullable === */
          Promise<sequence<(DOMString or long)?>> fetchData();
        };
        
        /* === Partial interface === */
        partial interface Window {
          attribute double ratio;
        };
        
        /* === Interface mixin === */
        interface mixin FooMixin {
          readonly attribute long mixinValue;
          void mixinMethod();
        };
        
        /* === Includes statement === */
        Window includes FooMixin;
        
        /* === Dictionary with inheritance === */
        dictionary BaseOptions {
          boolean enabled = true;
        };
        
        dictionary RequestOptions : BaseOptions {
          DOMString method = "GET";
          HeadersInit? headers;
          sequence<DOMString> tags;
        };
        
        /* === Enum === */
        enum Direction {
          "left",
          "right",
          "up",
          "down"
        };
        
        /* === Typedef === */
        typedef unsigned long long DOMHighResTimeStamp;
        
        /* === Callback === */
        callback Comparator = long (any a, any b);
        
        /* === Callback interface === */
        callback interface EventHandler {
          void handleEvent(Event event);
        };
        
        /* === Namespace === */
        [Exposed=Window]
        namespace Math {
          readonly attribute double PI;
          double sin(double x);
          double cos(double x);
        };
        
        /* === Partial namespace === */
        partial namespace Math {
          double tan(double x);
        };
        
        /* === Implements (legacy, deprecated but встречается) === */
        Window implements EventHandler;
    """.trimIndent())
    println(ast)
}