package com.huskerdev.webidl.parser

import com.huskerdev.webidl.WebIDL
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull


class InterfaceMembers {

    @Test
    fun constants(){
        WebIDL.parseDefinitions("""
            [Exposed=Window]
            interface Util {
                const boolean DEBUG = false;
                const octet LF = 10;
                const unsigned long BIT_MASK = 0x0000fc00;
                const double AVOGADRO = 6.022e23;
            };
        """.trimIndent()).apply {
            assertEquals(definitions.size, 1)

            assertInterface(definitions[0],
                name = "Util",
                implements = null,
                definitions = 4,
                attributes = 1
            ) {
                assertAttributeIdent(attributes[0], "Exposed", "Window")

                assertField(definitions[0], "DEBUG", "boolean", value = "false", isConst = true)
                assertField(definitions[1], "LF", "octet", value = "10", isConst = true)
                assertField(definitions[2], "BIT_MASK", "unsigned long", value = "0x0000fc00", isConst = true)
                assertField(definitions[3], "AVOGADRO", "double", value = "6.022e23", isConst = true)
            }
        }
    }

    @Test
    fun attributes(){
        WebIDL.parseDefinitions("""
            [Exposed=Window]
            interface Animal {
            
                // A simple attribute that can be set to any string value.
                readonly attribute DOMString name;
            
                // An attribute whose value can be assigned to.
                attribute short age;
            };
            
            [Exposed=Window]
            interface Person : Animal {
            
                // An attribute whose getter behavior is inherited from Animal, and need not be
                // specified in the description of Person.
                inherit attribute DOMString name;
            };
        """.trimIndent()).apply {
            assertEquals(definitions.size, 2)

            assertInterface(definitions[0],
                name = "Animal",
                implements = null,
                definitions = 2,
                attributes = 1
            ) {
                assertAttributeIdent(attributes[0], "Exposed", "Window")

                assertField(definitions[0], "name", "DOMString", isAttribute = true, isReadOnly = true)
                assertField(definitions[1], "age", "short", isAttribute = true)
            }

            assertInterface(definitions[1],
                name = "Person",
                implements = "Animal",
                definitions = 1,
                attributes = 1
            ) {
                assertAttributeIdent(attributes[0], "Exposed", "Window")

                assertField(definitions[0], "name", "DOMString", isAttribute = true, isInherit = true)
            }
        }
    }

    @Test
    fun operations(){
        WebIDL.parseDefinitions("""
            [Exposed=Window]
            interface Dimensions {
                attribute long width;
                attribute long height;
            };
            
            [Exposed=Window]
            interface Button {
            
                // An operation that takes no arguments and returns a boolean.
                boolean isMouseOver();
            
                // Overloaded operations.
                undefined setDimensions(Dimensions size);
                undefined setDimensions(long width, long height);
            };
        """.trimIndent()).apply {
            assertEquals(definitions.size, 2)

            assertInterface(definitions[0],
                name = "Dimensions",
                implements = null,
                definitions = 2,
                attributes = 1
            ) {
                assertAttributeIdent(attributes[0], "Exposed", "Window")

                assertField(definitions[0], "width", "long", isAttribute = true)
                assertField(definitions[1], "height", "long", isAttribute = true)
            }

            assertInterface(definitions[1],
                name = "Button",
                implements = null,
                definitions = 3,
                attributes = 1
            ) {
                assertAttributeIdent(attributes[0], "Exposed", "Window")

                assertFunction(definitions[0], "isMouseOver", "boolean", argsCount = 0)
                assertFunction(definitions[1], "setDimensions", "undefined", argsCount = 1) {
                    assertField(args[0], "size", "Dimensions")
                }
                assertFunction(definitions[2], "setDimensions", "undefined", argsCount = 2) {
                    assertField(args[0], "width", "long")
                    assertField(args[1], "height", "long")
                }
            }
        }
    }

    @Test
    fun variadic(){
        WebIDL.parseDefinitions("""
            [Exposed=Window]
            interface IntegerSet {
                readonly attribute long cardinality;
            
                undefined union(long... ints);
                undefined intersection(long... ints);
            };
        """.trimIndent()).apply {
            assertEquals(definitions.size, 1)

            assertInterface(definitions[0],
                name = "IntegerSet",
                implements = null,
                definitions = 3,
                attributes = 1
            ) {
                assertAttributeIdent(attributes[0], "Exposed", "Window")

                assertField(definitions[0], "cardinality", "long", isAttribute = true, isReadOnly = true)
                assertFunction(definitions[1], "union", "undefined", argsCount = 1) {
                    assertField(args[0], "ints", "long", isVariadic = true)
                }
                assertFunction(definitions[2], "intersection", "undefined", argsCount = 1) {
                    assertField(args[0], "ints", "long", isVariadic = true)
                }
            }
        }
    }

    @Test
    fun optional(){
        WebIDL.parseDefinitions("""
            interface interface_identifier {
                void identifier1(int arg1, optional DOMString arg2);
                void identifier2(int arg1, optional DOMString arg2 = "value");
            };
        """.trimIndent()).apply {
            assertEquals(definitions.size, 1)

            assertInterface(definitions[0],
                name = "interface_identifier",
                implements = null,
                definitions = 2
            ) {
                assertFunction(definitions[0], "identifier1", "void", argsCount = 2) {
                    assertField(args[0], "arg1", "int")
                    assertField(args[1], "arg2", "DOMString", isOptional = true)
                }
                assertFunction(definitions[1], "identifier2", "void", argsCount = 2) {
                    assertField(args[0], "arg1", "int")
                    assertField(args[1], "arg2", "DOMString", isOptional = true, value = "\"value\"")
                }
            }
        }
    }

    @Test
    fun optionalDictionary(){
        WebIDL.parseDefinitions("""
            dictionary LookupOptions {
                boolean caseSensitive = false;
            };
            
            [Exposed=Window]
            interface AddressBook {
                boolean hasAddressForName(USVString name, optional LookupOptions options = {});
            };
        """.trimIndent()).apply {
            assertEquals(definitions.size, 2)

            assertDictionary(definitions[0],
                name = "LookupOptions",
                implements = null,
                definitions = 1
            ) {
                assertField(definitions[0], "caseSensitive", "boolean", value = "false")
            }
            assertInterface(definitions[1],
                name = "AddressBook",
                implements = null,
                definitions = 1,
                attributes = 1
            ) {
                assertAttributeIdent(attributes[0], "Exposed", "Window")

                assertFunction(definitions[0], "hasAddressForName", "boolean", argsCount = 2) {
                    assertField(args[0], "name", "USVString")
                    assertField(args[1], "options", "LookupOptions", isOptional = true, value = "{}")
                }
            }
        }
    }

    @Test
    fun constructors(){
        WebIDL.parseDefinitions("""
            [Exposed=Window]
            interface Circle {
                constructor();
                constructor(double radius);
                attribute double r;
                attribute double cx;
                attribute double cy;
                readonly attribute double circumference;
            };
        """.trimIndent()).apply {
            assertEquals(definitions.size, 1)

            assertInterface(definitions[0],
                name = "Circle",
                implements = null,
                definitions = 6,
                attributes = 1
            ) {
                assertAttributeIdent(attributes[0], "Exposed", "Window")

                assertConstructor(definitions[0], 0)
                assertConstructor(definitions[1], 1) {
                    assertField(args[0], "radius", "double")
                }
                assertField(definitions[2], "r", "double", isAttribute = true)
                assertField(definitions[3], "cx", "double", isAttribute = true)
                assertField(definitions[4], "cy", "double", isAttribute = true)
                assertField(definitions[5], "circumference", "double", isAttribute = true, isReadOnly = true)
            }
        }
    }

    @Test
    fun stringifiers(){
        WebIDL.parseDefinitions("""
            interface interface_identifier1 {
                stringifier;
            };
            interface interface_identifier2 {
                stringifier attribute DOMString identifier;
            };
        """.trimIndent()).apply {
            assertEquals(definitions.size, 2)

            assertInterface(definitions[0],
                name = "interface_identifier1",
                implements = null,
                definitions = 1
            ) {
                assertStringifier(definitions[0])
            }
            assertInterface(definitions[1],
                name = "interface_identifier2",
                implements = null,
                definitions = 1
            ) {
                assertStringifier(definitions[0]) {
                    assertNotNull(field)
                    assertField(field, "identifier", "DOMString", isAttribute = true)
                }
            }
        }
    }

    @Test
    fun specialOperators(){
        WebIDL.parseDefinitions("""
            interface Dictionary {
                getter double (DOMString propertyName);
                setter undefined (DOMString propertyName, double propertyValue);
            };
            interface Dictionary1 {
                getter double getProperty(DOMString propertyName);
                setter undefined setProperty(DOMString propertyName, double propertyValue);
            };
        """.trimIndent()).apply {
            assertEquals(definitions.size, 2)

            assertInterface(definitions[0],
                name = "Dictionary",
                implements = null,
                definitions = 2
            ) {
                assertGetter(definitions[0]) {
                    assertFunction(function, "", "double", argsCount = 1) {
                        assertField(args[0], "propertyName", "DOMString")
                    }
                }
                assertSetter(definitions[1]) {
                    assertFunction(function, "", "undefined", argsCount = 2) {
                        assertField(args[0], "propertyName", "DOMString")
                        assertField(args[1], "propertyValue", "double")
                    }
                }
            }
            assertInterface(definitions[1],
                name = "Dictionary1",
                implements = null,
                definitions = 2
            ) {
                assertGetter(definitions[0]) {
                    assertFunction(function, "getProperty", "double", argsCount = 1) {
                        assertField(args[0], "propertyName", "DOMString")
                    }
                }
                assertSetter(definitions[1]) {
                    assertFunction(function, "setProperty", "undefined", argsCount = 2) {
                        assertField(args[0], "propertyName", "DOMString")
                        assertField(args[1], "propertyValue", "double")
                    }
                }
            }
        }
    }

    @Test
    fun static(){
        WebIDL.parseDefinitions("""
            [Exposed=Window]
            interface Point { /* ... */ };
            
            [Exposed=Window]
            interface Circle {
                attribute double cx;
                attribute double cy;
                attribute double radius;
            
                static readonly attribute long triangulationCount;
                static Point triangulate(Circle c1, Circle c2, Circle c3);
            };
        """.trimIndent()).apply {
            assertEquals(definitions.size, 2)

            assertInterface(definitions[0],
                name = "Point",
                implements = null,
                definitions = 0,
                attributes = 1
            ) {
                assertAttributeIdent(attributes[0], "Exposed", "Window")
            }
            assertInterface(definitions[1],
                name = "Circle",
                implements = null,
                definitions = 5,
                attributes = 1
            ) {
                assertAttributeIdent(attributes[0], "Exposed", "Window")

                assertField(definitions[0], "cx", "double", isAttribute = true)
                assertField(definitions[1], "cy", "double", isAttribute = true)
                assertField(definitions[2], "radius", "double", isAttribute = true)
                assertField(definitions[3], "triangulationCount", "long",
                    isAttribute = true, isReadOnly = true, isStatic = true)
                assertFunction(definitions[4], "triangulate", "Point", argsCount = 3, isStatic = true) {
                    assertField(args[0], "c1", "Circle")
                    assertField(args[1], "c2", "Circle")
                    assertField(args[2], "c3", "Circle")
                }
            }
        }
    }

    @Test
    fun overloading(){
        WebIDL.parseDefinitions("""
            interface B {
                undefined f(DOMString w);
                undefined f(long w, double x, Node y, Node z);
            };
        """.trimIndent()).apply {
            assertEquals(definitions.size, 1)

            assertInterface(definitions[0],
                name = "B",
                implements = null,
                definitions = 2
            ) {
                assertFunction(definitions[0], "f", "undefined", argsCount = 1) {
                    assertField(args[0], "w", "DOMString")
                }
                assertFunction(definitions[1], "f", "undefined", argsCount = 4) {
                    assertField(args[0], "w", "long")
                    assertField(args[1], "x", "double")
                    assertField(args[2], "y", "Node")
                    assertField(args[3], "z", "Node")
                }
            }
        }
    }

    @Test
    fun iterable(){
        WebIDL.parseDefinitions("""
            interface interface_identifier {
                iterable<int>;
                iterable<DOMString, int>;
            };
        """.trimIndent()).apply {
            assertEquals(definitions.size, 1)

            assertInterface(definitions[0],
                name = "interface_identifier",
                implements = null,
                definitions = 2
            ) {
                assertIterable(definitions[0], "int")
                assertIterable(definitions[1], "DOMString", "int")
            }
        }
    }

    @Test
    fun iterableAsync(){
        WebIDL.parseDefinitions("""
            interface interface_identifier {
                async_iterable<int>;
                async_iterable<DOMString, int>;
            };
        """.trimIndent()).apply {
            assertEquals(definitions.size, 1)

            assertInterface(definitions[0],
                name = "interface_identifier",
                implements = null,
                definitions = 2
            ) {
                assertAsyncIterable(definitions[0], "int")
                assertAsyncIterable(definitions[1], "DOMString", "int")
            }
        }
    }

    @Test
    fun maplike(){
        WebIDL.parseDefinitions("""
            interface interface_identifier {
                readonly maplike<DOMString, int>;
                maplike<DOMString, int>;
            };
        """.trimIndent()).apply {
            assertEquals(definitions.size, 1)

            assertInterface(definitions[0],
                name = "interface_identifier",
                implements = null,
                definitions = 2
            ) {
                assertMapLike(definitions[0], "DOMString", "int", true)
                assertMapLike(definitions[1], "DOMString", "int", false)
            }
        }
    }

    @Test
    fun setlike(){
        WebIDL.parseDefinitions("""
            interface interface_identifier {
                readonly setlike<int>;
                setlike<int>;
            };
        """.trimIndent()).apply {
            assertEquals(definitions.size, 1)

            assertInterface(definitions[0],
                name = "interface_identifier",
                implements = null,
                definitions = 2
            ) {
                assertSetLike(definitions[0], "int", true)
                assertSetLike(definitions[1], "int", false)
            }
        }
    }
}