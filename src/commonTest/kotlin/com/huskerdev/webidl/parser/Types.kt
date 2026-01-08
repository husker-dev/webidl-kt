package com.huskerdev.webidl.parser

import com.huskerdev.webidl.WebIDL
import kotlin.test.Test
import kotlin.test.assertEquals


class Types {

    @Test
    fun test1(){
        val types = setOf(
            "void",
            "any",
            "undefined",
            "sequence<int>",
            "FrozenArray<int>",
            "record<int, long long>",
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
            "symbol"
        )
        WebIDL.parseDefinitions("""
            |interface A {
            |    ${types.joinToString("\n|    ") { "$it a();" }}
            |};
        """.trimMargin("|")).apply {
            assertEquals(definitions.size, 1)

            assertInterface(definitions[0],
                name = "A",
                implements = null,
                definitions = 26
            ) {
                types.forEachIndexed { index, string ->
                    assertOperation(definitions[index], "a", string, 0)
                }
            }
        }
    }
}