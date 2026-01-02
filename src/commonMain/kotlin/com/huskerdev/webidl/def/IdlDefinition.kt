package com.huskerdev.webidl.def

import com.huskerdev.webidl.WebIDLLexer


sealed class IdlDefinition {
    fun toString(spaces: Int): String =
        StringBuilder()
            .apply { toString(spaces, this) }
            .toString()

    abstract fun toString(spaces: Int, builder: StringBuilder)

    override fun toString() =
        toString(2)

    protected fun appendAttributes(
        spaces: Int,
        builder: StringBuilder,
        attributes: List<IdlExtendedAttributeDef>,
        newLine: Boolean = true
    ){
        if(attributes.isEmpty())
            return
        builder.append("[")
        attributes.forEachIndexed { index, def ->
            def.toString(spaces, builder)
            if(index != attributes.lastIndex)
                builder.append(", ")
        }
        builder.append("]")
        if(newLine)
            builder.append('\n')
    }
}

class IdlDefinitionRoot(
    val definitions: List<IdlDefinition>
): IdlDefinition() {
    override fun toString(spaces: Int, builder: StringBuilder) {
        definitions.forEachIndexed { index, def ->
            if(index != 0)
                builder.append("\n")
            def.toString(spaces, builder)
        }
    }
}

// interface
data class IdlInterfaceDef(
    val name: String,
    val isPartial: Boolean,
    val isMixin: Boolean,
    val isCallback: Boolean,
    val implements: String?,
    val definitions: List<IdlDefinition>,
    val attributes: List<IdlExtendedAttributeDef>
): IdlDefinition() {
    override fun toString(spaces: Int, builder: StringBuilder) {
        builder.append("\n")
        appendAttributes(spaces, builder, attributes)
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
data class IdlNamespaceDef(
    val name: String,
    val isPartial: Boolean,
    val definitions: List<IdlDefinition>,
    val attributes: List<IdlExtendedAttributeDef>
): IdlDefinition() {
    override fun toString(spaces: Int, builder: StringBuilder) {
        builder.append("\n")
        appendAttributes(spaces, builder, attributes)
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
data class IdlDictionaryDef(
    val name: String,
    val implements: String?,
    val isPartial: Boolean,
    val definitions: List<IdlDefinition>,
    val attributes: List<IdlExtendedAttributeDef>
): IdlDefinition() {
    override fun toString(spaces: Int, builder: StringBuilder) {
        builder.append("\n")
        appendAttributes(spaces, builder, attributes)
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
data class IdlCallbackFunctionDef(
    val name: String,
    val type: String,
    val args: List<IdlFieldDef>,
    val attributes: List<IdlExtendedAttributeDef>
): IdlDefinition() {
    override fun toString(spaces: Int, builder: StringBuilder) {
        builder.append("\n")
        appendAttributes(spaces, builder, attributes)
        builder.append("callback ")
            .append(name)
            .append(" = ")
            .append(type)
            .append(" (")
        args.forEachIndexed { index, def ->
            def.toString(spaces, builder)
            if(index != args.lastIndex)
                builder.append(", ")
        }
        builder.append(");")
    }
}

// typedef
data class IdlTypeDefDef(
    val name: String,
    val type: String,
    val attributes: List<IdlExtendedAttributeDef>
): IdlDefinition() {
    override fun toString(spaces: Int, builder: StringBuilder) {
        builder.append("\n")
        appendAttributes(spaces, builder, attributes)
        builder.append("typedef ")
            .append(type)
            .append(" ")
            .append(name)
            .append(";")
    }
}

// enum
data class IdlEnumDef(
    val name: String,
    val elements: List<String>,
    val attributes: List<IdlExtendedAttributeDef>
): IdlDefinition() {
    override fun toString(spaces: Int, builder: StringBuilder) {
        builder.append("\n")
        appendAttributes(spaces, builder, attributes)
        builder.append("enum ")
            .append(name)
            .append(" {\n")

        val indent = " ".repeat(spaces)
        elements.forEachIndexed { index, element ->
            builder.append(indent)
                .append("\"")
                .append(element)
                .append("\"")
            if(index != element.lastIndex)
                builder.append(",")
            builder.append("\n")
        }
        builder.append("};")
    }
}

// includes
data class IdlIncludesDef(
    val target: String,
    val source: String
): IdlDefinition() {
    override fun toString(spaces: Int, builder: StringBuilder) {
        builder.append("\n")
            .append(target)
            .append(" includes ")
            .append(source)
            .append(";")
    }
}

// implements
data class IdlImplementsDef(
    val target: String,
    val source: String
): IdlDefinition() {
    override fun toString(spaces: Int, builder: StringBuilder) {
        builder.append("\n")
            .append(target)
            .append(" implements ")
            .append(source)
            .append(";")
    }
}

// constructor
data class IdlConstructorDef(
    val args: List<IdlFieldDef>
): IdlDefinition() {
    override fun toString(spaces: Int, builder: StringBuilder) {
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
data class IdlFunctionDef(
    val name: String,
    val type: String,
    val args: List<IdlFieldDef>
): IdlDefinition() {
    override fun toString(spaces: Int, builder: StringBuilder) {
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
data class IdlFieldDef(
    val name: String,
    val type: String,
    val value: WebIDLLexer.Lexeme?,
    val isAttribute: Boolean,
    val isStatic: Boolean,
    val isReadOnly: Boolean,
    val isInherit: Boolean,
    val isOptional: Boolean,
    val isConst: Boolean,
): IdlDefinition() {
    override fun toString(spaces: Int, builder: StringBuilder) {
        if(isStatic) builder.append("static ")
        if(isReadOnly) builder.append("readonly ")
        if(isInherit) builder.append("inherit ")
        if(isOptional) builder.append("optional ")
        if(isConst) builder.append("const ")
        if(isAttribute) builder.append("attribute ")
        builder.append(type)
            .append(" ")
            .append(name)
        if(value != null) {
            builder.append(" = ")
            if(value.type == WebIDLLexer.LexemeType.STRING)
                builder.append("\"").append(value.content).append("\"")
            else
                builder.append(value.content)
        }
    }
}


// iterable
class IdlIterableDef(
    val type: String
): IdlDefinition() {
    override fun toString(spaces: Int, builder: StringBuilder) {
        builder.append("iterable<")
            .append(type)
            .append(">")
    }
}

// async_iterable
class IdlAsyncIterableLikeDef(
    val type: String
): IdlDefinition() {
    override fun toString(spaces: Int, builder: StringBuilder) {
        builder.append("async_iterable<")
            .append(type)
            .append(">")
    }
}

class IdlMapLikeDef(
    val keyType: String,
    val valueType: String
): IdlDefinition() {
    override fun toString(spaces: Int, builder: StringBuilder) {
        builder.append("maplike<")
            .append(keyType)
            .append(", ")
            .append(valueType)
            .append(">")
    }
}

class IdlSetLikeDef(
    val type: String
): IdlDefinition() {
    override fun toString(spaces: Int, builder: StringBuilder) {
        builder.append("setlike<")
            .append(type)
            .append(">")
    }
}


// extended attributes
sealed class IdlExtendedAttributeDef: IdlDefinition() {
    abstract val name: String
}

data class IdlExtendedAttributeDefNoArgs(
    override val name: String
): IdlExtendedAttributeDef() {
    override fun toString(spaces: Int, builder: StringBuilder) {
        builder.append(name)
    }
}

data class IdlExtendedAttributeDefArgList(
    override val name: String,
    val args: List<IdlFieldDef>
): IdlExtendedAttributeDef() {
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

data class IdlExtendedAttributeDefNamedArgList(
    override val name: String,
    val identifier: String,
    val args: List<IdlFieldDef>
): IdlExtendedAttributeDef() {
    override fun toString(spaces: Int, builder: StringBuilder) {
        builder.append(name)
            .append("=")
            .append(identifier)
            .append("(")
        args.forEachIndexed { index, def ->
            def.toString(spaces, builder)
            if(index != args.lastIndex)
                builder.append(", ")
        }
        builder.append(")")
    }
}

data class IdlExtendedAttributeDefIdent(
    override val name: String,
    val identifier: String
): IdlExtendedAttributeDef() {
    override fun toString(spaces: Int, builder: StringBuilder) {
        builder.append(name)
            .append("=")
            .append(identifier)
    }
}

data class IdlExtendedAttributeDefString(
    override val name: String,
    val value: String
): IdlExtendedAttributeDef() {
    override fun toString(spaces: Int, builder: StringBuilder) {
        builder.append(name)
            .append("=")
            .append(value)
    }
}

data class IdlExtendedAttributeDefInteger(
    override val name: String,
    val value: Int
): IdlExtendedAttributeDef() {
    override fun toString(spaces: Int, builder: StringBuilder) {
        builder.append(name)
            .append("=")
            .append(value)
    }
}

data class IdlExtendedAttributeDefDecimal(
    override val name: String,
    val value: Double
): IdlExtendedAttributeDef() {
    override fun toString(spaces: Int, builder: StringBuilder) {
        builder.append(name)
            .append("=")
            .append(value)
    }
}

data class IdlExtendedAttributeDefIntegerList(
    override val name: String,
    val array: List<Int>
): IdlExtendedAttributeDef() {
    override fun toString(spaces: Int, builder: StringBuilder) {
        builder.append(name)
            .append("=(")
        array.joinTo(builder, ", ")
        builder.append(")")
    }
}

data class IdlExtendedAttributeDefIdentList(
    override val name: String,
    val array: List<String>
): IdlExtendedAttributeDef() {
    override fun toString(spaces: Int, builder: StringBuilder) {
        builder.append(name)
            .append("=(")
        array.joinTo(builder, ",")
        builder.append(")")
    }
}

data class IdlExtendedAttributeDefWildcard(
    override val name: String,
): IdlExtendedAttributeDef() {
    override fun toString(spaces: Int, builder: StringBuilder) {
        builder.append(name)
            .append("=*")
    }
}