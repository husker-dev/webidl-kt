package com.huskerdev.webidl.parser

sealed class WebIDLType {
    var nullable = false

    protected val nullableChar
        get() = if(nullable) "?" else ""
}

class WebIDLIdentifierType(val name: String): WebIDLType() {
    override fun toString() = "$name$nullableChar"
}

class WebIDLUnionType(val types: List<WebIDLType>): WebIDLType() {
    override fun toString() = "(${types.joinToString(" or ")})$nullableChar"
}

class WebIDLGenericType(val name: String, val types: List<WebIDLType>): WebIDLType() {
    override fun toString() = "$name<${types.joinToString(", ")}>$nullableChar"
}

class WebIDLDefaultType(val name: String): WebIDLType() {
    override fun toString() = "$name$nullableChar"
}