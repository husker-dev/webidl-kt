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
        WebIDL.parseDefinitions("""
            |interface A {
            |    ${types.joinToString("\n|    ") { "$it a();" }}
            |};
        """.trimMargin("|")).first.apply {
            assertEquals(1, this.definitions.size)

            assertInterface(
                this.definitions[0],
                name = "A",
                implements = null,
                definitions = 27
            ) {
                types.forEachIndexed { index, string ->
                    assertOperation(this.definitions[index], "a", string, 0)
                }
            }
        }
    }
}