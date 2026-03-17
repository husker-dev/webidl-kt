package com.huskerdev.webidl.parser

import kotlinx.serialization.Serializable

@Serializable
sealed interface IdlType {
    val isNullable: Boolean

    @Serializable
    data class Default(
        val name: String,
        override val isNullable: Boolean = false,
        val parameters: List<IdlType> = emptyList(),
    ): IdlType

    @Serializable
    data class Union(
        val types: List<IdlType>,
        override val isNullable: Boolean,
    ): IdlType
}








