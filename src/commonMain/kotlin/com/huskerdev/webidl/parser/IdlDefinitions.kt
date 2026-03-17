package com.huskerdev.webidl.parser

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


@Serializable
sealed interface IdlDefinition

@Serializable
sealed interface IdlDefinitionContainer<T: IdlDefinition>: IdlDefinition {
    val definitions: ArrayList<T>
}

@Serializable
sealed interface IdlDefaultDefinitionContainer: IdlDefinitionContainer<IdlDefinition>


// Implementations

@Serializable
data class IdlDefinitionRoot(
    override val definitions: ArrayList<IdlDefinition> = arrayListOf()
): IdlDefaultDefinitionContainer

@Serializable
data class IdlInterface(
    val name: String,
    val isPartial: Boolean = false,
    val isMixin: Boolean = false,
    val isCallback: Boolean = false,
    val implements: String? = null,
    override val attributes: List<IdlExtendedAttribute> = emptyList(),
    override val definitions: ArrayList<IdlDefinition> = arrayListOf()
): IdlDefaultDefinitionContainer, IdlAttributedHolder

@Serializable
data class IdlNamespace(
    val name: String,
    val isPartial: Boolean = false,
    override val attributes: List<IdlExtendedAttribute> = emptyList(),
    override val definitions: ArrayList<IdlDefinition> = arrayListOf()
): IdlDefaultDefinitionContainer, IdlAttributedHolder

@Serializable
data class IdlDictionary(
    val name: String,
    val implements: String? = null,
    val isPartial: Boolean = false,
    override val attributes: List<IdlExtendedAttribute> = emptyList(),
    override val definitions: ArrayList<IdlDefinition> = arrayListOf()
): IdlDefaultDefinitionContainer, IdlAttributedHolder

@Serializable
data class IdlCallbackFunction(
    val name: String,
    val operation: IdlOperation,
    override val attributes: List<IdlExtendedAttribute> = emptyList()
): IdlDefinition, IdlAttributedHolder

@Serializable
data class IdlTypeDef(
    val name: String,
    @SerialName("_type")
    val type: IdlType,
    override val attributes: List<IdlExtendedAttribute> = emptyList()
): IdlDefinition, IdlAttributedHolder

@Serializable
data class IdlEnum(
    val name: String,
    override val attributes: List<IdlExtendedAttribute> = emptyList(),
    override val definitions: ArrayList<IdlEnumElement> = arrayListOf()
): IdlDefinitionContainer<IdlEnumElement>, IdlAttributedHolder

@Serializable
data class IdlEnumElement(
    val name: String
): IdlDefinition

@Serializable
data class IdlIncludes(
    val target: String,
    val source: String
): IdlDefinition

@Serializable
data class IdlImplements(
    val target: String,
    val source: String
): IdlDefinition

@Serializable
data class IdlConstructor(
    val args: List<IdlField>,
    override val attributes: List<IdlExtendedAttribute>
): IdlDefinition, IdlAttributedHolder

@Serializable
data class IdlOperation(
    val name: String,
    @SerialName("_type")
    val type: IdlType,
    val args: List<IdlField> = emptyList(),
    val isStatic: Boolean = false,
    override val attributes: List<IdlExtendedAttribute> = emptyList(),
): IdlDefinition, IdlAttributedHolder

@Serializable
data class IdlField(
    val name: String,
    @SerialName("_type")
    val type: IdlType,
    val value: IdlValue? = null,
    val isAttribute: Boolean = false,
    val isStatic: Boolean = false,
    val isReadOnly: Boolean = false,
    val isInherit: Boolean = false,
    val isOptional: Boolean = false,
    val isConst: Boolean = false,
    val isVariadic: Boolean = false,
    val isRequired: Boolean = false,
    override val attributes: List<IdlExtendedAttribute> = emptyList()
): IdlDefinition, IdlAttributedHolder

@Serializable
data class IdlIterable(
    val keyType: IdlType,
    val valueType: IdlType? = null
): IdlDefinition

@Serializable
data class IdlAsyncIterableLike(
    val keyType: IdlType,
    val valueType: IdlType? = null
): IdlDefinition

@Serializable
data class IdlMapLike(
    val keyType: IdlType,
    val valueType: IdlType,
    val isReadOnly: Boolean = false
): IdlDefinition

@Serializable
data class IdlSetLike(
    @SerialName("_type")
    val type: IdlType,
    val isReadOnly: Boolean
): IdlDefinition

@Serializable
data class IdlStringifier(
     val field: IdlField? = null
): IdlDefinition

@Serializable
data class IdlGetter(
    val operation: IdlOperation
): IdlDefinition

@Serializable
data class IdlSetter(
    val operation: IdlOperation
): IdlDefinition

@Serializable
sealed interface IdlExtendedAttribute: IdlDefinition {
    val name: String

    @Serializable
    data class NoArgs(
        override val name: String
    ): IdlExtendedAttribute

    @Serializable
    data class ArgList(
        override val name: String,
        val args: List<IdlField>
    ): IdlExtendedAttribute

    @Serializable
    data class NamedArgList(
        override val name: String,
        val identifier: String,
        val args: List<IdlField>
    ): IdlExtendedAttribute

    @Serializable
    data class IdentifierValue(
        override val name: String,
        val identifier: String
    ): IdlExtendedAttribute

    @Serializable
    data class StringValue(
        override val name: String,
        val value: String
    ): IdlExtendedAttribute

    @Serializable
    data class IntegerValue(
        override val name: String,
        val value: Int
    ): IdlExtendedAttribute

    @Serializable
    data class DecimalValue(
        override val name: String,
        val value: Double
    ): IdlExtendedAttribute

    @Serializable
    data class IntegerList(
        override val name: String,
        val array: List<Int>
    ): IdlExtendedAttribute

    @Serializable
    data class IdentifierList(
        override val name: String,
        val array: List<String>
    ): IdlExtendedAttribute

    @Serializable
    data class Wildcard(
        override val name: String,
    ): IdlExtendedAttribute
}