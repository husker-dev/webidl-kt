package com.huskerdev.webidl.resolved

sealed interface WebIDLType {
    val nullable: Boolean
}

class WebIDLReferenceType(
    val ref: WebIDLDeclaration,
    override val nullable: Boolean,
): WebIDLType

class WebIDLUnionType(
    val types: List<WebIDLDeclaration>,
    override val nullable: Boolean
): WebIDLType