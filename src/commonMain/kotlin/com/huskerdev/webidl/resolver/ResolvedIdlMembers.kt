package com.huskerdev.webidl.resolver

import com.huskerdev.webidl.parser.IdlAttributedHolder
import com.huskerdev.webidl.parser.IdlExtendedAttribute
import com.huskerdev.webidl.parser.IdlValue
import kotlinx.serialization.Serializable

@Serializable
sealed interface ResolvedIdlMember: IdlAttributedHolder

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
        override val attributes: List<IdlExtendedAttribute>,
    ): ResolvedIdlField

    @Serializable
    open class Argument(
        override val name: String,
        override val type: ResolvedIdlType,
        override val value: IdlValue?,
        val isOptional: Boolean,
        val isVariadic: Boolean,
        override val attributes: List<IdlExtendedAttribute>,
    ): ResolvedIdlField
}

@Serializable
class ResolvedIdlOperation(
    val name: String,
    val type: ResolvedIdlType,
    val args: List<ResolvedIdlField.Argument>,
    val isStatic: Boolean,
    override val attributes: List<IdlExtendedAttribute>,
): ResolvedIdlMember

@Serializable
class ResolvedIdlConstructor(
    val args: List<ResolvedIdlField.Argument>,
    override val attributes: List<IdlExtendedAttribute>,
): ResolvedIdlMember