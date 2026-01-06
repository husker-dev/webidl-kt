package com.huskerdev.webidl.parser

import com.huskerdev.webidl.WebIDL
import kotlin.test.Test
import kotlin.test.assertEquals


class Interfaces {

    @Test
    fun test1(){
        WebIDL.parseDefinitions("""
            [Exposed=Window]
            interface A {
                undefined f();
                undefined g();
            };
            
            [Exposed=Window]
            interface B : A {
                undefined f();
                undefined g(DOMString x);
            };
        """.trimIndent()).apply {
            assertEquals(definitions.size, 2)

            assertInterface(definitions[0],
                name = "A",
                implements = null,
                definitions = 2,
                attributes = 1
            ) {
                assertAttributeIdent(attributes[0], "Exposed", "Window")
                assertFunction(definitions[0], "f", "undefined", argsCount = 0)
                assertFunction(definitions[1], "g", "undefined", argsCount = 0)
            }
            assertInterface(definitions[1],
                name = "B",
                implements = "A",
                definitions = 2,
                attributes = 1
            ) {
                assertAttributeIdent(attributes[0], "Exposed", "Window")
                assertFunction(definitions[0], "f", "undefined", argsCount = 0)
                assertFunction(definitions[1], "g", "undefined", argsCount = 1) {
                    assertField(args[0], "x", "DOMString")
                }
            }
        }
    }

    @Test
    fun test2(){
        WebIDL.parseDefinitions("""
            [extended_attributes]
            interface identifier {
            
                [extended_attributes]
                const int constant_identifier = 42;
            
                [extended_attributes]
                attribute int identifier;
            
                [extended_attributes]
                int identifier(/* arguments... */);
            };
        """.trimIndent()).apply {
            assertEquals(definitions.size, 1)

            assertInterface(definitions[0],
                name = "identifier",
                implements = null,
                definitions = 3,
                attributes = 1
            ) {
                assertAttribute(attributes[0], "extended_attributes")

                assertField(definitions[0], "constant_identifier", "int", value = "42", attributes = 1, isConst = true) {
                    assertAttribute(attributes[0], "extended_attributes")
                }
                assertField(definitions[1], "identifier", "int", attributes = 1, isAttribute = true) {
                    assertAttribute(attributes[0], "extended_attributes")
                }
                assertFunction(definitions[2], "identifier", "int", argsCount = 0, attributes = 1) {
                    assertAttribute(attributes[0], "extended_attributes")
                }
            }
        }
    }

    @Test
    fun test3(){
        WebIDL.parseDefinitions("""
            interface SomeInterface {
                /* interface_members... */
            };
            
            partial interface SomeInterface {
                /* interface_members... */
            };
        """.trimIndent()).apply {
            assertEquals(2, definitions.size)
            assertInterface(definitions[0],
                name = "SomeInterface",
                implements = null,
                definitions = 0
            )
            assertInterface(definitions[1],
                name = "SomeInterface",
                implements = null,
                definitions = 0,
                isPartial = true
            )
        }
    }

    @Test
    fun test4(){
        WebIDL.parseDefinitions("""
            [Exposed=Window]
            interface Animal {
                attribute DOMString name;
            };
            
            [Exposed=Window]
            interface Human : Animal {
                attribute Dog? pet;
            };
            
            [Exposed=Window]
            interface Dog : Animal {
                attribute Human? owner;
            };
        """.trimIndent()).apply {
            assertEquals(3, definitions.size)

            assertInterface(definitions[0],
                name = "Animal",
                implements = null,
                definitions = 1,
                attributes = 1
            ) {
                assertAttributeIdent(attributes[0], "Exposed", "Window")
                assertField(definitions[0], "name", "DOMString", isAttribute = true)
            }
            assertInterface(definitions[1],
                name = "Human",
                implements = "Animal",
                definitions = 1,
                attributes = 1
            ) {
                assertAttributeIdent(attributes[0], "Exposed", "Window")
                assertField(definitions[0], "pet", "Dog?", isAttribute = true)
            }
            assertInterface(definitions[2],
                name = "Dog",
                implements = "Animal",
                definitions = 1,
                attributes = 1
            ) {
                assertAttributeIdent(attributes[0], "Exposed", "Window")
                assertField(definitions[0], "owner", "Human?", isAttribute = true)
            }
        }
    }

    @Test
    fun test5(){
        WebIDL.parseDefinitions("""
            [Exposed=Window]
            interface Node {
                readonly attribute DOMString nodeName;
                readonly attribute Node? parentNode;
                Node appendChild(Node newChild);
                undefined addEventListener(DOMString type, EventListener listener);
            };
            
            callback interface EventListener {
                undefined handleEvent(Event event);
            };
        """.trimIndent()).apply {
            assertEquals(2, definitions.size)

            assertInterface(definitions[0],
                name = "Node",
                implements = null,
                definitions = 4,
                attributes = 1
            ) {
                assertAttributeIdent(attributes[0], "Exposed", "Window")
                assertField(definitions[0], "nodeName", "DOMString", isAttribute = true, isReadOnly = true)
                assertField(definitions[1], "parentNode", "Node?", isAttribute = true, isReadOnly = true)
                assertFunction(definitions[2], "appendChild", "Node", argsCount = 1) {
                    assertField(args[0], "newChild", "Node")
                }
                assertFunction(definitions[3], "addEventListener", "undefined", argsCount = 2) {
                    assertField(args[0], "type", "DOMString")
                    assertField(args[1], "listener", "EventListener")
                }
            }
            assertInterface(definitions[1],
                name = "EventListener",
                implements = null,
                definitions = 1,
                isCallback = true
            ) {
                assertFunction(definitions[0], "handleEvent", "undefined", argsCount = 1) {
                    assertField(args[0], "event", "Event")
                }
            }
        }
    }
}