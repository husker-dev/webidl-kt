package com.huskerdev.webidl.resolved

sealed interface WebIDLMember

sealed interface WebIDLField: WebIDLMember {
    val name: String
    val type: WebIDLType
    val value: Any?
}

open class WebIDLDeclarationField(
    override val name: String,
    override val type: WebIDLType,
    override val value: Any?,
    val isConst: Boolean,
    val isReadonly: Boolean,
    val isStatic: Boolean,
    val isInherit: Boolean,
): WebIDLField

open class WebIDLArgField(
    override val name: String,
    override val type: WebIDLType,
    override val value: Any?,
    val isOptional: Boolean,
    val isVariadic: Boolean,
): WebIDLField


class WebIDLFunction(
    val name: String,
    val type: WebIDLType,
    val args: List<WebIDLField>,
    val isStatic: Boolean,
): WebIDLMember


class WebIDLConstructor(
    val args: List<WebIDLField>,
): WebIDLMember