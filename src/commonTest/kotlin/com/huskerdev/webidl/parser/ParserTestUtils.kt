package com.huskerdev.webidl.parser

import com.huskerdev.webidl.WebIDLPrinter
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull

@OptIn(ExperimentalContracts::class)
fun assertField(
    def: IdlDefinition,
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
    block: IdlField.() -> Unit = {}
) {
    contract { returns() implies (def is IdlField) }

    assertIs<IdlField>(def)

    assertEquals(name, def.name)
    assertEquals(type, WebIDLPrinter.printType(def.type))
    if(value != null) {
        assertNotNull(def.value)
        assertEquals(value, WebIDLPrinter.printValue(def.value))
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
fun assertOperation(
    def: IdlDefinition,
    name: String,
    type: String,
    argsCount: Int,
    isStatic: Boolean = false,
    attributes: Int = 0,
    block: IdlOperation.() -> Unit = {}
){
    contract { returns() implies (def is IdlOperation) }

    assertIs<IdlOperation>(def)

    assertEquals(name, def.name)
    assertEquals(type, WebIDLPrinter.printType(def.type))
    assertEquals(argsCount, def.args.size)
    assertEquals(isStatic, def.isStatic, "expected 'static'")

    assertEquals(attributes, def.attributes.size, "attributes mismatch")

    block(def)
}

@OptIn(ExperimentalContracts::class)
fun assertConstructor(
    def: IdlDefinition,
    argsCount: Int,
    attributes: Int = 0,
    block: IdlConstructor.() -> Unit = {}
){
    contract { returns() implies (def is IdlConstructor) }

    assertIs<IdlConstructor>(def)

    assertEquals(argsCount, def.args.size)
    assertEquals(attributes, def.attributes.size, "attributes mismatch")

    block(def)
}

@OptIn(ExperimentalContracts::class)
fun assertInterface(
    def: IdlDefinition,
    name: String,
    implements: String?,
    definitions: Int,
    attributes: Int = 0,
    isPartial: Boolean = false,
    isMixin: Boolean = false,
    isCallback: Boolean = false,
    block: IdlInterface.() -> Unit = {}
){
    contract { returns() implies (def is IdlInterface) }

    assertIs<IdlInterface>(def)

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
    def: IdlDefinition,
    name: String,
    implements: String?,
    definitions: Int,
    attributes: Int = 0,
    isPartial: Boolean = false,
    block: IdlDictionary.() -> Unit = {}
){
    contract { returns() implies (def is IdlDictionary) }

    assertIs<IdlDictionary>(def)

    assertEquals(name, def.name, "name mismatch")
    assertEquals(implements, def.implements, "implementation mismatch")
    assertEquals(definitions, def.definitions.size, "definitions mismatch")
    assertEquals(attributes, def.attributes.size, "attributes mismatch")
    assertEquals(isPartial, def.isPartial, "expected 'partial'")

    block(def)
}

@OptIn(ExperimentalContracts::class)
fun assertNamespace(
    def: IdlDefinition,
    name: String,
    definitions: Int,
    attributes: Int = 0,
    isPartial: Boolean = false,
    block: IdlNamespace.() -> Unit = {}
){
    contract { returns() implies (def is IdlNamespace) }

    assertIs<IdlNamespace>(def)

    assertEquals(name, def.name, "name mismatch")
    assertEquals(definitions, def.definitions.size, "definitions mismatch")
    assertEquals(attributes, def.attributes.size, "attributes mismatch")
    assertEquals(isPartial, def.isPartial, "expected 'partial'")

    block(def)
}

@OptIn(ExperimentalContracts::class)
fun assertEnum(
    def: IdlDefinition,
    name: String,
    elements: List<String>
){
    contract { returns() implies (def is IdlEnum) }

    assertIs<IdlEnum>(def)

    assertEquals(name, def.name, "name mismatch")
    assertContentEquals(elements, def.definitions.map { it.name })
}

@OptIn(ExperimentalContracts::class)
fun assertCallbackFunction(
    def: IdlDefinition,
    name: String,
    block: IdlCallbackFunction.() -> Unit = {}
){
    contract { returns() implies (def is IdlCallbackFunction) }

    assertIs<IdlCallbackFunction>(def)
    assertEquals(name, def.name, "name mismatch")

    block(def)
}

@OptIn(ExperimentalContracts::class)
fun assertAttribute(
    attr: IdlExtendedAttribute,
    name: String
){
    assertIs<IdlExtendedAttribute.NoArgs>(attr)
    assertEquals(name, attr.name)
}

@OptIn(ExperimentalContracts::class)
fun assertAttributeIdent(
    attr: IdlExtendedAttribute,
    name: String,
    identifier: String
){
    assertIs<IdlExtendedAttribute.IdentifierValue>(attr)
    assertEquals(name, attr.name)
    assertEquals(identifier, attr.identifier)
}

@OptIn(ExperimentalContracts::class)
fun assertIncludes(
    attr: IdlDefinition,
    target: String,
    source: String
){
    assertIs<IdlIncludes>(attr)
    assertEquals(target, attr.target)
    assertEquals(source, attr.source)
}

@OptIn(ExperimentalContracts::class)
fun assertIterable(
    attr: IdlDefinition,
    keyType: String,
    valueType: String? = null
){
    assertIs<IdlIterable>(attr)
    assertEquals(keyType, WebIDLPrinter.printType(attr.keyType))
    if(valueType != null) {
        assertNotNull(attr.valueType)
        assertEquals(valueType, WebIDLPrinter.printType(attr.valueType))
    }
}

@OptIn(ExperimentalContracts::class)
fun assertAsyncIterable(
    attr: IdlDefinition,
    keyType: String,
    valueType: String? = null
){
    assertIs<IdlAsyncIterableLike>(attr)
    assertEquals(keyType, WebIDLPrinter.printType(attr.keyType))
    if(valueType != null) {
        assertNotNull(attr.valueType)
        assertEquals(valueType, WebIDLPrinter.printType(attr.valueType))
    }
}

@OptIn(ExperimentalContracts::class)
fun assertMapLike(
    attr: IdlDefinition,
    keyType: String,
    valueType: String? = null,
    isReadOnly: Boolean
){
    assertIs<IdlMapLike>(attr)
    assertEquals(keyType, WebIDLPrinter.printType(attr.keyType))
    assertEquals(isReadOnly, attr.isReadOnly)
    if(valueType != null) {
        assertNotNull(attr.valueType)
        assertEquals(valueType, WebIDLPrinter.printType(attr.valueType))
    }
}

@OptIn(ExperimentalContracts::class)
fun assertSetLike(
    attr: IdlDefinition,
    type: String,
    isReadOnly: Boolean
){
    assertIs<IdlSetLike>(attr)
    assertEquals(type, WebIDLPrinter.printType(attr.type))
    assertEquals(isReadOnly, attr.isReadOnly)
}

@OptIn(ExperimentalContracts::class)
fun assertTypedef(
    attr: IdlDefinition,
    type: String,
    identifier: String
){
    assertIs<IdlTypeDef>(attr)
    assertEquals(type, WebIDLPrinter.printType(attr.type))
    assertEquals(identifier, attr.name)
}

@OptIn(ExperimentalContracts::class)
fun assertStringifier(
    def: IdlDefinition,
    block: IdlStringifier.() -> Unit = {}
){
    assertIs<IdlStringifier>(def)
    block(def)
}

@OptIn(ExperimentalContracts::class)
fun assertGetter(
    def: IdlDefinition,
    block: IdlGetter.() -> Unit = {}
){
    assertIs<IdlGetter>(def)
    block(def)
}

@OptIn(ExperimentalContracts::class)
fun assertSetter(
    def: IdlDefinition,
    block: IdlSetter.() -> Unit = {}
){
    assertIs<IdlSetter>(def)
    block(def)
}