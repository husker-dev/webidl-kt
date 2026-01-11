package com.huskerdev.webidl.resolver

import com.huskerdev.webidl.WebIDL
import kotlin.test.Test
import kotlin.test.assertEquals


class InterfaceMixins {

    @Test
    fun test1(){
        WebIDL.resolve("""
            interface mixin SomeMixin {
                /* mixin_members... */
            };
            
            partial interface mixin SomeMixin {
                /* mixin_members... */
            };
        """.trimIndent()).apply {
            assertEquals(0, interfaces.size)
        }
    }

    @Test
    fun test2(){
        WebIDL.resolve("""
            interface Entry {
                readonly attribute short entryType;
                // ...
            };
            
            interface mixin Observable {
                undefined addEventListener(DOMString type,
                                           EventListener listener,
                                           boolean useCapture);
                // ...
            };
            
            Entry includes Observable;
            
            // for resolver
            interface EventListener {};
        """.trimIndent()).apply {
            assertEquals(2, interfaces.size)

            assertInterface(interfaces.values.toList()[0], "Entry") {
                assertField(fields[0], "entryType", isReadOnly = true, isAttribute = true) {
                    assertDefaultType(type, "short")
                }
                assertOperation(operations[0], "addEventListener") {
                    assertDefaultType(type, "undefined")
                    assertArgument(args[0], "type") {
                        assertDefaultType(type, "DOMString")
                    }
                    assertArgument(args[1], "listener") {
                        assertDefaultType(type, "EventListener")
                    }
                    assertArgument(args[2], "useCapture") {
                        assertDefaultType(type, "boolean")
                    }
                }
            }
        }
    }
}