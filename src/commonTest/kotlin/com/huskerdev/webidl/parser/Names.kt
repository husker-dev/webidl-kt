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
        """.trimIndent()).first.apply {
            assertEquals(9, this.definitions.size)

            assertInterface(
                this.definitions[0],
                name = "interface_identifier",
                implements = null,
                definitions = 0
            )
            assertInterface(
                this.definitions[1],
                name = "interface_identifier",
                implements = null,
                definitions = 0,
                isPartial = true
            )
            assertNamespace(
                this.definitions[2],
                name = "namespace_identifier",
                definitions = 0
            )
            assertNamespace(
                this.definitions[3],
                name = "namespace_identifier",
                definitions = 0,
                isPartial = true
            )
            assertDictionary(
                this.definitions[4],
                name = "dictionary_identifier",
                implements = null,
                definitions = 0
            )
            assertDictionary(
                this.definitions[5],
                name = "dictionary_identifier",
                implements = null,
                definitions = 0,
                isPartial = true
            )
            assertEnum(
                this.definitions[6],
                name = "enumeration_identifier",
                elements = listOf("enum", "values")
            )
            assertCallbackFunction(this.definitions[7], "callback_identifier") {
                assertOperation(operation, "", "void", argsCount = 0)
            }
            assertInterface(
                this.definitions[8],
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
            interface interface_identifier {
                attribute long attribute_identifier;
            };
            
            typedef long typedef_identifier;
            
            dictionary dictionary_identifier {
                long dictionary_member_identifier;
            };
        """.trimIndent()).first.apply {
            assertEquals(3, this.definitions.size)

            assertInterface(
                this.definitions[0],
                name = "interface_identifier",
                implements = null,
                definitions = 1,
                attributes = 1
            ) {
                assertAttribute(attributes!![0], "extended_attributes")

                assertField(this.definitions[0], "attribute_identifier", "long", isAttribute = true)
            }
            assertTypedef(this.definitions[1], "long", "typedef_identifier")
            assertDictionary(
                this.definitions[2],
                name = "dictionary_identifier",
                implements = null,
                definitions = 1
            ) {
                assertField(definitions[0], "dictionary_member_identifier", "long")
            }
        }
    }

    @Test
    fun test3() {
        WebIDL.parseDefinitions("""
            interface interface_identifier {
                const long constant_identifier = 42;
            };
        """.trimIndent()
        ).first.apply {
            assertEquals(1, this.definitions.size)

            assertInterface(
                this.definitions[0],
                name = "interface_identifier",
                implements = null,
                definitions = 1
            ) {
                assertField(this.definitions[0], "constant_identifier", "long", isConst = true)
            }
        }
    }

    @Test
    fun test4() {
        WebIDL.parseDefinitions("""
            interface interface_identifier {
                long operation_identifier(/* arguments... */);
            };
        """.trimIndent()
        ).first.apply {
            assertEquals(1, this.definitions.size)

            assertInterface(
                this.definitions[0],
                name = "interface_identifier",
                implements = null,
                definitions = 1
            ) {
                assertOperation(this.definitions[0], "operation_identifier", "long", argsCount = 0)
            }
        }
    }

    @Test
    fun test5() {
        WebIDL.parseDefinitions("""
            interface interface_identifier {
                long operation_identifier(long argument_identifier /* , ... */);
            };
        """.trimIndent()
        ).first.apply {
            assertEquals(1, this.definitions.size)

            assertInterface(
                this.definitions[0],
                name = "interface_identifier",
                implements = null,
                definitions = 1
            ) {
                assertOperation(this.definitions[0], "operation_identifier", "long", argsCount = 1) {
                    assertField(args[0], "argument_identifier", "long")
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
        ).first.apply {
            assertEquals(3, this.definitions.size)

            assertInterface(
                this.definitions[0],
                name = "B",
                implements = "A",
                definitions = 1,
                attributes = 1
            ) {
                assertAttributeIdent(attributes!![0], "Exposed", "Window")

                assertOperation(this.definitions[0], "f", "undefined", argsCount = 1) {
                    assertField(args[0], "x", "SequenceOfLongs")
                }
            }
            assertInterface(
                this.definitions[1],
                name = "A",
                implements = null,
                definitions = 0,
                attributes = 1
            ) {
                assertAttributeIdent(attributes!![0], "Exposed", "Window")
            }
            assertTypedef(this.definitions[2], "sequence<long>", "SequenceOfLongs")
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
        ).first.apply {
            assertEquals(3, this.definitions.size)

            assertTypedef(this.definitions[0], "double", "number")

            assertInterface(
                this.definitions[1],
                name = "System",
                implements = null,
                definitions = 3,
                attributes = 1
            ) {
                assertAttributeIdent(attributes!![0], "Exposed", "Window")

                assertOperation(this.definitions[0], "createObject", "object", argsCount = 1) {
                    assertField(args[0], "_interface", "DOMString")
                }
                assertOperation(this.definitions[1], "getObjects", "sequence<object>", argsCount = 1) {
                    assertField(args[0], "interface", "DOMString")
                }
                assertGetter(this.definitions[2]) {
                    assertOperation(operation, "", "DOMString", argsCount = 1) {
                        assertField(args[0], "keyName", "DOMString")
                    }
                }
            }

            assertInterface(
                this.definitions[2],
                name = "TextField",
                implements = null,
                definitions = 2,
                attributes = 1
            ) {
                assertAttributeIdent(attributes!![0], "Exposed", "Window")

                assertField(this.definitions[0], "_const", "boolean", isAttribute = true)
                assertField(this.definitions[1], "_value", "DOMString?", isAttribute = true)
            }
        }
    }
}