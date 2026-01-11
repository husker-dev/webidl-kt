package com.huskerdev.webidl.resolver

import com.huskerdev.webidl.parser.IdlValue

sealed interface ResolvedIdlType {
    val isNullable: Boolean

    fun canConsume(value: IdlValue): Boolean

    class Default(
        val declaration: ResolvedIdlDeclaration,
        val parameters: List<ResolvedIdlType>,
        override val isNullable: Boolean,
    ): ResolvedIdlType {
        override fun canConsume(value: IdlValue): Boolean {
            if(declaration is ResolvedIdlDictionary)
                return value is IdlValue.DictionaryInitValue

            if(declaration is BuiltinIdlDeclaration) {
                return when(value) {
                    is IdlValue.BooleanValue ->
                        declaration.kind == WebIDLBuiltinKind.BOOLEAN
                    is IdlValue.DecimalValue ->
                        declaration.kind == WebIDLBuiltinKind.INT ||
                        declaration.kind == WebIDLBuiltinKind.UNSIGNED_INT ||
                        declaration.kind == WebIDLBuiltinKind.FLOAT ||
                        declaration.kind == WebIDLBuiltinKind.UNRESTRICTED_FLOAT ||
                        declaration.kind == WebIDLBuiltinKind.DOUBLE ||
                        declaration.kind == WebIDLBuiltinKind.UNRESTRICTED_DOUBLE ||
                        declaration.kind == WebIDLBuiltinKind.BYTE ||
                        declaration.kind == WebIDLBuiltinKind.UNSIGNED_BYTE ||
                        declaration.kind == WebIDLBuiltinKind.SHORT ||
                        declaration.kind == WebIDLBuiltinKind.UNSIGNED_SHORT ||
                        declaration.kind == WebIDLBuiltinKind.LONG ||
                        declaration.kind == WebIDLBuiltinKind.UNSIGNED_LONG
                    IdlValue.NullValue ->
                        declaration.kind == WebIDLBuiltinKind.ANY ||
                        declaration.kind == WebIDLBuiltinKind.OBJECT ||
                        declaration.kind == WebIDLBuiltinKind.LIST ||
                        declaration.kind == WebIDLBuiltinKind.MUTABLE_LIST ||
                        declaration.kind == WebIDLBuiltinKind.MAP ||
                        declaration.kind == WebIDLBuiltinKind.PROMISE
                    is IdlValue.IntValue ->
                        declaration.kind == WebIDLBuiltinKind.INT ||
                        declaration.kind == WebIDLBuiltinKind.UNSIGNED_INT ||
                        declaration.kind == WebIDLBuiltinKind.BYTE ||
                        declaration.kind == WebIDLBuiltinKind.UNSIGNED_BYTE ||
                        declaration.kind == WebIDLBuiltinKind.SHORT ||
                        declaration.kind == WebIDLBuiltinKind.UNSIGNED_SHORT ||
                        declaration.kind == WebIDLBuiltinKind.LONG ||
                        declaration.kind == WebIDLBuiltinKind.UNSIGNED_LONG
                    is IdlValue.StringValue ->
                        declaration.kind == WebIDLBuiltinKind.STRING ||
                        declaration.kind == WebIDLBuiltinKind.USV_STRING
                    else -> false
                }
            }
            if(value is IdlValue.NullValue && (declaration is ResolvedIdlInterface || declaration is ResolvedIdlDictionary))
                return declaration.nullable
            return false
        }
    }

    class Union(
        val types: List<ResolvedIdlType>,
        override val isNullable: Boolean
    ): ResolvedIdlType {
        override fun canConsume(value: IdlValue): Boolean = false
    }
}

