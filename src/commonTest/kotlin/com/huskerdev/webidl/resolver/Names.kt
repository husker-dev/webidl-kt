package com.huskerdev.webidl.resolver

import com.huskerdev.webidl.WebIDL
import kotlin.test.Test
import kotlin.test.assertNotNull


class Names {

    @Test
    fun test1(){
        WebIDL.resolve("""
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
            assertInterface(interfaces.values.toList()[0], "interface_identifier")
            assertNamespace(namespaces.values.toList()[0], "namespace_identifier")
            assertDictionary(dictionaries.values.toList()[0], "dictionary_identifier")
            assertEnum(enums.values.toList()[0], "enumeration_identifier", listOf("enum", "values"))
            assertCallbackFunction(callbacks.values.toList()[0], "callback_identifier")
            assertInterface(interfaces.values.toList()[1], "callback_interface_identifier", isCallback = true)
        }
    }

    @Test
    fun test2(){
        WebIDL.resolve("""
            [extended_attributes]
            interface interface_identifier {
                attribute long attribute_identifier;
            };
            
            typedef long typedef_identifier;
            
            dictionary dictionary_identifier {
                long dictionary_member_identifier;
            };
        """.trimIndent()).apply {
            assertInterface(interfaces.values.toList()[0], "interface_identifier") {
                assertField(fields[0], "attribute_identifier", isAttribute = true) {
                    assertDefaultType(type, "long")
                }
            }
            assertTypedef(typeDefs.values.toList()[0], "typedef_identifier") {
                assertDefaultType(type, "long")
            }
            assertDictionary(dictionaries.values.toList()[0], "dictionary_identifier") {
                assertField(fields[0], "dictionary_member_identifier") {
                    assertDefaultType(type, "long")
                }
            }
        }
    }

    @Test
    fun test3() {
        WebIDL.resolve("""
            interface interface_identifier {
                const long constant_identifier = 42;
            };
        """.trimIndent()
        ).apply {
            assertInterface(interfaces.values.toList()[0], "interface_identifier") {
                assertField(fields[0], "constant_identifier", value = "42", isConst = true) {
                    assertDefaultType(type, "long")
                }
            }
        }
    }

    @Test
    fun test4() {
        WebIDL.resolve("""
            interface interface_identifier {
                long operation_identifier(/* arguments... */);
            };
        """.trimIndent()
        ).apply {
            assertInterface(interfaces.values.toList()[0], "interface_identifier") {
                assertOperation(operations[0], "operation_identifier") {
                    assertDefaultType(type, "long")
                }
            }
        }
    }

    @Test
    fun test5() {
        WebIDL.resolve("""
            interface interface_identifier {
                long operation_identifier(long argument_identifier /* , ... */);
            };
        """.trimIndent()
        ).apply {
            assertInterface(interfaces.values.toList()[0], "interface_identifier") {
                assertOperation(operations[0], "operation_identifier") {
                    assertDefaultType(type, "long")
                    assertArgument(args[0], "argument_identifier") {
                        assertDefaultType(type, "long")
                    }
                }
            }
        }
    }

    @Test
    fun test6() {
        WebIDL.resolve("""
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
            assertInterface(interfaces.values.toList()[0], "B", implements = interfaces.values.toList()[1]) {
                assertOperation(operations[0], "f") {
                    assertDefaultType(type, "undefined")
                    assertArgument(args[0], "x") {
                        assertDefaultType(type, "SequenceOfLongs")
                    }
                }
            }
            assertInterface(interfaces.values.toList()[1], "A")

            assertTypedef(typeDefs.values.toList()[0], "SequenceOfLongs") {
                assertDefaultType(type, "sequence<long>", parameters = 1)
            }
        }
    }

    @Test
    fun test7() {
        WebIDL.resolve("""
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
            assertTypedef(typeDefs.values.toList()[0], "number") {
                assertDefaultType(type, "double")
            }

            assertInterface(interfaces.values.toList()[0], "System") {
                assertOperation(operations[0], "createObject") {
                    assertDefaultType(type, "object")
                    assertArgument(args[0], "_interface") {
                        assertDefaultType(type, "DOMString")
                    }
                }
                assertOperation(operations[1], "getObjects") {
                    assertDefaultType(type, "sequence<object>", parameters = 1)
                    assertArgument(args[0], "interface") {
                        assertDefaultType(type, "DOMString")
                    }
                }
                assertNotNull(getter)
                assertOperation(getter!!, "") {
                    assertDefaultType(type, "DOMString")
                    assertArgument(args[0], "keyName") {
                        assertDefaultType(type, "DOMString")
                    }
                }
            }
            assertInterface(interfaces.values.toList()[1], "TextField") {
                assertField(fields[0], "_const", isAttribute = true) {
                    assertDefaultType(type, "boolean")
                }
                assertField(fields[1], "_value", isAttribute = true) {
                    assertDefaultType(type, "DOMString?", isNullable = true)
                }
            }
        }
    }
}