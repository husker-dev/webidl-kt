package com.huskerdev.webidl.resolver

import com.huskerdev.webidl.WebIDL
import com.huskerdev.webidl.WebIDLPrinter
import kotlin.test.Test
import kotlin.test.assertEquals


class Types {

    @Test
    fun test1(){
        val types = setOf(
            "void",
            "any",
            "undefined",
            "sequence<long>",
            "FrozenArray<long>",
            "record<long, long long>",
            "Promise<long long>",
            "boolean",
            "byte",
            "octet",
            "short",
            "unsigned short",
            "long",
            "unsigned long",
            "long long",
            "unsigned long long",
            "float",
            "unrestricted float",
            "double",
            "unrestricted double",
            "bigint",
            "DOMString",
            "ByteString",
            "USVString",
            "object",
            "symbol",
            "(long or DOMString)"
        )
        WebIDL.resolve("""
            |interface A {
            |    ${types.joinToString("\n|    ") { "$it a();" }}
            |};
        """.trimMargin("|")).apply {

            assertInterface(interfaces.values.first(), "A") {
                types.forEachIndexed { index, string ->
                    assertOperation(operations[index], "a") {
                        assertEquals(string, WebIDLPrinter.printResolvedType(type))
                    }
                }
            }
        }
    }
}