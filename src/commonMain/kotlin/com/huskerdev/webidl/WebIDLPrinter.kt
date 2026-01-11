package com.huskerdev.webidl

import com.huskerdev.webidl.parser.*
import com.huskerdev.webidl.resolver.ResolvedIdlType

object WebIDLPrinter {

    private fun print(
        builder: IntentStringBuilder,
        definition: IdlDefinition,
        singleLine: Boolean = false
    ): IntentStringBuilder {
        definition.apply {
            when (this) {
                is IdlDefinitionRoot -> {
                    definitions.forEachIndexed { index, definition ->
                        if(index != 0)
                            builder.append("\n\n")
                        print(builder, definition)
                        builder.append(";")
                    }
                }

                is IdlInterface -> {
                    printAttributes(builder, attributes)
                    if (isPartial) builder.append("partial ")
                    if (isCallback) builder.append("callback ")
                    builder.append("interface ")
                    if (isMixin) builder.append("mixin ")
                    builder.append(name)
                    if (implements != null)
                        builder.append(": ").append(implements)
                    builder.append(" {")

                    val tabbed = builder.tab()
                    definitions.forEach { definition ->
                        tabbed.append("\n\n")
                        print(tabbed, definition)
                        tabbed.append(";")
                    }
                    builder.append("\n}")
                }

                is IdlNamespace -> {
                    printAttributes(builder, attributes)
                    if (isPartial) builder.append("partial ")
                    builder.append("namespace ").append(name).append(" {")

                    val tabbed = builder.tab()
                    definitions.forEach { definition ->
                        tabbed.append("\n\n")
                        print(tabbed, definition)
                        builder.append(";")
                    }
                    builder.append("\n}")
                }

                is IdlDictionary -> {
                    printAttributes(builder, attributes)
                    if (isPartial) builder.append("partial ")
                    builder.append("dictionary ")
                    builder.append(name)
                    if (implements != null) builder.append(": ").append(implements)
                    builder.append(" {")

                    val tabbed = builder.tab()
                    definitions.forEach { definition ->
                        tabbed.append("\n\n")
                        print(tabbed, definition)
                        tabbed.append(";")
                    }
                    builder.append("\n}")
                }

                is IdlCallbackFunction -> {
                    printAttributes(builder, attributes)
                    builder.append("callback ").append(name).append(" = ")
                    print(builder, operation)
                }

                is IdlTypeDef -> {
                    printAttributes(builder, attributes)
                    builder.append("typedef ")
                    printType(builder, type)
                    builder.append(' ').append(name)
                }

                is IdlEnum -> {
                    printAttributes(builder, attributes)
                    builder.append("enum ").append(name).append(" {")

                    val tabbed = builder.tab()
                    definitions.forEachIndexed { index, element ->
                        tabbed.append("\n")
                        print(tabbed, element)
                        if (index != definitions.lastIndex)
                            builder.append(',')
                    }
                    builder.append("\n}")
                }

                is IdlEnumElement ->
                    builder.append('\"').append(name).append('\"')

                is IdlIncludes ->
                    builder.append(target).append(" includes ").append(source)

                is IdlImplements ->
                    builder.append(target).append(" implements ").append(source)

                is IdlConstructor -> {
                    printAttributes(builder, attributes)
                    builder.append("constructor(")
                    args.forEachIndexed { index, def ->
                        print(builder, def, true)
                        if (index != args.lastIndex)
                            builder.append(", ")
                    }
                    builder.append(')')
                }

                is IdlOperation -> {
                    printAttributes(builder, attributes)
                    if(isStatic) builder.append("static ")
                    printType(builder, type)
                    if(name.isNotEmpty())
                        builder.append(' ').append(name)
                    builder.append('(')
                    args.forEachIndexed { index, def ->
                        print(builder, def, true)
                        if (index != args.lastIndex)
                            builder.append(", ")
                    }
                    builder.append(')')
                }

                is IdlField -> {
                    printAttributes(builder, attributes, !singleLine)
                    if (isStatic) builder.append("static ")
                    if (isReadOnly) builder.append("readonly ")
                    if (isInherit) builder.append("inherit ")
                    if (isOptional) builder.append("optional ")
                    if (isConst) builder.append("const ")
                    if (isAttribute) builder.append("attribute ")
                    if (isRequired) builder.append("required ")
                    printType(builder, type)
                    if (isVariadic)
                        builder.append("...")
                    builder.append(' ').append(name)
                    if (value != null) {
                        builder.append(" = ")
                        printValue(builder, value)
                    }
                }

                is IdlIterable -> {
                    builder.append("iterable<")
                    printType(builder, keyType)
                    if (valueType != null) {
                        builder.append(", ")
                        printType(builder, valueType)
                    }
                    builder.append('>')
                }

                is IdlAsyncIterableLike -> {
                    builder.append("async_iterable<")
                    printType(builder, keyType)
                    if (valueType != null) {
                        builder.append(", ")
                        printType(builder, valueType)
                    }
                    builder.append('>')
                }

                is IdlMapLike -> {
                    if (isReadOnly)
                        builder.append("readonly ")
                    builder.append("maplike<")
                    printType(builder, keyType)
                    builder.append(", ")
                    printType(builder, valueType)
                    builder.append('>')
                }

                is IdlSetLike -> {
                    if (isReadOnly)
                        builder.append("readonly ")
                    builder.append("setlike<")
                    printType(builder, type)
                    builder.append('>')
                }

                is IdlStringifier -> {
                    builder.append("stringifier")
                    if (field != null) {
                        builder.append(' ')
                        print(builder, field)
                    }
                }

                is IdlGetter -> {
                    builder.append("getter ")
                    print(builder, operation)
                }

                is IdlSetter -> {
                    builder.append("setter ")
                    print(builder, operation)
                }

                is IdlExtendedAttribute.NoArgs -> {
                    builder.append(name)
                }

                is IdlExtendedAttribute.ArgList -> {
                    builder.append(name).append('(')
                    args.forEachIndexed { index, def ->
                        print(builder, def)
                        if (index != args.lastIndex)
                            builder.append(", ")
                    }
                    builder.append(')')
                }

                is IdlExtendedAttribute.NamedArgList -> {
                    builder.append(name).append('=')
                    builder.append(identifier).append('(')
                    args.forEachIndexed { index, def ->
                        print(builder, def)
                        if (index != args.lastIndex)
                            builder.append(", ")
                    }
                    builder.append(')')
                }

                is IdlExtendedAttribute.IdentifierValue ->
                    builder.append(name).append('=').append(identifier)

                is IdlExtendedAttribute.StringValue ->
                    builder.append(name).append('=').append(value)

                is IdlExtendedAttribute.IntegerValue ->
                    builder.append(name).append('=').append(value.toString())

                is IdlExtendedAttribute.DecimalValue ->
                    builder.append(name).append('=').append(value.toString())

                is IdlExtendedAttribute.IntegerList -> {
                    builder.append(name).append("=(")
                    array.joinTo(builder, prefix = "(", separator = ",", postfix = ")")
                }

                is IdlExtendedAttribute.IdentifierList -> {
                    builder.append(name).append('=')
                    array.joinTo(builder, prefix = "(", separator = ",", postfix = ")")
                }

                is IdlExtendedAttribute.Wildcard ->
                    builder.append(name).append("=*")

                else -> throw UnsupportedOperationException()
            }
        }
        return builder
    }

    private fun printValue(builder: IntentStringBuilder, value: IdlValue) {
        builder.append(when(value) {
            IdlValue.DictionaryInitValue -> "{}"
            IdlValue.NullValue -> "null"
            is IdlValue.BooleanValue -> value.boolValue.toString()
            is IdlValue.DecimalValue -> value.text
            is IdlValue.IntValue -> value.text
            is IdlValue.StringValue -> "\"${value.text}\""
        })
    }

    private fun printType(builder: Appendable, type: IdlType) {
        val nullableChar = if(type.nullable) "?" else ""
        when(type) {
            is IdlType.Default -> {
                builder.append(type.name)
                if(type.parameters.isNotEmpty()) {
                    builder.append("<")
                    type.parameters.forEachIndexed { index, it ->
                        printType(builder, it)
                        if(index != type.parameters.lastIndex)
                            builder.append(", ")
                    }
                    builder.append(">")
                }
            }
            is IdlType.Union -> {
                builder.append('(')
                type.types.forEachIndexed { index, it ->
                    printType(builder, it)
                    if(index != type.types.lastIndex)
                        builder.append(" or ")
                }
                builder.append(')')
            }
        }
        builder.append(nullableChar)
    }

    private fun printResolvedType(builder: Appendable, type: ResolvedIdlType) {
        val nullableChar = if(type.isNullable) "?" else ""
        when(type) {
            is ResolvedIdlType.Default -> {
                builder.append(type.declaration.name)
                if(type.parameters.isNotEmpty()) {
                    builder.append("<")
                    type.parameters.forEachIndexed { index, it ->
                        printResolvedType(builder, it)
                        if(index != type.parameters.lastIndex)
                            builder.append(", ")
                    }
                    builder.append(">")
                }
            }
            is ResolvedIdlType.Union -> {
                builder.append('(')
                type.types.forEachIndexed { index, it ->
                    printResolvedType(builder, it)
                    if(index != type.types.lastIndex)
                        builder.append(" or ")
                }
                builder.append(')')
            }
        }
        builder.append(nullableChar)
    }

    private fun printAttributes(
        builder: IntentStringBuilder,
        attributes: List<IdlExtendedAttribute>,
        newLine: Boolean = true,
    ) {
        if (attributes.isEmpty())
            return
        builder.append("[")
        attributes.forEachIndexed { index, def ->
            print(builder, def)
            if (index != attributes.lastIndex)
                builder.append(", ")
        }
        builder.append("]")
        if (newLine)
            builder.append('\n')
        else builder.append(" ")
    }


    @Suppress("unused")
    fun print(definition: IdlDefinition, spaces: Int = 2) =
        print(IntentStringBuilder(" ".repeat(spaces)), definition).toString()

    @Suppress("unused")
    fun printValue(value: IdlValue) = IntentStringBuilder().apply {
        printValue(this, value)
    }.toString()

    @Suppress("unused")
    fun printType(type: IdlType) = IntentStringBuilder().apply {
        printType(this, type)
    }.toString()

    @Suppress("unused")
    fun printResolvedType(type: ResolvedIdlType) = IntentStringBuilder().apply {
        printResolvedType(this, type)
    }.toString()

    @Suppress("unused")
    fun printAttributes(
        attributes: List<IdlExtendedAttribute>,
        newLine: Boolean = true,
    ) = IntentStringBuilder().apply {
        printAttributes(this, attributes, newLine)
    }.toString()

}

private class IntentStringBuilder(
    val spacing: String = "",
    val tab: Int = 0,
    private val builder: StringBuilder = StringBuilder()
): Appendable {
    private val fullSpacing = spacing.repeat(tab)

    fun tab() = IntentStringBuilder(spacing, tab + 1, builder)

    override fun append(value: Char): IntentStringBuilder {
        builder.append(value)
        if(value == '\n')
            builder.append(fullSpacing)
        return this
    }

    override fun append(value: CharSequence?): IntentStringBuilder {
        if(value != null)
            builder.append(value.toString().replace("\n", "\n$fullSpacing"))
        return this
    }

    override fun append(
        value: CharSequence?,
        startIndex: Int,
        endIndex: Int
    ): IntentStringBuilder {
        if(value != null)
            builder.append(value.replace("\n".toRegex(), "\n$fullSpacing"), startIndex, endIndex)
        return this
    }

    override fun toString() =
        builder.toString()
}