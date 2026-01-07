package com.huskerdev.webidl.parser

import com.huskerdev.webidl.lexer.WebIDLLexer


sealed class WebIDLDefinition {
    fun toString(spaces: Int): String =
        StringBuilder()
            .apply { toString(spaces, this) }
            .toString()

    abstract fun toString(spaces: Int, builder: StringBuilder)

    override fun toString() =
        toString(2)
}

sealed class WebIDLDefinitionContainer<T: WebIDLDefinition>: WebIDLDefinition() {
    val definitions = ArrayList<T>()
}

sealed class WebIDLSimpleDefinitionContainer: WebIDLDefinitionContainer<WebIDLDefinition>()

sealed interface IdlAttributedHolder {
    val attributes: List<WebIDLExtendedAttributeDef>

    fun printAttributes(
        builder: StringBuilder,
        newLine: Boolean = true
    ){
        if(attributes.isEmpty())
            return
        builder.append("[")
        attributes.forEachIndexed { index, def ->
            def.toString(0, builder)
            if(index != attributes.lastIndex)
                builder.append(", ")
        }
        builder.append("]")
        if(newLine)
            builder.append('\n')
        else builder.append(" ")
    }
}

class WebIDLDefinitionRoot: WebIDLSimpleDefinitionContainer() {
    override fun toString(spaces: Int, builder: StringBuilder) {
        definitions.forEachIndexed { index, def ->
            if(index != 0)
                builder.append("\n")
            def.toString(spaces, builder)
        }
    }
}

// interface
data class WebIDLInterfaceDef(
    val name: String,
    val isPartial: Boolean,
    val isMixin: Boolean,
    val isCallback: Boolean,
    val implements: String?,
    override val attributes: List<WebIDLExtendedAttributeDef>
): WebIDLSimpleDefinitionContainer(), IdlAttributedHolder {
    override fun toString(spaces: Int, builder: StringBuilder) {
        builder.append("\n")
        printAttributes(builder)
        if(isPartial) builder.append("partial ")
        if(isCallback) builder.append("callback ")
        builder.append("interface ")
        if(isMixin) builder.append("mixin ")
        if(implements != null) builder.append(": ").append(implements).append(" ")
        builder.append("{\n")

        val indent = " ".repeat(spaces)
        definitions.forEach{ def ->
            if(definitions.size != 1)
                builder.append("\n")
            builder.append(indent)
            def.toString(spaces, builder)
            builder.append(";\n")
        }
        builder.append("};")
    }
}

// namespace
data class WebIDLNamespaceDef(
    val name: String,
    val isPartial: Boolean,
    override val attributes: List<WebIDLExtendedAttributeDef>
): WebIDLSimpleDefinitionContainer(), IdlAttributedHolder {
    override fun toString(spaces: Int, builder: StringBuilder) {
        builder.append("\n")
        printAttributes(builder)
        if(isPartial) builder.append("partial ")
        builder.append("namespace ")
            .append(name)
            .append(" {\n")

        val indent = " ".repeat(spaces)
        definitions.forEach {
            if(definitions.size != 1)
                builder.append("\n")
            builder.append(indent)
            it.toString(spaces, builder)
            builder.append(";\n")
        }
        builder.append("};")
    }
}

// dictionary
data class WebIDLDictionaryDef(
    val name: String,
    val implements: String?,
    val isPartial: Boolean,
    override val attributes: List<WebIDLExtendedAttributeDef>
): WebIDLSimpleDefinitionContainer(), IdlAttributedHolder {
    override fun toString(spaces: Int, builder: StringBuilder) {
        builder.append("\n")
        printAttributes(builder)
        if(isPartial) builder.append("partial ")
        builder.append("dictionary ")
        if(implements != null) builder.append(": ").append(implements).append(" ")
        builder.append("{\n")

        val indent = " ".repeat(spaces)
        definitions.forEach {
            if(definitions.size != 1)
                builder.append("\n")
            builder.append(indent)
            it.toString(spaces, builder)
            builder.append(";\n")
        }
        builder.append("};")
    }
}

// callback function
data class WebIDLCallbackFunctionDef(
    val name: String,
    val function: WebIDLFunctionDef,
    override val attributes: List<WebIDLExtendedAttributeDef>
): WebIDLDefinition(), IdlAttributedHolder {
    override fun toString(spaces: Int, builder: StringBuilder) {
        builder.append("\n")
        printAttributes(builder)
        builder.append("callback ")
            .append(name)
            .append(" = ")
        function.toString(spaces, builder)
        builder.append(";")
    }
}

// typedef
data class WebIDLTypeDefDef(
    val name: String,
    val type: WebIDLType,
    override val attributes: List<WebIDLExtendedAttributeDef>
): WebIDLDefinition(), IdlAttributedHolder {
    override fun toString(spaces: Int, builder: StringBuilder) {
        builder.append("\n")
        printAttributes(builder)
        builder.append("typedef ")
            .append(type)
            .append(" ")
            .append(name)
            .append(";")
    }
}

// enum
data class WebIDLEnumDef(
    val name: String,
    override val attributes: List<WebIDLExtendedAttributeDef>
): WebIDLDefinitionContainer<WebIDLEnumElementDef>(), IdlAttributedHolder {
    override fun toString(spaces: Int, builder: StringBuilder) {
        builder.append("\n")
        printAttributes(builder)
        builder.append("enum ")
            .append(name)
            .append(" {\n")


        definitions.forEachIndexed { index, element ->
            element.toString(spaces, builder)
            if(index != definitions.lastIndex)
                builder.append(",")
            builder.append("\n")
        }
        builder.append("};")
    }
}

data class WebIDLEnumElementDef(
    val name: String
): WebIDLDefinition() {
    override fun toString(spaces: Int, builder: StringBuilder) {
        val indent = " ".repeat(spaces)
        builder.append(indent)
            .append("\"")
            .append(name)
            .append("\"")
    }
}

// includes
data class WebIDLIncludesDef(
    val target: String,
    val source: String
): WebIDLDefinition() {
    override fun toString(spaces: Int, builder: StringBuilder) {
        builder.append("\n")
            .append(target)
            .append(" includes ")
            .append(source)
            .append(";")
    }
}

// implements
data class WebIDLImplementsDef(
    val target: String,
    val source: String
): WebIDLDefinition() {
    override fun toString(spaces: Int, builder: StringBuilder) {
        builder.append("\n")
            .append(target)
            .append(" implements ")
            .append(source)
            .append(";")
    }
}

// constructor
data class WebIDLConstructorDef(
    val args: List<WebIDLFieldDef>,
    override val attributes: List<WebIDLExtendedAttributeDef>
): WebIDLDefinition(), IdlAttributedHolder {
    override fun toString(spaces: Int, builder: StringBuilder) {
        printAttributes(builder)
        builder.append("constructor(")
        args.forEachIndexed { index, def ->
            def.toString(spaces, builder)
            if(index != args.lastIndex)
                builder.append(", ")
        }
        builder.append(")")
    }
}

// function
data class WebIDLFunctionDef(
    val name: String,
    val type: WebIDLType,
    val args: List<WebIDLFieldDef>,
    val isStatic: Boolean,
    override val attributes: List<WebIDLExtendedAttributeDef>,
): WebIDLDefinition(), IdlAttributedHolder {
    override fun toString(spaces: Int, builder: StringBuilder) {
        printAttributes(builder)
        builder.append(type)
            .append(" ")
            .append(name)
            .append("(")
        args.forEachIndexed { index, def ->
            def.toString(spaces, builder)
            if(index != args.lastIndex)
                builder.append(", ")
        }
        builder.append(")")
    }
}

// field
data class WebIDLFieldDef(
    val name: String,
    val type: WebIDLType,
    val value: Value?,
    val isAttribute: Boolean,
    val isStatic: Boolean,
    val isReadOnly: Boolean,
    val isInherit: Boolean,
    val isOptional: Boolean,
    val isConst: Boolean,
    val isVariadic: Boolean,
    val isRequired: Boolean,
    override val attributes: List<WebIDLExtendedAttributeDef>
): WebIDLDefinition(), IdlAttributedHolder {
    override fun toString(spaces: Int, builder: StringBuilder) {
        printAttributes(builder)
        if(isStatic) builder.append("static ")
        if(isReadOnly) builder.append("readonly ")
        if(isInherit) builder.append("inherit ")
        if(isOptional) builder.append("optional ")
        if(isConst) builder.append("const ")
        if(isAttribute) builder.append("attribute ")
        if(isRequired) builder.append("required")
        builder.append(type)
        if(isVariadic)
            builder.append("...")
        builder.append(" ")
            .append(name)
        if(value != null)
            builder.append(" = ").append(value)
    }

    interface Value

    object NullValue: Value {
        override fun toString() = "null"
    }

    object DictionaryInitValue: Value {
        override fun toString() = "{}"
    }

    class StringValue(
        val text: String
    ): Value {
        override fun toString() = "\"$text\""
    }

    class IntValue(
        val text: String,
        val number: Int = when {
            "0x" in text.lowercase() -> text.drop(2).toInt(16)
            "0o" in text.lowercase() -> text.drop(2).toInt(8)
            "0b" in text.lowercase() -> text.drop(2).toInt(2)
            else -> text.toInt()
        }
    ): Value {
        override fun toString() = text
    }

    class DecimalValue(
        val text: String,
        val number: Double = text.toDouble()
    ): Value {
        override fun toString() = text
    }

    class BooleanValue(
        val boolValue: Boolean
    ): Value {
        override fun toString() = boolValue.toString()
    }
}


// iterable
class WebIDLIterableDef(
    val keyType: WebIDLType,
    val valueType: WebIDLType?
): WebIDLDefinition() {
    override fun toString(spaces: Int, builder: StringBuilder) {
        builder.append("iterable<")
            .append(keyType)
        if(valueType != null)
            builder.append(", ").append(valueType)
        builder.append(">")
    }
}

// async_iterable
class WebIDLAsyncIterableLikeDef(
    val keyType: WebIDLType,
    val valueType: WebIDLType?
): WebIDLDefinition() {
    override fun toString(spaces: Int, builder: StringBuilder) {
        builder.append("async_iterable<")
            .append(keyType)
        if(valueType != null)
            builder.append(", ").append(valueType)
        builder.append(">")
    }
}

class WebIDLMapLikeDef(
    val keyType: WebIDLType,
    val valueType: WebIDLType,
    val isReadOnly: Boolean
): WebIDLDefinition() {
    override fun toString(spaces: Int, builder: StringBuilder) {
        builder.append("maplike<")
            .append(keyType)
            .append(", ")
            .append(valueType)
            .append(">")
    }
}

class WebIDLSetLikeDef(
    val type: WebIDLType,
    val isReadOnly: Boolean
): WebIDLDefinition() {
    override fun toString(spaces: Int, builder: StringBuilder) {
        builder.append("setlike<")
            .append(type)
            .append(">")
    }
}

class WebIDLStringifierDef(
     val field: WebIDLFieldDef?
): WebIDLDefinition() {
    override fun toString(spaces: Int, builder: StringBuilder) {
        builder.append("stringifier")
        if(field != null) {
            builder.append(" ")
            field.toString(spaces, builder)
        }
    }
}

class WebIDLGetterDef(
    val function: WebIDLFunctionDef
): WebIDLDefinition() {
    override fun toString(spaces: Int, builder: StringBuilder) {
        builder.append("getter")
            .append(" ")
        function.toString(spaces, builder)
    }
}

class WebIDLSetterDef(
    val function: WebIDLFunctionDef
): WebIDLDefinition() {
    override fun toString(spaces: Int, builder: StringBuilder) {
        builder.append("setter")
            .append(" ")
        function.toString(spaces, builder)
    }
}

// extended attributes
sealed class WebIDLExtendedAttributeDef: WebIDLDefinition() {
    abstract val firstLexeme: WebIDLLexer.Lexeme
    abstract val name: String
}

data class WebIDLExtendedAttributeDefNoArgs(
    override val firstLexeme: WebIDLLexer.Lexeme,
    override val name: String
): WebIDLExtendedAttributeDef() {
    override fun toString(spaces: Int, builder: StringBuilder) {
        builder.append(name)
    }
}

data class WebIDLExtendedAttributeDefArgList(
    override val firstLexeme: WebIDLLexer.Lexeme,
    override val name: String,
    val args: List<WebIDLFieldDef>
): WebIDLExtendedAttributeDef() {
    override fun toString(spaces: Int, builder: StringBuilder) {
        builder.append(name)
            .append("(")
        args.forEachIndexed { index, def ->
            def.toString(spaces, builder)
            if(index != args.lastIndex)
                builder.append(", ")
        }
        builder.append(")")
    }
}

data class WebIDLExtendedAttributeDefNamedArgList(
    override val firstLexeme: WebIDLLexer.Lexeme,
    override val name: String,
    val function: WebIDLFunctionDef
): WebIDLExtendedAttributeDef() {
    override fun toString(spaces: Int, builder: StringBuilder) {
        builder.append(name)
            .append("=")
        function.toString(spaces, builder)
    }
}

data class WebIDLExtendedAttributeDefIdent(
    override val firstLexeme: WebIDLLexer.Lexeme,
    override val name: String,
    val identifier: String
): WebIDLExtendedAttributeDef() {
    override fun toString(spaces: Int, builder: StringBuilder) {
        builder.append(name)
            .append("=")
            .append(identifier)
    }
}

data class WebIDLExtendedAttributeDefString(
    override val firstLexeme: WebIDLLexer.Lexeme,
    override val name: String,
    val value: String
): WebIDLExtendedAttributeDef() {
    override fun toString(spaces: Int, builder: StringBuilder) {
        builder.append(name)
            .append("=")
            .append(value)
    }
}

data class WebIDLExtendedAttributeDefInteger(
    override val firstLexeme: WebIDLLexer.Lexeme,
    override val name: String,
    val value: Int
): WebIDLExtendedAttributeDef() {
    override fun toString(spaces: Int, builder: StringBuilder) {
        builder.append(name)
            .append("=")
            .append(value)
    }
}

data class WebIDLExtendedAttributeDefDecimal(
    override val firstLexeme: WebIDLLexer.Lexeme,
    override val name: String,
    val value: Double
): WebIDLExtendedAttributeDef() {
    override fun toString(spaces: Int, builder: StringBuilder) {
        builder.append(name)
            .append("=")
            .append(value)
    }
}

data class WebIDLExtendedAttributeDefIntegerList(
    override val firstLexeme: WebIDLLexer.Lexeme,
    override val name: String,
    val array: List<Int>
): WebIDLExtendedAttributeDef() {
    override fun toString(spaces: Int, builder: StringBuilder) {
        builder.append(name)
            .append("=(")
        array.joinTo(builder, ", ")
        builder.append(")")
    }
}

data class WebIDLExtendedAttributeDefIdentList(
    override val firstLexeme: WebIDLLexer.Lexeme,
    override val name: String,
    val array: List<String>
): WebIDLExtendedAttributeDef() {
    override fun toString(spaces: Int, builder: StringBuilder) {
        builder.append(name)
            .append("=(")
        array.joinTo(builder, ",")
        builder.append(")")
    }
}

data class WebIDLExtendedAttributeDefWildcard(
    override val firstLexeme: WebIDLLexer.Lexeme,
    override val name: String,
): WebIDLExtendedAttributeDef() {
    override fun toString(spaces: Int, builder: StringBuilder) {
        builder.append(name)
            .append("=*")
    }
}