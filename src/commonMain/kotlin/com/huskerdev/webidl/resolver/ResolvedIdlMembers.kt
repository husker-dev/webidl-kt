package com.huskerdev.webidl.resolver

import com.huskerdev.webidl.IdlElementBounds
import com.huskerdev.webidl.parser.IdlAttributeHolder
import com.huskerdev.webidl.parser.IdlAttributes
import com.huskerdev.webidl.parser.IdlValue
import kotlinx.serialization.Serializable

@Serializable
sealed interface ResolvedIdlMember: IdlAttributeHolder {
    val bounds: IdlElementBounds
}

@Serializable
sealed interface ResolvedIdlField: ResolvedIdlMember {
    val name: String
    val type: ResolvedIdlType
    val value: IdlValue?

    @Serializable
    open class Declaration(
        override val name: String,
        override val type: ResolvedIdlType,
        override val value: IdlValue?,
        val isAttribute: Boolean,
        val isStatic: Boolean,
        val isReadOnly: Boolean,
        val isInherit: Boolean,
        val isConst: Boolean,
        val isRequired: Boolean,
        override val attributes: IdlAttributes?,
        override val bounds: IdlElementBounds = IdlElementBounds()
    ): ResolvedIdlField

    @Serializable
    open class Argument(
        override val name: String,
        override val type: ResolvedIdlType,
        override val value: IdlValue?,
        val isOptional: Boolean,
        val isVariadic: Boolean,
        override val attributes: IdlAttributes?,
        override val bounds: IdlElementBounds = IdlElementBounds()
    ): ResolvedIdlField
}

@Serializable
class ResolvedIdlOperation(
    val name: String,
    val type: ResolvedIdlType,
    val args: List<ResolvedIdlField.Argument>,
    val isStatic: Boolean,
    override val attributes: IdlAttributes?,
    override val bounds: IdlElementBounds = IdlElementBounds()
): ResolvedIdlMember

@Serializable
class ResolvedIdlConstructor(
    val args: List<ResolvedIdlField.Argument>,
    override val attributes: IdlAttributes?,
    override val bounds: IdlElementBounds = IdlElementBounds()
): ResolvedIdlMember