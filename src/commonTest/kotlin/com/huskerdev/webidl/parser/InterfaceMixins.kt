package com.huskerdev.webidl.parser

import com.huskerdev.webidl.WebIDL
import kotlin.test.Test
import kotlin.test.assertEquals


class InterfaceMixins {

    @Test
    fun test1(){
        WebIDL.parseDefinitions("""
            interface mixin SomeMixin {
                /* mixin_members... */
            };
            
            partial interface mixin SomeMixin {
                /* mixin_members... */
            };
        """.trimIndent()).apply {
            assertEquals(definitions.size, 2)

            assertInterface(definitions[0],
                name = "SomeMixin",
                implements = null,
                definitions = 0,
                isMixin = true
            )
            assertInterface(definitions[1],
                name = "SomeMixin",
                implements = null,
                definitions = 0,
                isPartial = true,
                isMixin = true
            )
        }
    }

    @Test
    fun test2(){
        WebIDL.parseDefinitions("""
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
        """.trimIndent()).apply {
            assertEquals(definitions.size, 3)

            assertInterface(definitions[0],
                name = "Entry",
                implements = null,
                definitions = 1
            ) {
                assertField(definitions[0], "entryType", "short", isReadOnly = true, isAttribute = true)
            }
            assertInterface(definitions[1],
                name = "Observable",
                implements = null,
                definitions = 1,
                isMixin = true
            ) {
                assertFunction(definitions[0], "addEventListener", "undefined", argsCount = 3) {
                    assertField(args[0], "type", "DOMString")
                    assertField(args[1], "listener", "EventListener")
                    assertField(args[2], "useCapture", "boolean")
                }
            }
            assertIncludes(definitions[2], "Entry", "Observable")
        }
    }
}