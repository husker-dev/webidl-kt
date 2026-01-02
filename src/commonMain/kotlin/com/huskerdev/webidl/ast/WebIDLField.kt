package com.huskerdev.webidl.ast



sealed interface WebIDLField {
    val name: String
    val type: WebIDLDeclaration
    val nullable: Boolean
    val value: Any
}

open class WebIDLDeclarationField(
    override val name: String,
    override val type: WebIDLDeclaration,
    override val nullable: Boolean,
    override val value: Any,
    val isConst: Boolean,
    val isReadonly: Boolean,
    val isStatic: Boolean,
    val isInherit: Boolean,
): WebIDLField

open class WebIDLArgField(
    override val name: String,
    override val type: WebIDLDeclaration,
    override val nullable: Boolean,
    override val value: Any,
    val isOptional: Boolean,
    val isVariadic: Boolean,
): WebIDLField
