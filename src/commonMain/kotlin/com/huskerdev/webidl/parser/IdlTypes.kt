package com.huskerdev.webidl.parser

sealed interface IdlType {
    val nullable: Boolean

    data class Default(
        val name: String,
        override val nullable: Boolean = false,
        val types: List<IdlType> = emptyList(),
    ): IdlType

    data class Union(
        val types: List<IdlType>,
        override val nullable: Boolean,
    ): IdlType
}








