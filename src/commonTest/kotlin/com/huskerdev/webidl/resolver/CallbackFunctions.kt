package com.huskerdev.webidl.resolver

import com.huskerdev.webidl.WebIDL
import kotlin.test.Test


class CallbackFunctions {

    @Test
    fun test1(){
        WebIDL.resolve("""
            callback identifier = long (/* arguments... */);
        """.trimIndent()).apply {
            assertCallbackFunction(callbacks.values.toList()[0], "identifier") {
                assertDefaultType(type, "long")
            }
        }
    }

    @Test
    fun test2(){
        WebIDL.resolve("""
            callback AsyncOperationCallback = undefined (DOMString status);

            [Exposed=Window]
            interface AsyncOperations {
                undefined performOperation(AsyncOperationCallback whenFinished);
            };
        """.trimIndent()).apply {
            assertCallbackFunction(callbacks.values.toList()[0], "AsyncOperationCallback") {
                assertDefaultType(type, "undefined")
                assertArgument(args[0], "status") {
                    assertDefaultType(type, "DOMString")
                }
            }
            assertInterface(interfaces.values.toList()[0], "AsyncOperations") {
                assertOperation(operations[0], "performOperation") {
                    assertDefaultType(type, "undefined")
                    assertArgument(args[0], "whenFinished") {
                        assertDefaultType(type, "AsyncOperationCallback")
                    }
                }
            }
        }
    }

}