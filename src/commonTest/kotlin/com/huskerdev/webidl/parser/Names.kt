package com.huskerdev.webidl.parser

import com.huskerdev.webidl.WebIDL
import kotlin.test.Test
import kotlin.test.assertEquals


class Names {

    @Test
    fun test1(){
        WebIDL.parseDefinitions("""
            interface interface_identifier { /* interface_members... */ };
            partial interface interface_identifier { /* interface_members... */ };
            namespace namespace_identifier { /* namespace_members... */ };
            partial namespace namespace_identifier { /* namespace_members... */ };
            dictionary dictionary_identifier { /* dictionary_members... */ };
            partial dictionary dictionary_identifier { /* dictionary_members... */ };
            enum enumeration_identifier { "enum", "values" /* , ... */ };
            callback callback_identifier = void (/* arguments... */);
            callback interface callback_interface_identifier { /* interface_members... */ };
        """.trimIndent()).apply {
            assertEquals(definitions.size, 9)

            assertInterface(definitions[0],
                name = "interface_identifier",
                implements = null,
                definitions = 0
            )
            assertInterface(definitions[1],
                name = "interface_identifier",
                implements = null,
                definitions = 0,
                isPartial = true
            )
            assertNamespace(definitions[2],
                name = "namespace_identifier",
                definitions = 0
            )
            assertNamespace(definitions[3],
                name = "namespace_identifier",
                definitions = 0,
                isPartial = true
            )
            assertDictionary(definitions[4],
                name = "dictionary_identifier",
                implements = null,
                definitions = 0
            )
            assertDictionary(definitions[5],
                name = "dictionary_identifier",
                implements = null,
                definitions = 0,
                isPartial = true
            )
            assertEnum(definitions[6],
                name = "enumeration_identifier",
                elements = listOf("enum", "values")
            )
            assertCallbackFunction(definitions[7], "callback_identifier") {
                assertFunction(function, "", "void", argsCount = 0)
            }
            assertInterface(definitions[8],
                name = "callback_interface_identifier",
                implements = null,
                definitions = 0,
                isCallback = true
            )
        }
    }

    @Test
    fun test2(){
        WebIDL.parseDefinitions("""
            [extended_attributes]
            interface identifier {
                attribute int attribute_identifier;
            };
            
            typedef int typedef_identifier;
            
            dictionary identifier {
                int dictionary_member_identifier;
            };
        """.trimIndent()).apply {
            assertEquals(definitions.size, 3)

            assertInterface(definitions[0],
                name = "identifier",
                implements = null,
                definitions = 1,
                attributes = 1
            ) {
                assertAttribute(attributes[0], "extended_attributes")

                assertField(definitions[0], "attribute_identifier", "int", isAttribute = true)
            }
            assertTypedef(definitions[1], "int", "typedef_identifier")
            assertDictionary(definitions[2],
                name = "identifier",
                implements = null,
                definitions = 1
            ) {
                assertField(definitions[0], "dictionary_member_identifier", "int")
            }
        }
    }

    @Test
    fun test3() {
        WebIDL.parseDefinitions("""
            interface interface_identifier {
                const int constant_identifier = 42;
            };
        """.trimIndent()
        ).apply {
            assertEquals(definitions.size, 1)

            assertInterface(definitions[0],
                name = "interface_identifier",
                implements = null,
                definitions = 1
            ) {
                assertField(definitions[0], "constant_identifier", "int", isConst = true)
            }
        }
    }

    @Test
    fun test4() {
        WebIDL.parseDefinitions("""
            interface interface_identifier {
                int operation_identifier(/* arguments... */);
            };
        """.trimIndent()
        ).apply {
            assertEquals(definitions.size, 1)

            assertInterface(definitions[0],
                name = "interface_identifier",
                implements = null,
                definitions = 1
            ) {
                assertFunction(definitions[0], "operation_identifier", "int", argsCount = 0)
            }
        }
    }

    @Test
    fun test5() {
        WebIDL.parseDefinitions("""
            interface interface_identifier {
                int operation_identifier(int argument_identifier /* , ... */);
            };
        """.trimIndent()
        ).apply {
            assertEquals(definitions.size, 1)

            assertInterface(definitions[0],
                name = "interface_identifier",
                implements = null,
                definitions = 1
            ) {
                assertFunction(definitions[0], "operation_identifier", "int", argsCount = 1) {
                    assertField(args[0], "argument_identifier", "int")
                }
            }
        }
    }

    @Test
    fun test6() {
        WebIDL.parseDefinitions("""
            [Exposed=Window]
            interface B : A {
                undefined f(SequenceOfLongs x);
            };
            
            [Exposed=Window]
            interface A {
            };
            
            typedef sequence<long> SequenceOfLongs;
        """.trimIndent()
        ).apply {
            assertEquals(definitions.size, 3)

            assertInterface(definitions[0],
                name = "B",
                implements = "A",
                definitions = 1,
                attributes = 1
            ) {
                assertAttributeIdent(attributes[0], "Exposed", "Window")

                assertFunction(definitions[0], "f", "undefined", argsCount = 1) {
                    assertField(args[0], "x", "SequenceOfLongs")
                }
            }
            assertInterface(definitions[1],
                name = "A",
                implements = null,
                definitions = 0,
                attributes = 1
            ) {
                assertAttributeIdent(attributes[0], "Exposed", "Window")
            }
            assertTypedef(definitions[2], "sequence<long>", "SequenceOfLongs")
        }
    }

    @Test
    fun test7() {
        WebIDL.parseDefinitions("""
            // Typedef identifier: "number"
            typedef double number;
            
            // Interface identifier: "System"
            [Exposed=Window]
            interface System {
            
                // Operation identifier:          "createObject"
                // Operation argument identifier: "interface"
                object createObject(DOMString _interface);
            
                // Operation argument identifier: "interface"
                sequence<object> getObjects(DOMString interface);
            
                // Operation has no identifier; it declares a getter.
                getter DOMString (DOMString keyName);
            };
            
            // Interface identifier: "TextField"
            [Exposed=Window]
            interface TextField {
            
                // Attribute identifier: "const"
                attribute boolean _const;
            
                // Attribute identifier: "value"
                attribute DOMString? _value;
            };
        """.trimIndent()
        ).apply {
            assertEquals(definitions.size, 3)

            assertTypedef(definitions[0], "double", "number")

            assertInterface(definitions[1],
                name = "System",
                implements = null,
                definitions = 3,
                attributes = 1
            ) {
                assertAttributeIdent(attributes[0], "Exposed", "Window")

                assertFunction(definitions[0], "createObject", "object", argsCount = 1) {
                    assertField(args[0], "_interface", "DOMString")
                }
                assertFunction(definitions[1], "getObjects", "sequence<object>", argsCount = 1) {
                    assertField(args[0], "interface", "DOMString")
                }
                assertGetter(definitions[2]) {
                    assertFunction(function, "", "DOMString", argsCount = 1) {
                        assertField(args[0], "keyName", "DOMString")
                    }
                }
            }

            assertInterface(definitions[2],
                name = "TextField",
                implements = null,
                definitions = 2,
                attributes = 1
            ) {
                assertAttributeIdent(attributes[0], "Exposed", "Window")

                assertField(definitions[0], "_const", "boolean", isAttribute = true)
                assertField(definitions[1], "_value", "DOMString?", isAttribute = true)
            }
        }
    }
}