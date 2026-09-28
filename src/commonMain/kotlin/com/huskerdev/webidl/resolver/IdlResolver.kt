package com.huskerdev.webidl.resolver

import com.huskerdev.webidl.*
import com.huskerdev.webidl.parser.*

class IdlResolver(
    val root: IdlRoot,
    val parserErrors: List<WebIDLSyntaxErrorException>,
    val env: WebIDLEnv = WebIDLEnv.Default
) {

    private val builtinTypes = env.builtinTypes.mapValues {
        BuiltinIdlDeclaration(it.key, it.value)
    }

    private val collectedInterfaces = linkedMapOf<String, ResolvedIdlInterface>()
    private val collectedMixins = linkedMapOf<String, ResolvedIdlInterface>()
    private val collectedDictionaries = linkedMapOf<String, ResolvedIdlDictionary>()
    private val collectedEnums = linkedMapOf<String, ResolvedIdlEnum>()
    private val collectedTypeDefs = linkedMapOf<String, ResolvedIdlTypeDef>()
    private val collectedNamespaces = linkedMapOf<String, ResolvedIdlNamespace>()
    private val collectedCallbacks = linkedMapOf<String, ResolvedIdlCallbackFunction>()

    val interfaces: Map<String, ResolvedIdlInterface> = collectedInterfaces
    val dictionaries: Map<String, ResolvedIdlDictionary> = collectedDictionaries
    val enums: Map<String, ResolvedIdlEnum> = collectedEnums
    val typeDefs: Map<String, ResolvedIdlTypeDef> = collectedTypeDefs
    val namespaces: Map<String, ResolvedIdlNamespace> = collectedNamespaces
    val callbacks: Map<String, ResolvedIdlCallbackFunction> = collectedCallbacks

    // Errors

    private val collectedTypeErrors = arrayListOf<WebIDLTypeErrorException>()
    val typeErrors: List<WebIDLTypeErrorException> = collectedTypeErrors

    val mergedErrors: List<WebIDLErrorException>

    init {
        declareTypes()
        resolveTypeDefs()
        resolveDeclarationMembers()
        execExpands()

        mergedErrors = parserErrors + typeErrors
    }

    fun findDeclaration(callerBounds: IdlElementBounds, name: String): ResolvedIdlDeclaration {
        return interfaces[name]
            ?: dictionaries[name]
            ?: callbacks[name]
            ?: enums[name]
            ?: typeDefs[name]
            ?: builtinTypes[name]
            ?: run {
                collectedTypeErrors += WebIDLUnresolvedReferenceException(callerBounds, name)
                BuiltinIdlDeclaration("undefined", WebIDLBuiltinKind.VOID)
            }
    }

    fun findType(type: IdlType): ResolvedIdlType = when(type) {
        is IdlType.Default -> {
            val declaration = findDeclaration(type.bounds, type.name.text)

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
        fun <T> putTypeDeclarationInto(map: MutableMap<String, T>, element: T, name: IdlName) {
            val nameText = name.text
            if(nameText in collectedMixins ||
                nameText in collectedInterfaces ||
                nameText in collectedDictionaries ||
                nameText in collectedEnums ||
                nameText in collectedNamespaces ||
                nameText in collectedTypeDefs ||
                nameText in collectedCallbacks
            ) collectedTypeErrors += WebIDLTypeErrorException(name.bounds, "Type with name '$nameText' is already defined.")
            else map[nameText] = element
        }

        root.definitions.forEach { def ->
            when (def) {
                is IdlInterface if(!def.isPartial) -> putTypeDeclarationInto(
                    map = (if(def.isMixin) collectedMixins else collectedInterfaces),
                    element = ResolvedIdlInterface(def.name.text, def.isCallback, def.attributes, def.bounds),
                    name = def.name
                )
                is IdlDictionary -> putTypeDeclarationInto(
                    map = collectedDictionaries,
                    element = ResolvedIdlDictionary(def.name.text, def.attributes, def.bounds),
                    name = def.name
                )
                is IdlEnum -> putTypeDeclarationInto(
                    map = collectedEnums,
                    element = ResolvedIdlEnum(
                        name = def.name.text,
                        elements = buildSet {
                            def.elements.forEach {
                                if(it.name.text in this)
                                    collectedTypeErrors += WebIDLTypeErrorException(it.name.bounds, "Element '${it.name.text}' is already defined.")
                                else add(it.name.text)
                            }
                        }.toList(),
                        attributes = def.attributes,
                        bounds = def.bounds
                    ),
                    name = def.name
                )
                is IdlNamespace -> putTypeDeclarationInto(
                    map = collectedNamespaces,
                    element = ResolvedIdlNamespace(def.name.text, def.attributes, def.bounds),
                    name = def.name
                )
                is IdlTypeDef -> putTypeDeclarationInto(
                    map = collectedTypeDefs,
                    element = ResolvedIdlTypeDef(def.name.text, def.type, def.attributes, def.bounds),
                    name = def.name
                )
                is IdlCallbackFunction -> putTypeDeclarationInto(
                    map = collectedCallbacks,
                    element = ResolvedIdlCallbackFunction(def.name.text, def.attributes, def.bounds),
                    name = def.name
                )
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
                        collectedMixins[decl.name.text]!! else collectedInterfaces[decl.name.text]!!

                    if(decl.implements != null) {
                        val implementsDeclaration = findDeclaration(decl.implements.bounds, decl.implements.text)
                        if(implementsDeclaration is ResolvedIdlInterface)
                            inter.implements = implementsDeclaration
                        else collectedTypeErrors += WebIDLTypeErrorException(decl.implements.bounds, "Expected interface.")
                    }

                    decl.definitions.forEach { decl ->
                        when(decl) {
                            is IdlField -> {
                                val name = decl.name.text
                                if(inter.staticFields.any { it.name == name } || inter.fields.any { it.name == name }) {
                                    collectedTypeErrors += WebIDLTypeErrorException(decl.name.bounds, "Field '$name' is already defined.")
                                    return@forEach
                                }
                                (if (decl.isStatic) inter.staticFields else inter.fields) += decl.toDeclarationField()
                            }

                            is IdlOperation -> {
                                val name = decl.name.text
                                if(!env.overloadingSupport && (
                                        inter.staticOperations.any { it.name == name } ||
                                        inter.operations.any { it.name == name }
                                )) {
                                    collectedTypeErrors += WebIDLTypeErrorException(decl.name.bounds, "Operation '$name' is already defined.")
                                    return@forEach
                                }

                                (if (decl.isStatic) inter.staticOperations else inter.operations) += decl.toFunction()
                            }

                            is IdlConstructor -> {
                                val args = decl.args.toArguments()
                                if(inter.constructors.any { it.args == args }) {
                                    collectedTypeErrors += WebIDLTypeErrorException(decl.bounds, "Constructor with the same arguments is already defined.")
                                    return@forEach
                                }

                                inter.constructors += ResolvedIdlConstructor(
                                    args = args,
                                    attributes = decl.attributes,
                                    bounds = decl.bounds
                                )
                            }

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
                            else -> collectedTypeErrors += WebIDLTypeErrorException(decl.bounds, "Unexpected definition.")
                        }
                    }
                }
                is IdlDictionary -> {
                    val dict = collectedDictionaries[decl.name.text]!!

                    if(decl.implements != null) {
                        val implementsDeclaration = findDeclaration(decl.implements.bounds, decl.implements.text)

                        if(implementsDeclaration is ResolvedIdlDictionary)
                            dict.implements = implementsDeclaration
                        else collectedTypeErrors += WebIDLTypeErrorException(decl.implements.bounds, "Expected dictionary.")
                    }

                    decl.definitions.forEach { decl ->
                        when(decl) {
                            is IdlField -> {
                                val name = decl.name.text
                                if(dict.fields.any { it.name == name }) {
                                    collectedTypeErrors += WebIDLTypeErrorException(decl.name.bounds, "Field '$name' is already defined.")
                                    return@forEach
                                }
                                dict.fields += decl.toDeclarationField()
                            }
                            else -> collectedTypeErrors += WebIDLTypeErrorException(decl.bounds, "Unexpected definition.")
                        }
                    }
                }
                is IdlCallbackFunction -> {
                    val callback = collectedCallbacks[decl.name.text]!!
                    callback.type = findType(decl.operation.type)
                    callback.args = decl.operation.args.toArguments()
                }
                is IdlNamespace -> {
                    val namespace = collectedNamespaces[decl.name.text]!!

                    decl.definitions.forEach { decl ->
                        when(decl) {
                            is IdlField -> {
                                val name = decl.name.text
                                if(namespace.fields.any { it.name == name }) {
                                    collectedTypeErrors += WebIDLTypeErrorException(decl.name.bounds, "Field '$name' is already defined.")
                                    return@forEach
                                }
                                namespace.fields += decl.toDeclarationField()
                            }
                            is IdlOperation -> {
                                val name = decl.name.text
                                if(namespace.operations.any { it.name == name }) {
                                    collectedTypeErrors += WebIDLTypeErrorException(decl.name.bounds, "Operation '$name' is already defined.")
                                    return@forEach
                                }
                                namespace.operations += decl.toFunction()
                            }
                            else -> collectedTypeErrors += WebIDLTypeErrorException(decl.bounds, "Unexpected definition.")
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
                    val target = collectedInterfaces[decl.target.text]
                        ?: run {
                            collectedTypeErrors += WebIDLTypeErrorException(decl.target.bounds, "Expected interface.")
                            return@forEach
                        }
                    val source = collectedInterfaces[decl.source.text]
                        ?: run {
                            collectedTypeErrors += WebIDLTypeErrorException(decl.target.bounds, "Expected interface.")
                            return@forEach
                        }
                    target.implements = source
                }
                is IdlIncludes -> {
                    val target = collectedInterfaces[decl.target.text]
                        ?: run {
                            collectedTypeErrors += WebIDLTypeErrorException(decl.target.bounds, "Expected interface.")
                            return@forEach
                        }
                    val source = collectedMixins[decl.source.text]
                        ?: run {
                            collectedTypeErrors += WebIDLTypeErrorException(decl.target.bounds, "Expected interface.")
                            return@forEach
                        }
                    target.applyMixin(source)
                }
                else -> Unit
            }
        }
    }

    private fun IdlField.toDeclarationField(): ResolvedIdlField.Declaration {
        val type = findType(type)
        if(value != null && !type.canConsume(value)) {
            collectedTypeErrors += WebIDLTypeErrorException(
                value.bounds,
                "Incompatible field type and value: '${WebIDLPrinter.printValue(value)}'."
            )
        }
        return ResolvedIdlField.Declaration(
            name.text,
            type,
            value,
            isAttribute, isStatic, isReadOnly, isInherit, isConst, isRequired,
            attributes,
            bounds
        )
    }

    private fun IdlField.toArgField() = ResolvedIdlField.Argument(
        name.text,
        findType(type),
        value,
        isOptional, isVariadic,
        attributes,
        bounds
    )

    private fun IdlOperation.toFunction() = ResolvedIdlOperation(
        name.text,
        findType(type),
        args.toArguments(),
        isStatic,
        attributes,
        bounds
    )

    private fun List<IdlField>.toArguments() = buildList {
        val names = hashSetOf<String>()
        this@toArguments.forEach {
            if(it.name.text in names) {
                collectedTypeErrors += WebIDLTypeErrorException(
                    it.name.bounds,
                    "Argument '${it.name.text}' is already defined."
                )
            }
            names += it.name.text
            add(it.toArgField())
        }
    }
}