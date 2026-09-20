@file:OptIn(ExperimentalContracts::class)
@file:Suppress("unused")

package com.huskerdev.webidl

import com.huskerdev.webidl.resolver.*
import kotlin.collections.contains
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract


fun ResolvedIdlType.arrayTypeOrNull(): ResolvedIdlType.Default? {
    contract {
        returnsNotNull() implies(this@arrayTypeOrNull is ResolvedIdlType.Default)
    }
    if(!isArray()) return null
    return parameters.firstOrNull() as? ResolvedIdlType.Default
}

fun ResolvedIdlType.builtinOrNull(): BuiltinIdlDeclaration? {
    contract {
        returnsNotNull() implies(this@builtinOrNull is ResolvedIdlType.Default)
    }
    if (this !is ResolvedIdlType.Default || declaration !is BuiltinIdlDeclaration)
        return null
    return declaration
}

fun ResolvedIdlType.isPrimitive(): Boolean {
    contract {
        returns(true) implies(this@isPrimitive is ResolvedIdlType.Default)
    }
    return builtinOrNull()?.kind in setOf(
        WebIDLBuiltinKind.CHAR,
        WebIDLBuiltinKind.BOOLEAN,
        WebIDLBuiltinKind.BYTE,
        WebIDLBuiltinKind.UNSIGNED_BYTE,
        WebIDLBuiltinKind.SHORT,
        WebIDLBuiltinKind.UNSIGNED_SHORT,
        WebIDLBuiltinKind.INT,
        WebIDLBuiltinKind.UNSIGNED_INT,
        WebIDLBuiltinKind.LONG,
        WebIDLBuiltinKind.UNSIGNED_LONG,
        WebIDLBuiltinKind.FLOAT,
        WebIDLBuiltinKind.UNRESTRICTED_FLOAT,
        WebIDLBuiltinKind.DOUBLE,
        WebIDLBuiltinKind.UNRESTRICTED_DOUBLE
    )
}

fun ResolvedIdlType.isNonPrimitive(): Boolean {
    contract {
        returns(true) implies(this@isNonPrimitive is ResolvedIdlType.Default)
    }
    return !isPrimitive()
}

fun ResolvedIdlType.isUnsigned(): Boolean {
    contract {
        returns(true) implies(this@isUnsigned is ResolvedIdlType.Default)
    }
    val kind = builtinOrNull()?.kind
        ?: throw UnsupportedOperationException("Not builtin type")
    return kind == WebIDLBuiltinKind.UNSIGNED_BYTE ||
            kind == WebIDLBuiltinKind.UNSIGNED_SHORT ||
            kind == WebIDLBuiltinKind.UNSIGNED_INT ||
            kind == WebIDLBuiltinKind.UNSIGNED_LONG
}

fun ResolvedIdlType.isSigned(): Boolean {
    contract {
        returns(true) implies(this@isSigned is ResolvedIdlType.Default)
    }
    return !isUnsigned()
}

fun ResolvedIdlType.toSignedType(): ResolvedIdlType {
    contract {
        returns(true) implies(this@toSignedType is ResolvedIdlType.Default)
    }
    val kind = builtinOrNull()?.kind
        ?: throw UnsupportedOperationException("Not builtin type")
    val signedKind = when (kind) {
        WebIDLBuiltinKind.UNSIGNED_BYTE -> WebIDLBuiltinKind.BYTE
        WebIDLBuiltinKind.UNSIGNED_SHORT -> WebIDLBuiltinKind.SHORT
        WebIDLBuiltinKind.UNSIGNED_INT -> WebIDLBuiltinKind.INT
        WebIDLBuiltinKind.UNSIGNED_LONG -> WebIDLBuiltinKind.LONG
        else -> kind
    }
    return ResolvedIdlType.Default(BuiltinIdlDeclaration(declaration.name, signedKind), isNullable = isNullable)
}

fun ResolvedIdlType.toUnsignedType(): ResolvedIdlType {
    contract {
        returns(true) implies(this@toUnsignedType is ResolvedIdlType.Default)
    }
    val kind = builtinOrNull()?.kind
        ?: throw UnsupportedOperationException("Not builtin type")
    val unsignedKind = when (kind) {
        WebIDLBuiltinKind.BYTE -> WebIDLBuiltinKind.UNSIGNED_BYTE
        WebIDLBuiltinKind.SHORT -> WebIDLBuiltinKind.UNSIGNED_SHORT
        WebIDLBuiltinKind.INT -> WebIDLBuiltinKind.UNSIGNED_INT
        WebIDLBuiltinKind.LONG -> WebIDLBuiltinKind.UNSIGNED_LONG
        else -> kind
    }
    return ResolvedIdlType.Default(BuiltinIdlDeclaration(declaration.name, unsignedKind), isNullable = isNullable)
}

fun ResolvedIdlType.isVoid(): Boolean {
    contract {
        returns(true) implies(this@isVoid is ResolvedIdlType.Void)
    }
    return this is ResolvedIdlType.Void
}

fun ResolvedIdlType.isByte(): Boolean {
    contract {
        returns(true) implies(this@isByte is ResolvedIdlType.Default)
    }
    return builtinOrNull()?.kind == WebIDLBuiltinKind.BYTE
}

fun ResolvedIdlType.isUByte(): Boolean {
    contract {
        returns(true) implies(this@isUByte is ResolvedIdlType.Default)
    }
    return builtinOrNull()?.kind == WebIDLBuiltinKind.UNSIGNED_BYTE
}

fun ResolvedIdlType.isBoolean(): Boolean {
    contract {
        returns(true) implies(this@isBoolean is ResolvedIdlType.Default)
    }
    return builtinOrNull()?.kind == WebIDLBuiltinKind.BOOLEAN
}

fun ResolvedIdlType.isChar(): Boolean {
    contract {
        returns(true) implies(this@isChar is ResolvedIdlType.Default)
    }
    return builtinOrNull()?.kind == WebIDLBuiltinKind.CHAR
}

fun ResolvedIdlType.isShort(): Boolean {
    contract {
        returns(true) implies(this@isShort is ResolvedIdlType.Default)
    }
    return builtinOrNull()?.kind == WebIDLBuiltinKind.SHORT
}

fun ResolvedIdlType.isUShort(): Boolean {
    contract {
        returns(true) implies(this@isUShort is ResolvedIdlType.Default)
    }
    return builtinOrNull()?.kind == WebIDLBuiltinKind.UNSIGNED_SHORT
}

fun ResolvedIdlType.isInt(): Boolean {
    contract {
        returns(true) implies(this@isInt is ResolvedIdlType.Default)
    }
    return builtinOrNull()?.kind == WebIDLBuiltinKind.INT
}

fun ResolvedIdlType.isUInt(): Boolean {
    contract {
        returns(true) implies(this@isUInt is ResolvedIdlType.Default)
    }
    return builtinOrNull()?.kind == WebIDLBuiltinKind.UNSIGNED_INT
}

fun ResolvedIdlType.isLong(): Boolean {
    contract {
        returns(true) implies(this@isLong is ResolvedIdlType.Default)
    }
    return builtinOrNull()?.kind == WebIDLBuiltinKind.LONG
}

fun ResolvedIdlType.isULong(): Boolean {
    contract {
        returns(true) implies(this@isULong is ResolvedIdlType.Default)
    }
    return builtinOrNull()?.kind == WebIDLBuiltinKind.UNSIGNED_LONG
}

fun ResolvedIdlType.isFloat(): Boolean {
    contract {
        returns(true) implies(this@isFloat is ResolvedIdlType.Default)
    }
    return builtinOrNull()?.kind == WebIDLBuiltinKind.FLOAT
}

fun ResolvedIdlType.isDouble(): Boolean {
    contract {
        returns(true) implies(this@isDouble is ResolvedIdlType.Default)
    }
    return builtinOrNull()?.kind == WebIDLBuiltinKind.DOUBLE
}

fun ResolvedIdlType.isString(): Boolean {
    contract {
        returns(true) implies(this@isString is ResolvedIdlType.Default)
    }
    return builtinOrNull()?.kind == WebIDLBuiltinKind.STRING
}

fun ResolvedIdlType.isArray(): Boolean {
    contract {
        returns(true) implies(this@isArray is ResolvedIdlType.Default)
    }
    return builtinOrNull()?.kind == WebIDLBuiltinKind.LIST
}

fun ResolvedIdlType.isCallback(): Boolean {
    contract {
        returns(true) implies(this@isCallback is ResolvedIdlType.Default)
    }
    return this is ResolvedIdlType.Default && declaration is ResolvedIdlCallbackFunction
}

fun ResolvedIdlType.isEnum(): Boolean {
    contract {
        returns(true) implies(this@isEnum is ResolvedIdlType.Default)
    }
    return this is ResolvedIdlType.Default && declaration is ResolvedIdlEnum
}

fun ResolvedIdlType.isInterface(): Boolean {
    contract {
        returns(true) implies(this@isInterface is ResolvedIdlType.Default)
    }
    return this is ResolvedIdlType.Default && declaration is ResolvedIdlInterface
}

fun ResolvedIdlType.isDictionary(): Boolean {
    contract {
        returns(true) implies(this@isDictionary is ResolvedIdlType.Default)
    }
    return this is ResolvedIdlType.Default && declaration is ResolvedIdlDictionary
}

// ==== Arrays =====

fun ResolvedIdlType.isByteArray(): Boolean {
    contract {
        returns(true) implies(this@isByteArray is ResolvedIdlType.Default)
    }
    return arrayTypeOrNull()?.isByte() ?: false
}

fun ResolvedIdlType.isUByteArray(): Boolean {
    contract {
        returns(true) implies(this@isUByteArray is ResolvedIdlType.Default)
    }
    return arrayTypeOrNull()?.isUByte() ?: false
}

fun ResolvedIdlType.isBooleanArray(): Boolean {
    contract {
        returns(true) implies(this@isBooleanArray is ResolvedIdlType.Default)
    }
    return arrayTypeOrNull()?.isBoolean() ?: false
}

fun ResolvedIdlType.isCharArray(): Boolean {
    contract {
        returns(true) implies(this@isCharArray is ResolvedIdlType.Default)
    }
    return arrayTypeOrNull()?.isChar() ?: false
}

fun ResolvedIdlType.isShortArray(): Boolean {
    contract {
        returns(true) implies(this@isShortArray is ResolvedIdlType.Default)
    }
    return arrayTypeOrNull()?.isShort() ?: false
}

fun ResolvedIdlType.isUShortArray(): Boolean {
    contract {
        returns(true) implies(this@isUShortArray is ResolvedIdlType.Default)
    }
    return arrayTypeOrNull()?.isUShort() ?: false
}

fun ResolvedIdlType.isIntArray(): Boolean {
    contract {
        returns(true) implies(this@isIntArray is ResolvedIdlType.Default)
    }
    return arrayTypeOrNull()?.isInt() ?: false
}

fun ResolvedIdlType.isUIntArray(): Boolean {
    contract {
        returns(true) implies(this@isUIntArray is ResolvedIdlType.Default)
    }
    return arrayTypeOrNull()?.isUInt() ?: false
}

fun ResolvedIdlType.isLongArray(): Boolean {
    contract {
        returns(true) implies(this@isLongArray is ResolvedIdlType.Default)
    }
    return arrayTypeOrNull()?.isLong() ?: false
}

fun ResolvedIdlType.isULongArray(): Boolean {
    contract {
        returns(true) implies(this@isULongArray is ResolvedIdlType.Default)
    }
    return arrayTypeOrNull()?.isULong() ?: false
}

fun ResolvedIdlType.isFloatArray(): Boolean {
    contract {
        returns(true) implies(this@isFloatArray is ResolvedIdlType.Default)
    }
    return arrayTypeOrNull()?.isFloat() ?: false
}

fun ResolvedIdlType.isDoubleArray(): Boolean {
    contract {
        returns(true) implies(this@isDoubleArray is ResolvedIdlType.Default)
    }
    return arrayTypeOrNull()?.isDouble() ?: false
}

fun ResolvedIdlType.isStringArray(): Boolean {
    contract {
        returns(true) implies(this@isStringArray is ResolvedIdlType.Default)
    }
    return arrayTypeOrNull()?.isString() ?: false
}

fun ResolvedIdlType.isPrimitiveArray(): Boolean {
    contract {
        returns(true) implies(this@isPrimitiveArray is ResolvedIdlType.Default)
    }
    return arrayTypeOrNull()?.isPrimitive() ?: false
}

fun ResolvedIdlType.isNonPrimitiveArray(): Boolean {
    contract {
        returns(true) implies(this@isNonPrimitiveArray is ResolvedIdlType.Default)
    }
    return arrayTypeOrNull()?.isNonPrimitive() ?: false
}

fun ResolvedIdlType.isEnumArray(): Boolean {
    contract {
        returns(true) implies(this@isEnumArray is ResolvedIdlType.Default)
    }
    return arrayTypeOrNull()?.isEnum() ?: false
}

fun ResolvedIdlType.isDictionaryArray(): Boolean {
    contract {
        returns(true) implies(this@isDictionaryArray is ResolvedIdlType.Default)
    }
    return arrayTypeOrNull()?.isDictionary() ?: false
}

fun ResolvedIdlType.isInterfaceArray(): Boolean {
    contract {
        returns(true) implies(this@isInterfaceArray is ResolvedIdlType.Default)
    }
    return arrayTypeOrNull()?.isInterface() ?: false
}

fun ResolvedIdlDictionary.collectAllFields() = buildList {
    var cur: ResolvedIdlDictionary? = this@collectAllFields
    while(cur != null) {
        addAll(0, cur.fields)
        cur = cur.implements
    }
}