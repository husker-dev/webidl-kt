package com.huskerdev.webidl.resolver

import com.huskerdev.webidl.WebIDLEnv
import com.huskerdev.webidl.WebIDLPrinter
import com.huskerdev.webidl.WebIDLResolverException
import com.huskerdev.webidl.parser.IdlAsyncIterableLike
import com.huskerdev.webidl.parser.IdlCallbackFunction
import com.huskerdev.webidl.parser.IdlConstructor
import com.huskerdev.webidl.parser.IdlDefinitionRoot
import com.huskerdev.webidl.parser.IdlDictionary
import com.huskerdev.webidl.parser.IdlEnum
import com.huskerdev.webidl.parser.IdlField
import com.huskerdev.webidl.parser.IdlGetter
import com.huskerdev.webidl.parser.IdlOperation
import com.huskerdev.webidl.parser.IdlImplements
import com.huskerdev.webidl.parser.IdlIncludes
import com.huskerdev.webidl.parser.IdlInterface
import com.huskerdev.webidl.parser.IdlIterable
import com.huskerdev.webidl.parser.IdlMapLike
import com.huskerdev.webidl.parser.IdlNamespace
import com.huskerdev.webidl.parser.IdlSetLike
import com.huskerdev.webidl.parser.IdlSetter
import com.huskerdev.webidl.parser.IdlStringifier
import com.huskerdev.webidl.parser.IdlType
import com.huskerdev.webidl.parser.IdlTypeDef

class IdlResolver(
    val root: IdlDefinitionRoot,
    env: WebIDLEnv = WebIDLEnv.Default
) {
    val builtinTypes: Map<String, BuiltinIdlDeclaration> = env.builtinTypes.mapValues {
        BuiltinIdlDeclaration(it.key, it.value)
    }

    val interfaces: Map<String, ResolvedIdlInterface> = linkedMapOf()

    private val mixins = linkedMapOf<String, ResolvedIdlInterface>()

    val dictionaries: Map<String, ResolvedIdlDictionary> = linkedMapOf()

    val enums: Map<String, ResolvedIdlEnum> = linkedMapOf()

    val typeDefs: Map<String, ResolvedIdlTypeDef> = linkedMapOf()

    val namespaces: Map<String, ResolvedIdlNamespace> = linkedMapOf()

    val callbacks: Map<String, ResolvedIdlCallbackFunction> = linkedMapOf()

    init {
        declareTypes()
        resolveTypeDefs()
        resolveDeclarationMembers()
        execExpands()
    }

    fun findDeclaration(name: String): ResolvedIdlDeclaration {
        return interfaces[name]
            ?: dictionaries[name]
            ?: callbacks[name]
            ?: enums[name]
            ?: typeDefs[name]
            ?: builtinTypes[name]
            ?: throw WebIDLResolverException("Type '$name' is not declared")
    }

    fun findType(type: IdlType): ResolvedIdlType = when(type) {
        is IdlType.Default -> {
            val declaration = findDeclaration(type.name)

            if(declaration is BuiltinIdlDeclaration && declaration.kind == WebIDLBuiltinKind.VOID)
                ResolvedIdlType.Void(declaration.name)
            else ResolvedIdlType.Default(
                declaration = declaration,
                parameters = type.parameters.map(::findType),
                isNullable = type.isNullable
            )
        }
        is IdlType.Union -> ResolvedIdlType.Union(
            types = type.types.map(::findType),
            isNullable = type.isNullable
        )
    }

    private fun declareTypes() {
        root.definitions.forEach { def ->
            when (def) {
                is IdlInterface if(!def.isPartial) ->
                    ((if(def.isMixin) mixins else interfaces) as MutableMap)[def.name]  = ResolvedIdlInterface(def.name, def.isCallback, def.attributes)
                is IdlDictionary ->
                    (dictionaries as MutableMap)[def.name] = ResolvedIdlDictionary(def.name, def.attributes)
                is IdlEnum ->
                    (enums as MutableMap)[def.name] = ResolvedIdlEnum(def.name, def.definitions.map { it.name }, def.attributes)
                is IdlNamespace ->
                    (namespaces as MutableMap)[def.name] = ResolvedIdlNamespace(def.name, def.attributes)
                is IdlTypeDef ->
                    (typeDefs as MutableMap)[def.name] = ResolvedIdlTypeDef(def.name, def.type, def.attributes)
                is IdlCallbackFunction ->
                    (callbacks as MutableMap)[def.name] = ResolvedIdlCallbackFunction(def.name, def.attributes)
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
                            if(this !is ResolvedIdlInterface)
                                throw WebIDLResolverException("Expected interface")
                            this
                        }
                    }

                    decl.definitions.forEach { decl ->
                        when(decl) {
                            is IdlField ->
                                (if(decl.isStatic) inter.staticFields else inter.fields) += decl.toDeclarationField()

                            is IdlOperation ->
                                (if(decl.isStatic) inter.staticOperations else inter.operations) += decl.toFunction()

                            is IdlConstructor ->
                                inter.constructors += ResolvedIdlConstructor(decl.args.map { it.toArgField() }, decl.attributes)

                            is IdlIterable -> {
                                inter.isIterable = true
                                inter.iterableType = findType(decl.keyType) to decl.valueType?.run { findType(this) }
                            }
                            is IdlAsyncIterableLike -> {
                                inter.isAsyncIterable = true
                                inter.asyncIterableType =
                                    findType(decl.keyType) to decl.valueType?.run { findType(this) }
                            }
                            is IdlMapLike -> {
                                inter.isMap = true
                                inter.isReadOnlyMap = decl.isReadOnly
                                inter.mapType = findType(decl.keyType) to findType(decl.valueType)
                            }
                            is IdlSetLike -> {
                                inter.isSet = true
                                inter.isReadOnlySet = decl.isReadOnly
                                inter.setType = findType(decl.type)
                            }
                            is IdlGetter ->
                                inter.getter = decl.operation.toFunction()

                            is IdlSetter ->
                                inter.setter = decl.operation.toFunction()

                            is IdlStringifier -> {
                                inter.stringifierEnabled = true
                                if(decl.field != null)
                                    inter.stringifier = decl.field.toDeclarationField()
                            }
                            else -> throw WebIDLResolverException("Unexpected definition")
                        }
                    }
                }
                is IdlDictionary -> {
                    val dict = dictionaries[decl.name]!!

                    if(decl.implements != null) {
                        dict.implements = findDeclaration(decl.implements).run {
                            if(this !is ResolvedIdlDictionary)
                                throw WebIDLResolverException("Expected dictionary")
                            this
                        }
                    }

                    decl.definitions.forEach { decl ->
                        when(decl) {
                            is IdlField ->
                                dict.fields += decl.toDeclarationField()
                            else -> throw WebIDLResolverException("Unexpected definition")
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
                                namespace.operations += decl.toFunction()
                            else -> throw WebIDLResolverException("Unexpected definition")
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
                        ?: throw WebIDLResolverException("Expected interface")
                    val source = interfaces[decl.source]
                        ?: throw WebIDLResolverException("Expected interface")
                    target.implements = source
                }
                is IdlIncludes -> {
                    val target = interfaces[decl.target]
                        ?: throw WebIDLResolverException("Expected interface")
                    val source = mixins[decl.source]
                        ?: throw WebIDLResolverException("Expected interface")
                    target.applyMixin(source)
                }
                else -> Unit
            }
        }
    }

    private fun IdlField.toDeclarationField(): ResolvedIdlField.Declaration {
        val type = findType(type)
        if(value != null && !type.canConsume(value))
            throw WebIDLResolverException("Incompatible field type and value: '${WebIDLPrinter.printValue(value)}'")
        return ResolvedIdlField.Declaration(
            name,
            type,
            value,
            isAttribute, isStatic, isReadOnly, isInherit, isConst, isRequired,
            attributes
        )
    }

    private fun IdlField.toArgField() = ResolvedIdlField.Argument(
        name,
        findType(type),
        value,
        isOptional, isVariadic,
        attributes
    )


    private fun IdlOperation.toFunction() = ResolvedIdlOperation(
        name,
        findType(type),
        args.map { it.toArgField() },
        isStatic,
        attributes
    )
}