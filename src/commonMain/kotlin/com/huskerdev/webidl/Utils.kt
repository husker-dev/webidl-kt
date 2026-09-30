@file:OptIn(ExperimentalContracts::class)
@file:Suppress("unused")

package com.huskerdev.webidl

import com.huskerdev.webidl.resolver.*
import kotlin.collections.contains
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

private val primitives = setOf(
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

fun ResolvedIdlType.isSameNullability(isNullable: Boolean?) =
    isNullable == null || isNullable == this.isNullable

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

fun ResolvedIdlType.isPrimitive(isNullable: Boolean? = null): Boolean {
    contract {
        returns(true) implies(this@isPrimitive is ResolvedIdlType.Default)
    }
    return builtinOrNull()?.kind in primitives && isSameNullability(isNullable)
}

fun ResolvedIdlType.isNonPrimitive(isNullable: Boolean? = null): Boolean {
    contract {
        returns(true) implies(this@isNonPrimitive is ResolvedIdlType.Default)
    }
    return builtinOrNull()?.kind !in primitives && isSameNullability(isNullable)
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

fun ResolvedIdlType.isByte(isNullable: Boolean? = null): Boolean {
    contract {
        returns(true) implies(this@isByte is ResolvedIdlType.Default)
    }
    return builtinOrNull()?.kind == WebIDLBuiltinKind.BYTE && isSameNullability(isNullable)
}

fun ResolvedIdlType.isUByte(isNullable: Boolean? = null): Boolean {
    contract {
        returns(true) implies(this@isUByte is ResolvedIdlType.Default)
    }
    return builtinOrNull()?.kind == WebIDLBuiltinKind.UNSIGNED_BYTE && isSameNullability(isNullable)
}

fun ResolvedIdlType.isBoolean(isNullable: Boolean? = null): Boolean {
    contract {
        returns(true) implies(this@isBoolean is ResolvedIdlType.Default)
    }
    return builtinOrNull()?.kind == WebIDLBuiltinKind.BOOLEAN && isSameNullability(isNullable)
}

fun ResolvedIdlType.isChar(isNullable: Boolean? = null): Boolean {
    contract {
        returns(true) implies(this@isChar is ResolvedIdlType.Default)
    }
    return builtinOrNull()?.kind == WebIDLBuiltinKind.CHAR && isSameNullability(isNullable)
}

fun ResolvedIdlType.isShort(isNullable: Boolean? = null): Boolean {
    contract {
        returns(true) implies(this@isShort is ResolvedIdlType.Default)
    }
    return builtinOrNull()?.kind == WebIDLBuiltinKind.SHORT && isSameNullability(isNullable)
}

fun ResolvedIdlType.isUShort(isNullable: Boolean? = null): Boolean {
    contract {
        returns(true) implies(this@isUShort is ResolvedIdlType.Default)
    }
    return builtinOrNull()?.kind == WebIDLBuiltinKind.UNSIGNED_SHORT && isSameNullability(isNullable)
}

fun ResolvedIdlType.isInt(isNullable: Boolean? = null): Boolean {
    contract {
        returns(true) implies(this@isInt is ResolvedIdlType.Default)
    }
    return builtinOrNull()?.kind == WebIDLBuiltinKind.INT && isSameNullability(isNullable)
}

fun ResolvedIdlType.isUInt(isNullable: Boolean? = null): Boolean {
    contract {
        returns(true) implies(this@isUInt is ResolvedIdlType.Default)
    }
    return builtinOrNull()?.kind == WebIDLBuiltinKind.UNSIGNED_INT && isSameNullability(isNullable)
}

fun ResolvedIdlType.isLong(isNullable: Boolean? = null): Boolean {
    contract {
        returns(true) implies(this@isLong is ResolvedIdlType.Default)
    }
    return builtinOrNull()?.kind == WebIDLBuiltinKind.LONG && isSameNullability(isNullable)
}

fun ResolvedIdlType.isULong(isNullable: Boolean? = null): Boolean {
    contract {
        returns(true) implies(this@isULong is ResolvedIdlType.Default)
    }
    return builtinOrNull()?.kind == WebIDLBuiltinKind.UNSIGNED_LONG && isSameNullability(isNullable)
}

fun ResolvedIdlType.isFloat(isNullable: Boolean? = null): Boolean {
    contract {
        returns(true) implies(this@isFloat is ResolvedIdlType.Default)
    }
    return builtinOrNull()?.kind == WebIDLBuiltinKind.FLOAT && isSameNullability(isNullable)
}

fun ResolvedIdlType.isDouble(isNullable: Boolean? = null): Boolean {
    contract {
        returns(true) implies(this@isDouble is ResolvedIdlType.Default)
    }
    return builtinOrNull()?.kind == WebIDLBuiltinKind.DOUBLE && isSameNullability(isNullable)
}

fun ResolvedIdlType.isString(isNullable: Boolean? = null): Boolean {
    contract {
        returns(true) implies(this@isString is ResolvedIdlType.Default)
    }
    return builtinOrNull()?.kind == WebIDLBuiltinKind.STRING && isSameNullability(isNullable)
}

fun ResolvedIdlType.isArray(isNullable: Boolean? = null, parameterIsNullable: Boolean? = null): Boolean {
    contract {
        returns(true) implies(this@isArray is ResolvedIdlType.Default)
    }
    return builtinOrNull()?.let { it.kind == WebIDLBuiltinKind.LIST } ?: false
            && isSameNullability(isNullable)
            && parameters[0].isSameNullability(parameterIsNullable)
}

fun ResolvedIdlType.isCallback(isNullable: Boolean? = null): Boolean {
    contract {
        returns(true) implies(this@isCallback is ResolvedIdlType.Default)
    }
    return this is ResolvedIdlType.Default
            && declaration is ResolvedIdlCallbackFunction
            && isSameNullability(isNullable)
}

fun ResolvedIdlType.isEnum(isNullable: Boolean? = null): Boolean {
    contract {
        returns(true) implies(this@isEnum is ResolvedIdlType.Default)
    }
    return this is ResolvedIdlType.Default
            && declaration is ResolvedIdlEnum
            && isSameNullability(isNullable)
}

fun ResolvedIdlType.isInterface(isNullable: Boolean? = null): Boolean {
    contract {
        returns(true) implies(this@isInterface is ResolvedIdlType.Default)
    }
    return this is ResolvedIdlType.Default
            && declaration is ResolvedIdlInterface
            && isSameNullability(isNullable)
}

fun ResolvedIdlType.isDictionary(isNullable: Boolean? = null): Boolean {
    contract {
        returns(true) implies(this@isDictionary is ResolvedIdlType.Default)
    }
    return this is ResolvedIdlType.Default
            && declaration is ResolvedIdlDictionary
            && isSameNullability(isNullable)
}

// ==== Arrays =====

fun ResolvedIdlType.isByteArray(isNullable: Boolean? = null, parameterIsNullable: Boolean? = null): Boolean {
    contract {
        returns(true) implies(this@isByteArray is ResolvedIdlType.Default)
    }
    return arrayTypeOrNull()?.isByte(parameterIsNullable) ?: false && isSameNullability(isNullable)
}

fun ResolvedIdlType.isUByteArray(isNullable: Boolean? = null, parameterIsNullable: Boolean? = null): Boolean {
    contract {
        returns(true) implies(this@isUByteArray is ResolvedIdlType.Default)
    }
    return arrayTypeOrNull()?.isUByte(parameterIsNullable) ?: false && isSameNullability(isNullable)
}

fun ResolvedIdlType.isBooleanArray(isNullable: Boolean? = null, parameterIsNullable: Boolean? = null): Boolean {
    contract {
        returns(true) implies(this@isBooleanArray is ResolvedIdlType.Default)
    }
    return arrayTypeOrNull()?.isBoolean(parameterIsNullable) ?: false && isSameNullability(isNullable)
}

fun ResolvedIdlType.isCharArray(isNullable: Boolean? = null, parameterIsNullable: Boolean? = null): Boolean {
    contract {
        returns(true) implies(this@isCharArray is ResolvedIdlType.Default)
    }
    return arrayTypeOrNull()?.isChar(parameterIsNullable) ?: false && isSameNullability(isNullable)
}

fun ResolvedIdlType.isShortArray(isNullable: Boolean? = null, parameterIsNullable: Boolean? = null): Boolean {
    contract {
        returns(true) implies(this@isShortArray is ResolvedIdlType.Default)
    }
    return arrayTypeOrNull()?.isShort(parameterIsNullable) ?: false && isSameNullability(isNullable)
}

fun ResolvedIdlType.isUShortArray(isNullable: Boolean? = null, parameterIsNullable: Boolean? = null): Boolean {
    contract {
        returns(true) implies(this@isUShortArray is ResolvedIdlType.Default)
    }
    return arrayTypeOrNull()?.isUShort(parameterIsNullable) ?: false && isSameNullability(isNullable)
}

fun ResolvedIdlType.isIntArray(isNullable: Boolean? = null, parameterIsNullable: Boolean? = null): Boolean {
    contract {
        returns(true) implies(this@isIntArray is ResolvedIdlType.Default)
    }
    return arrayTypeOrNull()?.isInt(parameterIsNullable) ?: false && isSameNullability(isNullable)
}

fun ResolvedIdlType.isUIntArray(isNullable: Boolean? = null, parameterIsNullable: Boolean? = null): Boolean {
    contract {
        returns(true) implies(this@isUIntArray is ResolvedIdlType.Default)
    }
    return arrayTypeOrNull()?.isUInt(parameterIsNullable) ?: false && isSameNullability(isNullable)
}

fun ResolvedIdlType.isLongArray(isNullable: Boolean? = null, parameterIsNullable: Boolean? = null): Boolean {
    contract {
        returns(true) implies(this@isLongArray is ResolvedIdlType.Default)
    }
    return arrayTypeOrNull()?.isLong(parameterIsNullable) ?: false && isSameNullability(isNullable)
}

fun ResolvedIdlType.isULongArray(isNullable: Boolean? = null, parameterIsNullable: Boolean? = null): Boolean {
    contract {
        returns(true) implies(this@isULongArray is ResolvedIdlType.Default)
    }
    return arrayTypeOrNull()?.isULong(parameterIsNullable) ?: false && isSameNullability(isNullable)
}

fun ResolvedIdlType.isFloatArray(isNullable: Boolean? = null, parameterIsNullable: Boolean? = null): Boolean {
    contract {
        returns(true) implies(this@isFloatArray is ResolvedIdlType.Default)
    }
    return arrayTypeOrNull()?.isFloat(parameterIsNullable) ?: false && isSameNullability(isNullable)
}

fun ResolvedIdlType.isDoubleArray(isNullable: Boolean? = null, parameterIsNullable: Boolean? = null): Boolean {
    contract {
        returns(true) implies(this@isDoubleArray is ResolvedIdlType.Default)
    }
    return arrayTypeOrNull()?.isDouble(parameterIsNullable) ?: false && isSameNullability(isNullable)
}

fun ResolvedIdlType.isStringArray(isNullable: Boolean? = null, parameterIsNullable: Boolean? = null): Boolean {
    contract {
        returns(true) implies(this@isStringArray is ResolvedIdlType.Default)
    }
    return arrayTypeOrNull()?.isString(parameterIsNullable) ?: false && isSameNullability(isNullable)
}

fun ResolvedIdlType.isPrimitiveArray(isNullable: Boolean? = null, parameterIsNullable: Boolean? = null): Boolean {
    contract {
        returns(true) implies(this@isPrimitiveArray is ResolvedIdlType.Default)
    }
    return arrayTypeOrNull()?.isPrimitive(parameterIsNullable) ?: false && isSameNullability(isNullable)
}

fun ResolvedIdlType.isNonPrimitiveArray(isNullable: Boolean? = null, parameterIsNullable: Boolean? = null): Boolean {
    contract {
        returns(true) implies(this@isNonPrimitiveArray is ResolvedIdlType.Default)
    }
    return arrayTypeOrNull()?.isNonPrimitive(parameterIsNullable) ?: false && isSameNullability(isNullable)
}

fun ResolvedIdlType.isEnumArray(isNullable: Boolean? = null, parameterIsNullable: Boolean? = null): Boolean {
    contract {
        returns(true) implies(this@isEnumArray is ResolvedIdlType.Default)
    }
    return arrayTypeOrNull()?.isEnum(parameterIsNullable) ?: false && isSameNullability(isNullable)
}

fun ResolvedIdlType.isDictionaryArray(isNullable: Boolean? = null, parameterIsNullable: Boolean? = null): Boolean {
    contract {
        returns(true) implies(this@isDictionaryArray is ResolvedIdlType.Default)
    }
    return arrayTypeOrNull()?.isDictionary(parameterIsNullable) ?: false && isSameNullability(isNullable)
}

fun ResolvedIdlType.isInterfaceArray(isNullable: Boolean? = null, parameterIsNullable: Boolean? = null): Boolean {
    contract {
        returns(true) implies(this@isInterfaceArray is ResolvedIdlType.Default)
    }
    return arrayTypeOrNull()?.isInterface(parameterIsNullable) ?: false && isSameNullability(isNullable)
}

fun ResolvedIdlDictionary.collectAllFields() = buildList {
    var cur: ResolvedIdlDictionary? = this@collectAllFields
    while(cur != null) {
        addAll(0, cur.fields)
        cur = cur.implements
    }
}