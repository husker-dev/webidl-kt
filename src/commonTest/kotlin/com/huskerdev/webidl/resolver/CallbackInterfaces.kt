package com.huskerdev.webidl.resolver

import com.huskerdev.webidl.WebIDL
import kotlin.test.Test


class CallbackInterfaces {

    @Test
    fun test1(){
        WebIDL.resolve("""
            callback interface identifier {
                void a();
            };
        """.trimIndent()).apply {
            assertInterface(interfaces.values.toList()[0], "identifier", isCallback = true) {
                assertOperation(operations[0], "a") {
                    assertDefaultType(type, "void")
                }
            }
        }
    }
}