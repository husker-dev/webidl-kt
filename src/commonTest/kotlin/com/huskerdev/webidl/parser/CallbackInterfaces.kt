package com.huskerdev.webidl.parser

import com.huskerdev.webidl.WebIDL
import kotlin.test.Test
import kotlin.test.assertEquals


class CallbackInterfaces {

    @Test
    fun test1(){
        WebIDL.parseDefinitions("""
            callback interface identifier {
                void a();
            };
        """.trimIndent()).first.apply {
            assertEquals(1, this.definitions.size)

            assertInterface(
                this.definitions[0],
                name = "identifier",
                implements = null,
                definitions = 1,
                isCallback = true
            ) {
                assertOperation(this.definitions[0], "a", "void", argsCount = 0)
            }
        }
    }
}