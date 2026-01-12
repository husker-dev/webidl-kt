package com.huskerdev.webidl.parser

sealed interface IdlType {
    val isNullable: Boolean

    data class Default(
        val name: String,
        override val isNullable: Boolean = false,
        val parameters: List<IdlType> = emptyList(),
    ): IdlType

    data class Union(
        val types: List<IdlType>,
        override val isNullable: Boolean,
    ): IdlType
}








