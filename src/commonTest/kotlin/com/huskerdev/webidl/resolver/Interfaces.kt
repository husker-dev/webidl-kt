package com.huskerdev.webidl.resolver

import com.huskerdev.webidl.WebIDL
import kotlin.test.Test
import kotlin.test.assertEquals


class Interfaces {

    @Test
    fun test1(){
        WebIDL.resolve("""
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
            assertInterface(interfaces.values.toList()[0], "A") {
                assertOperation(operations[0], "f") {
                    assertDefaultType(type, "undefined")
                }
                assertOperation(operations[1], "g") {
                    assertDefaultType(type, "undefined")
                }
            }
            assertInterface(interfaces.values.toList()[1], "B", implements = interfaces.values.toList()[0]) {
                assertOperation(operations[0], "f") {
                    assertDefaultType(type, "undefined")
                }
                assertOperation(operations[1], "g") {
                    assertDefaultType(type, "undefined")
                    assertArgument(args[0], "x") {
                        assertDefaultType(type, "DOMString")
                    }
                }
            }
        }
    }

    @Test
    fun test2(){
        WebIDL.resolve("""
            [extended_attributes]
            interface identifier {
            
                [extended_attributes]
                const long constant_identifier = 42;
            
                [extended_attributes]
                attribute long identifier;
            
                [extended_attributes]
                long identifier(/* arguments... */);
            };
        """.trimIndent()).apply {
            assertInterface(interfaces.values.toList()[0], "identifier") {
                assertField(fields[0], "constant_identifier", value = "42", isConst = true) {
                    assertDefaultType(type, "long")
                }
                assertField(fields[1], "identifier", isAttribute = true) {
                    assertDefaultType(type, "long")
                }
                assertOperation(operations[0], "identifier") {
                    assertDefaultType(type, "long")
                }
            }
        }
    }

    @Test
    fun test3(){
        WebIDL.resolve("""
            interface SomeInterface {
                /* interface_members... */
            };
            
            partial interface SomeInterface {
                /* interface_members... */
            };
        """.trimIndent()).apply {
            assertEquals(1, interfaces.size)
            assertInterface(interfaces.values.toList()[0], "SomeInterface")
        }
    }

    @Test
    fun test4(){
        WebIDL.resolve("""
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
            assertInterface(interfaces.values.toList()[0], "Animal") {
                assertField(fields[0], "name", isAttribute = true) {
                    assertDefaultType(type, "DOMString")
                }
            }
            assertInterface(interfaces.values.toList()[1], "Human", implements = interfaces.values.toList()[0]) {
                assertField(fields[0], "pet", isAttribute = true) {
                    assertDefaultType(type, "Dog?", isNullable = true)
                }
            }
            assertInterface(interfaces.values.toList()[2], "Dog", implements = interfaces.values.toList()[0]) {
                assertField(fields[0], "owner", isAttribute = true) {
                    assertDefaultType(type, "Human?", isNullable = true)
                }
            }
        }
    }

    @Test
    fun test5(){
        WebIDL.resolve("""
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
            
            // for resolver
            interface Event {};
        """.trimIndent()).apply {
            assertInterface(interfaces.values.toList()[0], "Node") {
                assertField(fields[0], "nodeName", isAttribute = true, isReadOnly = true) {
                    assertDefaultType(type, "DOMString")
                }
                assertField(fields[1], "parentNode", isAttribute = true, isReadOnly = true) {
                    assertDefaultType(type, "Node?", isNullable = true)
                }
                assertOperation(operations[0], "appendChild") {
                    assertDefaultType(type, "Node")
                    assertArgument(args[0], "newChild") {
                        assertDefaultType(type, "Node")
                    }
                }
                assertOperation(operations[1], "addEventListener") {
                    assertDefaultType(type, "undefined")
                    assertArgument(args[0], "type") {
                        assertDefaultType(type, "DOMString")
                    }
                    assertArgument(args[1], "listener") {
                        assertDefaultType(type, "EventListener")
                    }
                }
            }
            assertInterface(interfaces.values.toList()[1], "EventListener", isCallback = true) {
                assertOperation(operations[0], "handleEvent") {
                    assertDefaultType(type, "undefined")
                    assertArgument(args[0], "event") {
                        assertDefaultType(type, "Event")
                    }
                }
            }
        }
    }
}