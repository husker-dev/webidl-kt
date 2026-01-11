package com.huskerdev.webidl

import com.huskerdev.webidl.resolver.WebIDLBuiltinKind

@Suppress("unused")
interface WebIDLEnv {

    val builtinTypes: Map<String, WebIDLBuiltinKind>

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
    }

    object UniFFI: WebIDLEnv {
        override val builtinTypes = mapOf(
            "sequence"             to WebIDLBuiltinKind.MUTABLE_LIST,
            "record"               to WebIDLBuiltinKind.MAP,
            "boolean"              to WebIDLBuiltinKind.BOOLEAN,
            "i8"                   to WebIDLBuiltinKind.BYTE,
            "u8"                   to WebIDLBuiltinKind.UNSIGNED_BYTE,
            "i16"                  to WebIDLBuiltinKind.SHORT,
            "u16"                  to WebIDLBuiltinKind.UNSIGNED_SHORT,
            "i32"                  to WebIDLBuiltinKind.INT,
            "u32"                  to WebIDLBuiltinKind.UNSIGNED_INT,
            "i64"                  to WebIDLBuiltinKind.LONG,
            "u64"                  to WebIDLBuiltinKind.UNSIGNED_LONG,
            "f32"                  to WebIDLBuiltinKind.UNRESTRICTED_FLOAT,
            "f64"                  to WebIDLBuiltinKind.UNRESTRICTED_DOUBLE,
            "string"               to WebIDLBuiltinKind.STRING,
            "bytes"                to WebIDLBuiltinKind.BYTE_SEQUENCE,
            "timestamp"            to WebIDLBuiltinKind.LONG,
            "duration"             to WebIDLBuiltinKind.LONG,
            "void"                 to WebIDLBuiltinKind.VOID,
        )
    }
}

