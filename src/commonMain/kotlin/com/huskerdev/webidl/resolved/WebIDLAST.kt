package com.huskerdev.webidl.resolved

import com.huskerdev.webidl.WebIDLEnv
import com.huskerdev.webidl.parser.IdlAsyncIterableLike
import com.huskerdev.webidl.parser.IdlCallbackFunction
import com.huskerdev.webidl.parser.IdlConstructor
import com.huskerdev.webidl.parser.IdlDefinitionRoot
import com.huskerdev.webidl.parser.IdlDictionary
import com.huskerdev.webidl.parser.IdlEnum
import com.huskerdev.webidl.parser.IdlField
import com.huskerdev.webidl.parser.IdlOperation
import com.huskerdev.webidl.parser.IdlImplements
import com.huskerdev.webidl.parser.IdlIncludes
import com.huskerdev.webidl.parser.IdlInterface
import com.huskerdev.webidl.parser.IdlIterable
import com.huskerdev.webidl.parser.IdlMapLike
import com.huskerdev.webidl.parser.IdlNamespace
import com.huskerdev.webidl.parser.IdlSetLike
import com.huskerdev.webidl.parser.IdlTypeDef

class WebIDLAST(
    val root: IdlDefinitionRoot,
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

    fun findType(type: com.huskerdev.webidl.parser.IdlType): WebIDLType {
        throw UnsupportedOperationException()
    }

    private fun declareTypes() {
        root.definitions.forEach { def ->
            when (def) {
                is IdlInterface if(!def.isPartial) ->
                    (if(def.isMixin) mixins else interfaces)[def.name] = WebIDLInterface(def.name, def.isCallback)
                is IdlDictionary ->
                    dictionaries[def.name] = WebIDLDictionary(def.name)
                is IdlEnum ->
                    enums[def.name] = WebIDLEnum(def.name)
                is IdlNamespace ->
                    namespaces[def.name] = WebIDLNamespace(def.name)
                is IdlTypeDef ->
                    typeDefs[def.name] = WebIDLTypeDef(def.name, def.type)
                is IdlCallbackFunction ->
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
                is IdlInterface -> {
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
                            is IdlAsyncIterableLike ->
                                inter.asyncIterableType = findType(decl.keyType) to decl.valueType?.run { findType(this) }
                            is IdlIterable ->
                                inter.iterableType = findType(decl.keyType) to decl.valueType?.run { findType(this) }
                            is IdlMapLike ->
                                inter.mapType = findType(decl.keyType) to findType(decl.valueType)
                            is IdlSetLike ->
                                inter.setType = findType(decl.type)

                            is IdlField ->
                                (if(decl.isStatic) inter.staticFields else inter.fields) += decl.toDeclarationField()

                            is IdlOperation ->
                                (if(decl.isStatic) inter.staticFunctions else inter.functions) += decl.toFunction()

                            is IdlConstructor -> {
                                inter.constructors += WebIDLConstructor(
                                    decl.args.map { it.toArgField() }
                                )
                            }
                            else -> throw UnsupportedOperationException("Unexpected definition")
                        }
                    }
                }
                is IdlDictionary -> {
                    val dict = dictionaries[decl.name]!!

                    decl.definitions.forEach { decl ->
                        when(decl) {
                            is IdlField ->
                                dict.fields += decl.toDeclarationField()
                            else -> throw UnsupportedOperationException("Unexpected definition")
                        }
                    }
                }
                is IdlCallbackFunction -> {
                    val callback = callbacks[decl.name]!!
                    callback.type = findType(decl.operation.type)
                    callback.args = decl.operation.args.map { it.toArgField() }
                }
                is IdlNamespace -> {
                    val namespace = namespaces[decl.name]!!

                    decl.definitions.forEach { decl ->
                        when(decl) {
                            is IdlField ->
                                namespace.fields += decl.toDeclarationField()
                            is IdlOperation ->
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
                is IdlImplements -> {
                    val target = interfaces[decl.target]
                        ?: throw UnsupportedOperationException("Expected interface")
                    val source = interfaces[decl.source]
                        ?: throw UnsupportedOperationException("Expected interface")
                    target.implements = source
                }
                is IdlIncludes -> {
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

    private fun IdlField.toDeclarationField() = WebIDLDeclarationField(
        name,
        findType(type),
        value,
        isConst, isReadOnly,isStatic, isInherit
    )

    private fun IdlField.toArgField() = WebIDLArgField(
        name,
        findType(type),
        value,
        isOptional, isVariadic
    )


    private fun IdlOperation.toFunction() = WebIDLFunction(
        name,
        findType(type),
        args.map { it.toArgField() },
        isStatic
    )
}