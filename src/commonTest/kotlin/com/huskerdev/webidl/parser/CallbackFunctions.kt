package com.huskerdev.webidl.parser

import com.huskerdev.webidl.WebIDL
import kotlin.test.Test
import kotlin.test.assertEquals


class CallbackFunctions {

    @Test
    fun test1(){
        WebIDL.parseDefinitions("""
            callback identifier = int (/* arguments... */);
        """.trimIndent()).apply {
            assertEquals(definitions.size, 1)

            assertCallbackFunction(definitions[0], "identifier") {
                assertFunction(function, "", "int", argsCount = 0)
            }
        }
    }

    @Test
    fun test2(){
        WebIDL.parseDefinitions("""
            callback AsyncOperationCallback = undefined (DOMString status);

            [Exposed=Window]
            interface AsyncOperations {
                undefined performOperation(AsyncOperationCallback whenFinished);
            };
        """.trimIndent()).apply {
            assertEquals(definitions.size, 2)

            assertCallbackFunction(definitions[0], "AsyncOperationCallback") {
                assertFunction(function, "", "undefined", argsCount = 1) {
                    assertField(args[0], "status", "DOMString")
                }
            }

            assertInterface(definitions[1],
                name = "AsyncOperations",
                implements = null,
                definitions = 1,
                attributes = 1
            ) {
                assertAttributeIdent(attributes[0], "Exposed", "Window")

                assertFunction(definitions[0], "performOperation", "undefined", argsCount = 1) {
                    assertField(args[0], "whenFinished", "AsyncOperationCallback")
                }
            }
        }
    }

}