package com.huskerdev.webidl

import com.huskerdev.webidl.ast.WebIDLBuiltin
import com.huskerdev.webidl.ast.WebIDLDeclaration
import com.huskerdev.webidl.ast.WebIDLDictionary
import com.huskerdev.webidl.ast.WebIDLEnum
import com.huskerdev.webidl.ast.WebIDLInterface
import com.huskerdev.webidl.ast.WebIDLNamespace
import com.huskerdev.webidl.ast.WebIDLTypeDef
import com.huskerdev.webidl.def.IdlDefinitionRoot
import com.huskerdev.webidl.def.IdlDictionaryDef
import com.huskerdev.webidl.def.IdlEnumDef
import com.huskerdev.webidl.def.IdlInterfaceDef
import com.huskerdev.webidl.def.IdlNamespaceDef
import com.huskerdev.webidl.def.IdlTypeDefDef

class WebIDL(
    val root: IdlDefinitionRoot,
    val env: WebIDLEnv = WebIDLEnv.Default
) {
    val builtinTypes: Map<String, WebIDLBuiltin> = env.builtinTypes.mapValues {
        WebIDLBuiltin(it.key, it.value)
    }

    val interfaces: Map<String, WebIDLInterface>
        field = linkedMapOf()

    val dictionaries: Map<String, WebIDLDictionary>
        field = linkedMapOf()

    val enums: Map<String, WebIDLEnum>
        field = linkedMapOf()

    val typeDefs: Map<String, WebIDLTypeDef>
        field = linkedMapOf()

    val namespaces: Map<String, WebIDLNamespace>
        field = linkedMapOf()

    constructor(
        iterable: Iterator<String>,
        env: WebIDLEnv = WebIDLEnv.Default
    ): this(WebIDLParser.parse(iterable), env)

    constructor(
        text: String,
        env: WebIDLEnv = WebIDLEnv.Default
    ): this(WebIDLParser.parse(text), env)

    constructor(
        lineSequence: Sequence<String>,
        env: WebIDLEnv = WebIDLEnv.Default
    ): this(WebIDLParser.parse(lineSequence), env)

    constructor(
        lines: Iterable<String>,
        env: WebIDLEnv = WebIDLEnv.Default
    ): this(WebIDLParser.parse(lines), env)

    init {
        iterateTypeDeclarations()
        resolveTypeDefs()
    }

    fun findDeclaration(name: String): WebIDLDeclaration {
        return interfaces[name]
            ?: dictionaries[name]
            ?: enums[name]
            ?: typeDefs[name]
            ?: builtinTypes[name]
            ?: throw UnsupportedOperationException("Type '$name' is not declared")
    }

    private fun iterateTypeDeclarations() {
        root.definitions.forEach { def ->
            when (def) {
                is IdlInterfaceDef if(!def.isMixin && !def.isPartial) ->
                    interfaces[def.name] = WebIDLInterface(def.name, def.isCallback)
                is IdlDictionaryDef ->
                    (dictionaries as MutableMap)[def.name] = WebIDLDictionary(def.name)
                is IdlEnumDef ->
                    (enums as MutableMap)[def.name] = WebIDLEnum(def.name)
                is IdlNamespaceDef ->
                    (namespaces as MutableMap)[def.name] = WebIDLNamespace(def.name)
                is IdlTypeDefDef ->
                    (typeDefs as MutableMap)[def.name] = WebIDLTypeDef(def.name)
                else -> return@forEach
            }
        }
    }

    private fun resolveTypeDefs() {
        typeDefs.values.forEach {
            it.resolve(this)
        }
    }
}