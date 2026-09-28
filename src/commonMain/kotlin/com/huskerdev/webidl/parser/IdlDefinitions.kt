package com.huskerdev.webidl.parser

import com.huskerdev.webidl.IdlElementBounds
import com.huskerdev.webidl.LOCATION_NOT_SPECIFIED
import com.huskerdev.webidl.lexer.WebIDLLexer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


@Serializable
sealed interface IdlDefinition {
    var bounds: IdlElementBounds
    val children: List<IdlDefinition>
}

sealed interface IdlContainer: IdlDefinition {
    val definitions: MutableList<IdlDefinition>
}

interface IdlAttributeHolder {
    val attributes: IdlAttributes?
}

// Types

@Serializable
sealed interface IdlType: IdlDefinition {
    val isNullable: Boolean

    @Serializable
    data class Default(
        val name: IdlName,
        override val isNullable: Boolean = false,
        val parameters: List<IdlType> = emptyList(),
        override var bounds: IdlElementBounds = IdlElementBounds(),
    ): IdlType {
        override val children = listOf(name, *parameters.toTypedArray())
    }

    @Serializable
    data class Union(
        val types: List<IdlType>,
        override val isNullable: Boolean = false,
        override var bounds: IdlElementBounds = IdlElementBounds(),
    ): IdlType {
        override val children = types
    }
}

// Implementations

@Serializable
data class IdlName(
    val text: String,
    val isReference: Boolean = false,
    val isBuiltin: Boolean = false,
    override var bounds: IdlElementBounds = IdlElementBounds(),
): IdlDefinition {
    override val children = emptyList<IdlDefinition>()

    constructor(
        lexeme: WebIDLLexer.Lexeme,
        isReference: Boolean = lexeme.type == WebIDLLexer.LexemeType.IDENTIFIER,
        isBuiltin: Boolean = lexeme.type == WebIDLLexer.LexemeType.TYPE
    ): this(
        text = lexeme.content,
        isReference = isReference,
        isBuiltin = isBuiltin,
        bounds = lexeme.bounds,
    )
}

@Serializable
data class IdlAttributes(
    val list: List<IdlExtendedAttribute> = mutableListOf(),
    override var bounds: IdlElementBounds = IdlElementBounds(),
): IdlDefinition, List<IdlExtendedAttribute> by list {
    override val children = list
}

@Serializable
data class IdlRoot(
    override val definitions: MutableList<IdlDefinition> = mutableListOf(),
    override var bounds: IdlElementBounds = IdlElementBounds(0, LOCATION_NOT_SPECIFIED, 0, 0)
): IdlContainer {
    override val children = definitions
}

@Serializable
data class IdlInterface(
    val name: IdlName,
    val isPartial: Boolean = false,
    val isMixin: Boolean = false,
    val isCallback: Boolean = false,
    val implements: IdlName? = null,
    override val attributes: IdlAttributes? = null,
    override val definitions: MutableList<IdlDefinition> = mutableListOf(),
    override var bounds: IdlElementBounds = IdlElementBounds()
): IdlContainer, IdlAttributeHolder {
    override val children: List<IdlDefinition>
        get() = listOfNotNull(attributes, name, implements, *definitions.toTypedArray())
}

@Serializable
data class IdlNamespace(
    val name: IdlName,
    val isPartial: Boolean = false,
    override val attributes: IdlAttributes? = null,
    override val definitions: MutableList<IdlDefinition> = mutableListOf(),
    override var bounds: IdlElementBounds = IdlElementBounds()
): IdlContainer, IdlAttributeHolder {
    override val children: List<IdlDefinition>
        get() = listOfNotNull(attributes, name, *definitions.toTypedArray())
}

@Serializable
data class IdlDictionary(
    val name: IdlName,
    val implements: IdlName? = null,
    val isPartial: Boolean = false,
    override val attributes: IdlAttributes? = null,
    override val definitions: MutableList<IdlDefinition> = mutableListOf(),
    override var bounds: IdlElementBounds = IdlElementBounds()
): IdlContainer, IdlAttributeHolder {
    override val children: List<IdlDefinition>
        get() = listOfNotNull(attributes, name, implements, *definitions.toTypedArray())
}

@Serializable
data class IdlCallbackFunction(
    val name: IdlName,
    val operation: IdlOperation,
    override val attributes: IdlAttributes? = null,
    override var bounds: IdlElementBounds = IdlElementBounds()
): IdlDefinition, IdlAttributeHolder {
    override val children = listOfNotNull(attributes, name, operation)
}

@Serializable
data class IdlTypeDef(
    val name: IdlName,
    @SerialName("_type")
    val type: IdlType,
    override val attributes: IdlAttributes? = null,
    override var bounds: IdlElementBounds = IdlElementBounds()
): IdlDefinition, IdlAttributeHolder {
    override val children = listOfNotNull(attributes, type, name)
}

@Serializable
data class IdlEnum(
    val name: IdlName,
    override val attributes: IdlAttributes? = null,
    val elements: MutableList<IdlEnumElement> = mutableListOf(),
    override var bounds: IdlElementBounds = IdlElementBounds()
): IdlDefinition, IdlAttributeHolder {
    override val children: List<IdlDefinition>
        get() = listOfNotNull(attributes, name, *elements.toTypedArray())
}

@Serializable
data class IdlEnumElement(
    val name: IdlName,
    override var bounds: IdlElementBounds = IdlElementBounds()
): IdlDefinition {
    override val children = listOf(name)
}

@Serializable
data class IdlIncludes(
    val target: IdlName,
    val source: IdlName,
    override var bounds: IdlElementBounds = IdlElementBounds()
): IdlDefinition {
    override val children = listOf(target, source)
}

@Serializable
data class IdlImplements(
    val target: IdlName,
    val source: IdlName,
    override var bounds: IdlElementBounds = IdlElementBounds()
): IdlDefinition {
    override val children = listOf(target, source)
}

@Serializable
data class IdlConstructor(
    val args: List<IdlField>,
    val header: IdlName = IdlName("constructor"),
    override val attributes: IdlAttributes? = null,
    override var bounds: IdlElementBounds = IdlElementBounds(),
): IdlDefinition, IdlAttributeHolder {
    override val children = listOfNotNull(attributes, header, *args.toTypedArray())
}

@Serializable
data class IdlOperation(
    val name: IdlName,
    @SerialName("_type")
    val type: IdlType,
    val args: List<IdlField> = mutableListOf(),
    val isStatic: Boolean = false,
    override val attributes: IdlAttributes? = null,
    override var bounds: IdlElementBounds = IdlElementBounds()
): IdlDefinition, IdlAttributeHolder {
    override val children = listOfNotNull(attributes, type, name, *args.toTypedArray())
}

@Serializable
data class IdlField(
    val name: IdlName,
    @SerialName("_type")
    val type: IdlType,
    val value: IdlValue? = null,
    val isOperationArgument: Boolean = false,
    val isAttribute: Boolean = false,
    val isStatic: Boolean = false,
    val isReadOnly: Boolean = false,
    val isInherit: Boolean = false,
    val isOptional: Boolean = false,
    val isConst: Boolean = false,
    val isVariadic: Boolean = false,
    val isRequired: Boolean = false,
    override val attributes: IdlAttributes? = null,
    override var bounds: IdlElementBounds = IdlElementBounds()
): IdlDefinition, IdlAttributeHolder {
    override val children = listOfNotNull(attributes, type, name)
}

@Serializable
data class IdlIterable(
    val keyType: IdlType,
    val valueType: IdlType? = null,
    override var bounds: IdlElementBounds = IdlElementBounds()
): IdlDefinition {
    override val children = listOfNotNull(keyType, valueType)
}

@Serializable
data class IdlAsyncIterableLike(
    val keyType: IdlType,
    val valueType: IdlType? = null,
    override var bounds: IdlElementBounds = IdlElementBounds()
): IdlDefinition {
    override val children = listOfNotNull(keyType, valueType)
}

@Serializable
data class IdlMapLike(
    val keyType: IdlType,
    val valueType: IdlType,
    val isReadOnly: Boolean = false,
    override var bounds: IdlElementBounds = IdlElementBounds()
): IdlDefinition {
    override val children = listOf(keyType, valueType)
}

@Serializable
data class IdlSetLike(
    @SerialName("_type")
    val type: IdlType,
    val isReadOnly: Boolean,
    override var bounds: IdlElementBounds = IdlElementBounds()
): IdlDefinition {
    override val children = listOf(type)
}

@Serializable
data class IdlStringifier(
    val field: IdlField? = null,
    override var bounds: IdlElementBounds = IdlElementBounds()
): IdlDefinition {
    override val children = listOfNotNull(field)
}

@Serializable
data class IdlGetter(
    val operation: IdlOperation,
    override var bounds: IdlElementBounds = IdlElementBounds()
): IdlDefinition {
    override val children = listOf(operation)
}

@Serializable
data class IdlSetter(
    val operation: IdlOperation,
    override var bounds: IdlElementBounds = IdlElementBounds()
): IdlDefinition {
    override val children = listOf(operation)
}

@Serializable
sealed interface IdlExtendedAttribute: IdlDefinition {
    val name: IdlName

    /** `[Replaceable]` */
    @Serializable
    data class NoArgs(
        override val name: IdlName,
        override var bounds: IdlElementBounds = IdlElementBounds()
    ): IdlExtendedAttribute {
        override val children = listOf(name)
    }

    /** `[Constructor(double x, double y)]` */
    @Serializable
    data class ArgList(
        override val name: IdlName,
        val args: List<IdlField>,
        override var bounds: IdlElementBounds = IdlElementBounds()
    ): IdlExtendedAttribute {
        override val children = listOf(name, *args.toTypedArray())
    }

    /** `[LegacyFactoryFunction=Image(DOMString src)]` */
    @Serializable
    data class NamedArgList(
        override val name: IdlName,
        val identifier: IdlName,
        val args: List<IdlField>,
        override var bounds: IdlElementBounds = IdlElementBounds()
    ): IdlExtendedAttribute {
        override val children = listOf(name, identifier, *args.toTypedArray())
    }

    /** `[PutForwards=name]` */
    @Serializable
    data class IdentifierValue(
        override val name: IdlName,
        val identifier: IdlName,
        override var bounds: IdlElementBounds = IdlElementBounds()
    ): IdlExtendedAttribute {
        override val children = listOf(name, identifier)
    }

    /** `[Reflect="popover"]` */
    @Serializable
    data class StringValue(
        override val name: IdlName,
        val value: String,
        override var bounds: IdlElementBounds = IdlElementBounds()
    ): IdlExtendedAttribute {
        override val children = listOf(name)
    }

    /** `[ReflectDefault=2]` */
    @Serializable
    data class IntegerValue(
        override val name: IdlName,
        val value: Int,
        override var bounds: IdlElementBounds = IdlElementBounds()
    ): IdlExtendedAttribute {
        override val children = listOf(name)
    }

    /** `[ReflectDefault=2.0]` */
    @Serializable
    data class DecimalValue(
        override val name: IdlName,
        val value: Double,
        override var bounds: IdlElementBounds = IdlElementBounds()
    ): IdlExtendedAttribute {
        override val children = listOf(name)
    }

    /** `[ReflectRange=(2, 600)]` */
    @Serializable
    data class IntegerList(
        override val name: IdlName,
        val array: List<Int>,
        override var bounds: IdlElementBounds = IdlElementBounds()
    ): IdlExtendedAttribute {
        override val children = listOf(name)
    }

    /** `[Exposed=(Window,Worker)]` */
    @Serializable
    data class IdentifierList(
        override val name: IdlName,
        val identifiers: List<IdlName>,
        override var bounds: IdlElementBounds = IdlElementBounds()
    ): IdlExtendedAttribute {
        override val children = listOf(name, *identifiers.toTypedArray())
    }

    /** `[Exposed=*]` */
    @Serializable
    data class Wildcard(
        override val name: IdlName,
        override var bounds: IdlElementBounds = IdlElementBounds()
    ): IdlExtendedAttribute {
        override val children = listOf(name)
    }
}