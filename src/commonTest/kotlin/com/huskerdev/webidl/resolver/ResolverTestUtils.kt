package com.huskerdev.webidl.resolver

import com.huskerdev.webidl.WebIDLPrinter
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull


fun assertTypedef(
    typeDef: ResolvedIdlTypeDef,
    name: String,
    block: ResolvedIdlTypeDef.() -> Unit = {}
){
    assertEquals(name, typeDef.name)
    block(typeDef)
}

fun assertDefaultType(
    type: ResolvedIdlType,
    name: String,
    isNullable: Boolean = false,
    parameters: Int = 0,
) {
    assertIs<ResolvedIdlType.Default>(type)
    assertEquals(parameters, type.parameters.size)
    assertEquals(isNullable, type.isNullable)
    assertEquals(name, WebIDLPrinter.printResolvedType(type))
}

fun assertInterface(
    inter: ResolvedIdlInterface,
    name: String,
    implements: ResolvedIdlInterface? = null,
    isCallback: Boolean = false,
    block: ResolvedIdlInterface.() -> Unit = {}
) {
    assertEquals(name, inter.name)
    assertEquals(implements, inter.implements)
    assertEquals(isCallback, inter.isCallback)
    block(inter)
}

fun assertNamespace(
    namespace: ResolvedIdlNamespace,
    name: String,
    block: ResolvedIdlNamespace.() -> Unit = {}
) {
    assertEquals(name, namespace.name)
    block(namespace)
}

fun assertDictionary(
    dictionary: ResolvedIdlDictionary,
    name: String,
    implements: ResolvedIdlDictionary? = null,
    block: ResolvedIdlDictionary.() -> Unit = {}
) {
    assertEquals(name, dictionary.name)
    assertEquals(implements, dictionary.implements)
    block(dictionary)
}

fun assertEnum(
    en: ResolvedIdlEnum,
    name: String,
    elements: List<String>,
    block: ResolvedIdlEnum.() -> Unit = {}
) {
    assertEquals(name, en.name)
    assertEquals(elements, en.elements)
    block(en)
}

fun assertCallbackFunction(
    en: ResolvedIdlCallbackFunction,
    name: String,
    block: ResolvedIdlCallbackFunction.() -> Unit = {}
) {
    assertEquals(name, en.name)
    block(en)
}

fun assertField(
    field: ResolvedIdlField.Declaration,
    name: String,
    value: String? = null,
    isAttribute: Boolean = false,
    isStatic: Boolean = false,
    isReadOnly: Boolean = false,
    isInherit: Boolean = false,
    isConst: Boolean = false,
    isRequired: Boolean = false,
    block: ResolvedIdlField.Declaration.() -> Unit = {}
) {
    assertEquals(name, field.name)
    if(value != null) {
        assertNotNull(field.value)
        assertEquals(value, WebIDLPrinter.printValue(field.value!!))
    } else assertNull(field.value)

    assertEquals(isAttribute, field.isAttribute)
    assertEquals(isStatic, field.isStatic)
    assertEquals(isReadOnly, field.isReadOnly)
    assertEquals(isInherit, field.isInherit)
    assertEquals(isConst, field.isConst)
    assertEquals(isRequired, field.isRequired)
    block(field)
}

fun assertArgument(
    field: ResolvedIdlField.Argument,
    name: String,
    value: String? = null,
    isOptional: Boolean = false,
    isVariadic: Boolean = false,
    block: ResolvedIdlField.Argument.() -> Unit = {}
) {
    assertEquals(name, field.name)
    if(value != null) {
        assertNotNull(field.value)
        assertEquals(value, WebIDLPrinter.printValue(field.value!!))
    } else assertNull(field.value)

    assertEquals(isOptional, field.isOptional)
    assertEquals(isVariadic, field.isVariadic)
    block(field)
}

fun assertOperation(
    operation: ResolvedIdlOperation,
    name: String,
    isStatic: Boolean = false,
    block: ResolvedIdlOperation.() -> Unit = {}
) {
    assertEquals(name, operation.name)
    assertEquals(isStatic, operation.isStatic)
    block(operation)
}

fun assertConstructor(
    operation: ResolvedIdlConstructor,
    block: ResolvedIdlConstructor.() -> Unit = {}
) {
    block(operation)
}