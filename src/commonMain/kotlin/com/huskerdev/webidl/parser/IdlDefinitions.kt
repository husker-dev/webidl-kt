package com.huskerdev.webidl.parser


sealed interface IdlDefinition

sealed interface IdlDefinitionContainer<T: IdlDefinition>: IdlDefinition {
    val definitions: ArrayList<T>
}

sealed interface IdlDefaultDefinitionContainer: IdlDefinitionContainer<IdlDefinition>


// Implementations

data class IdlDefinitionRoot(
    override val definitions: ArrayList<IdlDefinition> = arrayListOf()
): IdlDefaultDefinitionContainer

data class IdlInterface(
    val name: String,
    val isPartial: Boolean = false,
    val isMixin: Boolean = false,
    val isCallback: Boolean = false,
    val implements: String? = null,
    override val attributes: List<IdlExtendedAttribute> = emptyList(),
    override val definitions: ArrayList<IdlDefinition> = arrayListOf()
): IdlDefaultDefinitionContainer, IdlAttributedHolder

data class IdlNamespace(
    val name: String,
    val isPartial: Boolean = false,
    override val attributes: List<IdlExtendedAttribute> = emptyList(),
    override val definitions: ArrayList<IdlDefinition> = arrayListOf()
): IdlDefaultDefinitionContainer, IdlAttributedHolder

data class IdlDictionary(
    val name: String,
    val implements: String? = null,
    val isPartial: Boolean = false,
    override val attributes: List<IdlExtendedAttribute> = emptyList(),
    override val definitions: ArrayList<IdlDefinition> = arrayListOf()
): IdlDefaultDefinitionContainer, IdlAttributedHolder

data class IdlCallbackFunction(
    val name: String,
    val operation: IdlOperation,
    override val attributes: List<IdlExtendedAttribute> = emptyList()
): IdlDefinition, IdlAttributedHolder

data class IdlTypeDef(
    val name: String,
    val type: IdlType,
    override val attributes: List<IdlExtendedAttribute> = emptyList()
): IdlDefinition, IdlAttributedHolder

data class IdlEnum(
    val name: String,
    override val attributes: List<IdlExtendedAttribute> = emptyList(),
    override val definitions: ArrayList<IdlEnumElement> = arrayListOf()
): IdlDefinitionContainer<IdlEnumElement>, IdlAttributedHolder

data class IdlEnumElement(
    val name: String
): IdlDefinition

data class IdlIncludes(
    val target: String,
    val source: String
): IdlDefinition

data class IdlImplements(
    val target: String,
    val source: String
): IdlDefinition

data class IdlConstructor(
    val args: List<IdlField>,
    override val attributes: List<IdlExtendedAttribute>
): IdlDefinition, IdlAttributedHolder

data class IdlOperation(
    val name: String,
    val type: IdlType,
    val args: List<IdlField> = emptyList(),
    val isStatic: Boolean = false,
    override val attributes: List<IdlExtendedAttribute> = emptyList(),
): IdlDefinition, IdlAttributedHolder

data class IdlField(
    val name: String,
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

data class IdlIterable(
    val keyType: IdlType,
    val valueType: IdlType? = null
): IdlDefinition

data class IdlAsyncIterableLike(
    val keyType: IdlType,
    val valueType: IdlType? = null
): IdlDefinition

data class IdlMapLike(
    val keyType: IdlType,
    val valueType: IdlType,
    val isReadOnly: Boolean = false
): IdlDefinition

data class IdlSetLike(
    val type: IdlType,
    val isReadOnly: Boolean
): IdlDefinition

data class IdlStringifier(
     val field: IdlField? = null
): IdlDefinition

data class IdlGetter(
    val operation: IdlOperation
): IdlDefinition

data class IdlSetter(
    val operation: IdlOperation
): IdlDefinition

sealed interface IdlExtendedAttribute: IdlDefinition {
    val name: String

    data class NoArgs(
        override val name: String
    ): IdlExtendedAttribute

    data class ArgList(
        override val name: String,
        val args: List<IdlField>
    ): IdlExtendedAttribute

    data class NamedArgList(
        override val name: String,
        val identifier: String,
        val args: List<IdlField>
    ): IdlExtendedAttribute

    data class IdentifierValue(
        override val name: String,
        val identifier: String
    ): IdlExtendedAttribute

    data class StringValue(
        override val name: String,
        val value: String
    ): IdlExtendedAttribute

    data class IntegerValue(
        override val name: String,
        val value: Int
    ): IdlExtendedAttribute

    data class DecimalValue(
        override val name: String,
        val value: Double
    ): IdlExtendedAttribute

    data class IntegerList(
        override val name: String,
        val array: List<Int>
    ): IdlExtendedAttribute

    data class IdentifierList(
        override val name: String,
        val array: List<String>
    ): IdlExtendedAttribute

    data class Wildcard(
        override val name: String,
    ): IdlExtendedAttribute
}