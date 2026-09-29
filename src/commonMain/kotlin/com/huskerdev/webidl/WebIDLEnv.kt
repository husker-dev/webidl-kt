package com.huskerdev.webidl

import com.huskerdev.webidl.resolver.WebIDLBuiltinKind

@Suppress("unused")
interface WebIDLEnv {
    val builtinTypes: Map<String, WebIDLBuiltinKind>

    val overloadingSupported: Boolean
    val emptyEnumSupported: Boolean

    object Default: WebIDLEnv {
        override val builtinTypes = mapOf(
            "void"                 to WebIDLBuiltinKind.VOID,
            "any"                  to WebIDLBuiltinKind.ANY,
            "undefined"            to WebIDLBuiltinKind.VOID,
            "sequence"             to WebIDLBuiltinKind.MUTABLE_LIST,
            "FrozenArray"          to WebIDLBuiltinKind.LIST,
            "record"               to WebIDLBuiltinKind.MAP,
            "Promise"              to WebIDLBuiltinKind.PROMISE,
            "boolean"              to WebIDLBuiltinKind.BOOLEAN,
            "byte"                 to WebIDLBuiltinKind.BYTE,
            "octet"                to WebIDLBuiltinKind.UNSIGNED_BYTE,
            "short"                to WebIDLBuiltinKind.SHORT,
            "unsigned short"       to WebIDLBuiltinKind.UNSIGNED_SHORT,
            "long"                 to WebIDLBuiltinKind.INT,
            "unsigned long"        to WebIDLBuiltinKind.UNSIGNED_INT,
            "long long"            to WebIDLBuiltinKind.LONG,
            "unsigned long long"   to WebIDLBuiltinKind.UNSIGNED_LONG,
            "float"                to WebIDLBuiltinKind.FLOAT,
            "unrestricted float"   to WebIDLBuiltinKind.UNRESTRICTED_FLOAT,
            "double"               to WebIDLBuiltinKind.DOUBLE,
            "unrestricted double"  to WebIDLBuiltinKind.UNRESTRICTED_DOUBLE,
            "bigint"               to WebIDLBuiltinKind.BIG_INT,
            "DOMString"            to WebIDLBuiltinKind.STRING,
            "ByteString"           to WebIDLBuiltinKind.BYTE_SEQUENCE,
            "USVString"            to WebIDLBuiltinKind.USV_STRING,
            "object"               to WebIDLBuiltinKind.OBJECT,
            "symbol"               to WebIDLBuiltinKind.CHAR,
        )
        override val overloadingSupported: Boolean = true
        override val emptyEnumSupported: Boolean = true
    }
}

