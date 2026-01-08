package com.huskerdev.webidl.ast

import com.huskerdev.webidl.WebIDLEnv
import com.huskerdev.webidl.parser.WebIDLAsyncIterableLikeDef
import com.huskerdev.webidl.parser.WebIDLCallbackFunctionDef
import com.huskerdev.webidl.parser.WebIDLConstructorDef
import com.huskerdev.webidl.parser.WebIDLDefinitionRoot
import com.huskerdev.webidl.parser.WebIDLDictionaryDef
import com.huskerdev.webidl.parser.WebIDLEnumDef
import com.huskerdev.webidl.parser.WebIDLFieldDef
import com.huskerdev.webidl.parser.WebIDLOperationDef
import com.huskerdev.webidl.parser.WebIDLImplementsDef
import com.huskerdev.webidl.parser.WebIDLIncludesDef
import com.huskerdev.webidl.parser.WebIDLInterfaceDef
import com.huskerdev.webidl.parser.WebIDLIterableDef
import com.huskerdev.webidl.parser.WebIDLMapLikeDef
import com.huskerdev.webidl.parser.WebIDLNamespaceDef
import com.huskerdev.webidl.parser.WebIDLSetLikeDef
import com.huskerdev.webidl.parser.WebIDLTypeDefDef

class WebIDLAST(
    val root: WebIDLDefinitionRoot,
    env: WebIDLEnv = WebIDLEnv.Default
) {
    val builtinTypes: Map<String, WebIDLBuiltin> = env.builtinTypes.mapValues {
        WebIDLBuiltin(it.key, it.value)
    }

    val interfaces: Map<String, WebIDLInterface>
        field = linkedMapOf()

    private val mixins = linkedMapOf<String, WebIDLInterface>()

    val dictionaries: Map<String, WebIDLDictionary>
        field = linkedMapOf()

    val enums: Map<String, WebIDLEnum>
        field = linkedMapOf()

    val typeDefs: Map<String, WebIDLTypeDef>
        field = linkedMapOf()

    val namespaces: Map<String, WebIDLNamespace>
        field = linkedMapOf()

    val callbacks: Map<String, WebIDLCallback>
        field = linkedMapOf()

    init {
        declareTypes()
        resolveTypeDefs()
        resolveDeclarationMembers()
        execExpands()
    }

    fun findDeclaration(name: String): WebIDLDeclaration {
        return interfaces[name]
            ?: dictionaries[name]
            ?: callbacks[name]
            ?: enums[name]
            ?: typeDefs[name]
            ?: builtinTypes[name]
            ?: throw UnsupportedOperationException("Type '$name' is not declared")
    }

    fun findType(type: com.huskerdev.webidl.parser.WebIDLType): WebIDLType {
        throw UnsupportedOperationException()
    }

    private fun declareTypes() {
        root.definitions.forEach { def ->
            when (def) {
                is WebIDLInterfaceDef if(!def.isPartial) ->
                    (if(def.isMixin) mixins else interfaces)[def.name] = WebIDLInterface(def.name, def.isCallback)
                is WebIDLDictionaryDef ->
                    dictionaries[def.name] = WebIDLDictionary(def.name)
                is WebIDLEnumDef ->
                    enums[def.name] = WebIDLEnum(def.name)
                is WebIDLNamespaceDef ->
                    namespaces[def.name] = WebIDLNamespace(def.name)
                is WebIDLTypeDefDef ->
                    typeDefs[def.name] = WebIDLTypeDef(def.name, def.type)
                is WebIDLCallbackFunctionDef ->
                    callbacks[def.name] = WebIDLCallback(def.name)
                else -> return@forEach
            }
        }
    }

    private fun resolveTypeDefs() {
        typeDefs.values.forEach {
            it.resolve(this)
        }
    }

    private fun resolveDeclarationMembers(){
        root.definitions.forEach { decl ->
            when (decl) {
                is WebIDLInterfaceDef -> {
                    val inter = if(decl.isMixin)
                        mixins[decl.name]!! else interfaces[decl.name]!!

                    if(decl.implements != null) {
                        inter.implements = findDeclaration(decl.implements).run {
                            if(this !is WebIDLInterface)
                                throw UnsupportedOperationException("Expected interface")
                            this
                        }
                    }

                    decl.definitions.forEach { decl ->
                        when(decl) {
                            is WebIDLAsyncIterableLikeDef ->
                                inter.asyncIterableType = findType(decl.keyType) to decl.valueType?.run { findType(this) }
                            is WebIDLIterableDef ->
                                inter.iterableType = findType(decl.keyType) to decl.valueType?.run { findType(this) }
                            is WebIDLMapLikeDef ->
                                inter.mapType = findType(decl.keyType) to findType(decl.valueType)
                            is WebIDLSetLikeDef ->
                                inter.setType = findType(decl.type)

                            is WebIDLFieldDef ->
                                (if(decl.isStatic) inter.staticFields else inter.fields) += decl.toDeclarationField()

                            is WebIDLOperationDef ->
                                (if(decl.isStatic) inter.staticFunctions else inter.functions) += decl.toFunction()

                            is WebIDLConstructorDef -> {
                                inter.constructors += WebIDLConstructor(
                                    decl.args.map { it.toArgField() }
                                )
                            }
                            else -> throw UnsupportedOperationException("Unexpected definition")
                        }
                    }
                }
                is WebIDLDictionaryDef -> {
                    val dict = dictionaries[decl.name]!!

                    decl.definitions.forEach { decl ->
                        when(decl) {
                            is WebIDLFieldDef ->
                                dict.fields += decl.toDeclarationField()
                            else -> throw UnsupportedOperationException("Unexpected definition")
                        }
                    }
                }
                is WebIDLCallbackFunctionDef -> {
                    val callback = callbacks[decl.name]!!
                    callback.type = findType(decl.operation.type)
                    callback.args = decl.operation.args.map { it.toArgField() }
                }
                is WebIDLNamespaceDef -> {
                    val namespace = namespaces[decl.name]!!

                    decl.definitions.forEach { decl ->
                        when(decl) {
                            is WebIDLFieldDef ->
                                namespace.fields += decl.toDeclarationField()
                            is WebIDLOperationDef ->
                                namespace.functions += decl.toFunction()
                            else -> throw UnsupportedOperationException("Unexpected definition")
                        }
                    }
                }
                else -> Unit
            }
        }
    }

    private fun execExpands(){
        root.definitions.forEach { decl ->
            when (decl) {
                is WebIDLImplementsDef -> {
                    val target = interfaces[decl.target]
                        ?: throw UnsupportedOperationException("Expected interface")
                    val source = interfaces[decl.source]
                        ?: throw UnsupportedOperationException("Expected interface")
                    target.implements = source
                }
                is WebIDLIncludesDef -> {
                    val target = interfaces[decl.target]
                        ?: throw UnsupportedOperationException("Expected interface")
                    val source = mixins[decl.source]
                        ?: throw UnsupportedOperationException("Expected interface")
                    target.applyMixin(source)
                }
                else -> Unit
            }
        }
    }

    private fun WebIDLFieldDef.toDeclarationField() = WebIDLDeclarationField(
        name,
        findType(type),
        value,
        isConst, isReadOnly,isStatic, isInherit
    )

    private fun WebIDLFieldDef.toArgField() = WebIDLArgField(
        name,
        findType(type),
        value,
        isOptional, isVariadic
    )


    private fun WebIDLOperationDef.toFunction() = WebIDLFunction(
        name,
        findType(type),
        args.map { it.toArgField() },
        isStatic
    )
}