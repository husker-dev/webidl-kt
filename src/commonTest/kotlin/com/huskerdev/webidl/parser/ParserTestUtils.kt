package com.huskerdev.webidl.parser

import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull

@OptIn(ExperimentalContracts::class)
fun assertField(
    def: WebIDLDefinition,
    name: String,
    type: String,
    value: String? = null,
    attributes: Int = 0,
    isAttribute: Boolean = false,
    isStatic: Boolean = false,
    isReadOnly: Boolean = false,
    isInherit: Boolean = false,
    isOptional: Boolean = false,
    isConst: Boolean = false,
    isVariadic: Boolean = false,
    isRequired: Boolean = false,
    block: WebIDLFieldDef.() -> Unit = {}
) {
    contract { returns() implies (def is WebIDLFieldDef) }

    assertIs<WebIDLFieldDef>(def)

    assertEquals(name, def.name)
    assertEquals(type, def.type.toString())
    if(value != null) {
        assertNotNull(def.value)
        assertEquals(value, def.value.toString())
    }
    assertEquals(attributes, def.attributes.size, "attributes mismatch")

    assertEquals(isAttribute, def.isAttribute, "expected 'attribute'")
    assertEquals(isStatic, def.isStatic, "expected 'static'")
    assertEquals(isReadOnly, def.isReadOnly, "expected 'readonly'")
    assertEquals(isInherit, def.isInherit, "expected 'inherit'")
    assertEquals(isOptional, def.isOptional, "expected 'optional'")
    assertEquals(isConst, def.isConst, "expected 'const'")
    assertEquals(isRequired, def.isRequired, "expected 'required'")
    assertEquals(isVariadic, def.isVariadic, "expected '...'")

    block(def)
}

@OptIn(ExperimentalContracts::class)
fun assertFunction(
    def: WebIDLDefinition,
    name: String,
    type: String,
    argsCount: Int,
    isStatic: Boolean = false,
    attributes: Int = 0,
    block: WebIDLFunctionDef.() -> Unit = {}
){
    contract { returns() implies (def is WebIDLFunctionDef) }

    assertIs<WebIDLFunctionDef>(def)

    assertEquals(name, def.name)
    assertEquals(type, def.type.toString())
    assertEquals(argsCount, def.args.size)
    assertEquals(isStatic, def.isStatic, "expected 'static'")

    assertEquals(attributes, def.attributes.size, "attributes mismatch")

    block(def)
}

@OptIn(ExperimentalContracts::class)
fun assertConstructor(
    def: WebIDLDefinition,
    argsCount: Int,
    attributes: Int = 0,
    block: WebIDLConstructorDef.() -> Unit = {}
){
    contract { returns() implies (def is WebIDLConstructorDef) }

    assertIs<WebIDLConstructorDef>(def)

    assertEquals(argsCount, def.args.size)
    assertEquals(attributes, def.attributes.size, "attributes mismatch")

    block(def)
}

@OptIn(ExperimentalContracts::class)
fun assertInterface(
    def: WebIDLDefinition,
    name: String,
    implements: String?,
    definitions: Int,
    attributes: Int = 0,
    isPartial: Boolean = false,
    isMixin: Boolean = false,
    isCallback: Boolean = false,
    block: WebIDLInterfaceDef.() -> Unit = {}
){
    contract { returns() implies (def is WebIDLInterfaceDef) }

    assertIs<WebIDLInterfaceDef>(def)

    assertEquals(name, def.name, "name mismatch")
    assertEquals(implements, def.implements, "implementation mismatch")
    assertEquals(definitions, def.definitions.size, "definitions mismatch")
    assertEquals(attributes, def.attributes.size, "attributes mismatch")
    assertEquals(isPartial, def.isPartial, "expected 'partial'")
    assertEquals(isMixin, def.isMixin, "expected 'mixin'")
    assertEquals(isCallback, def.isCallback, "expected 'callback'")

    block(def)
}

@OptIn(ExperimentalContracts::class)
fun assertDictionary(
    def: WebIDLDefinition,
    name: String,
    implements: String?,
    definitions: Int,
    attributes: Int = 0,
    isPartial: Boolean = false,
    block: WebIDLDictionaryDef.() -> Unit = {}
){
    contract { returns() implies (def is WebIDLDictionaryDef) }

    assertIs<WebIDLDictionaryDef>(def)

    assertEquals(name, def.name, "name mismatch")
    assertEquals(implements, def.implements, "implementation mismatch")
    assertEquals(definitions, def.definitions.size, "definitions mismatch")
    assertEquals(attributes, def.attributes.size, "attributes mismatch")
    assertEquals(isPartial, def.isPartial, "expected 'partial'")

    block(def)
}

@OptIn(ExperimentalContracts::class)
fun assertNamespace(
    def: WebIDLDefinition,
    name: String,
    definitions: Int,
    attributes: Int = 0,
    isPartial: Boolean = false,
    block: WebIDLNamespaceDef.() -> Unit = {}
){
    contract { returns() implies (def is WebIDLNamespaceDef) }

    assertIs<WebIDLNamespaceDef>(def)

    assertEquals(name, def.name, "name mismatch")
    assertEquals(definitions, def.definitions.size, "definitions mismatch")
    assertEquals(attributes, def.attributes.size, "attributes mismatch")
    assertEquals(isPartial, def.isPartial, "expected 'partial'")

    block(def)
}

@OptIn(ExperimentalContracts::class)
fun assertEnum(
    def: WebIDLDefinition,
    name: String,
    elements: List<String>
){
    contract { returns() implies (def is WebIDLEnumDef) }

    assertIs<WebIDLEnumDef>(def)

    assertEquals(name, def.name, "name mismatch")
    assertContentEquals(elements, def.elements)
}

@OptIn(ExperimentalContracts::class)
fun assertCallbackFunction(
    def: WebIDLDefinition,
    name: String,
    block: WebIDLCallbackFunctionDef.() -> Unit = {}
){
    contract { returns() implies (def is WebIDLCallbackFunctionDef) }

    assertIs<WebIDLCallbackFunctionDef>(def)
    assertEquals(name, def.name, "name mismatch")

    block(def)
}

@OptIn(ExperimentalContracts::class)
fun assertAttribute(
    attr: WebIDLExtendedAttributeDef,
    name: String
){
    assertIs<WebIDLExtendedAttributeDefNoArgs>(attr)
    assertEquals(name, attr.name)
}

@OptIn(ExperimentalContracts::class)
fun assertAttributeIdent(
    attr: WebIDLExtendedAttributeDef,
    name: String,
    identifier: String
){
    assertIs<WebIDLExtendedAttributeDefIdent>(attr)
    assertEquals(name, attr.name)
    assertEquals(identifier, attr.identifier)
}

@OptIn(ExperimentalContracts::class)
fun assertIncludes(
    attr: WebIDLDefinition,
    target: String,
    source: String
){
    assertIs<WebIDLIncludesDef>(attr)
    assertEquals(target, attr.target)
    assertEquals(source, attr.source)
}

@OptIn(ExperimentalContracts::class)
fun assertIterable(
    attr: WebIDLDefinition,
    keyType: String,
    valueType: String? = null
){
    assertIs<WebIDLIterableDef>(attr)
    assertEquals(keyType, attr.keyType.toString())
    if(valueType != null) {
        assertNotNull(attr.valueType)
        assertEquals(valueType, attr.valueType.toString())
    }
}

@OptIn(ExperimentalContracts::class)
fun assertAsyncIterable(
    attr: WebIDLDefinition,
    keyType: String,
    valueType: String? = null
){
    assertIs<WebIDLAsyncIterableLikeDef>(attr)
    assertEquals(keyType, attr.keyType.toString())
    if(valueType != null) {
        assertNotNull(attr.valueType)
        assertEquals(valueType, attr.valueType.toString())
    }
}

@OptIn(ExperimentalContracts::class)
fun assertMapLike(
    attr: WebIDLDefinition,
    keyType: String,
    valueType: String? = null,
    isReadOnly: Boolean
){
    assertIs<WebIDLMapLikeDef>(attr)
    assertEquals(keyType, attr.keyType.toString())
    assertEquals(isReadOnly, attr.isReadOnly)
    if(valueType != null) {
        assertNotNull(attr.valueType)
        assertEquals(valueType, attr.valueType.toString())
    }
}

@OptIn(ExperimentalContracts::class)
fun assertSetLike(
    attr: WebIDLDefinition,
    type: String,
    isReadOnly: Boolean
){
    assertIs<WebIDLSetLikeDef>(attr)
    assertEquals(type, attr.type.toString())
    assertEquals(isReadOnly, attr.isReadOnly)
}

@OptIn(ExperimentalContracts::class)
fun assertTypedef(
    attr: WebIDLDefinition,
    type: String,
    identifier: String
){
    assertIs<WebIDLTypeDefDef>(attr)
    assertEquals(type, attr.type.toString())
    assertEquals(identifier, attr.name)
}

@OptIn(ExperimentalContracts::class)
fun assertStringifier(
    def: WebIDLDefinition,
    block: WebIDLStringifierDef.() -> Unit = {}
){
    assertIs<WebIDLStringifierDef>(def)
    block(def)
}

@OptIn(ExperimentalContracts::class)
fun assertGetter(
    def: WebIDLDefinition,
    block: WebIDLGetterDef.() -> Unit = {}
){
    assertIs<WebIDLGetterDef>(def)
    block(def)
}

@OptIn(ExperimentalContracts::class)
fun assertSetter(
    def: WebIDLDefinition,
    block: WebIDLSetterDef.() -> Unit = {}
){
    assertIs<WebIDLSetterDef>(def)
    block(def)
}