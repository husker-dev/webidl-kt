package com.huskerdev.webidl.resolver

import com.huskerdev.webidl.WebIDL
import com.huskerdev.webidl.WebIDLPrinter
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue


class InterfaceMembers {

    @Test
    fun constants(){
        WebIDL.resolve("""
            [Exposed=Window]
            interface Util {
                const boolean DEBUG = false;
                const octet LF = 10;
                const unsigned long BIT_MASK = 0x0000fc00;
                const double AVOGADRO = 6.022e23;
            };
        """.trimIndent()).apply {
            assertInterface(interfaces.values.toList()[0], "Util") {
                assertField(fields[0], "DEBUG", value = "false", isConst = true) {
                    assertDefaultType(type, "boolean")
                }
                assertField(fields[1], "LF", value = "10", isConst = true) {
                    assertDefaultType(type, "octet")
                }
                assertField(fields[2], "BIT_MASK", value = "0x0000fc00", isConst = true) {
                    assertDefaultType(type, "unsigned long")
                }
                assertField(fields[3], "AVOGADRO", value = "6.022e23", isConst = true) {
                    assertDefaultType(type, "double")
                }
            }
        }
    }

    @Test
    fun attributes(){
        WebIDL.resolve("""
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
            assertInterface(interfaces.values.toList()[0], "Animal") {
                assertField(fields[0], "name", isReadOnly = true, isAttribute = true) {
                    assertDefaultType(type, "DOMString")
                }
                assertField(fields[1], "age", isAttribute = true) {
                    assertDefaultType(type, "short")
                }
            }
            assertInterface(interfaces.values.toList()[1], "Person", implements = interfaces.values.toList()[0]) {
                assertField(fields[0], "name", isInherit = true, isAttribute = true) {
                    assertDefaultType(type, "DOMString")
                }
            }
        }
    }

    @Test
    fun operations(){
        WebIDL.resolve("""
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
            assertInterface(interfaces.values.toList()[0], "Dimensions") {
                assertField(fields[0], "width", isAttribute = true) {
                    assertDefaultType(type, "long")
                }
                assertField(fields[1], "height", isAttribute = true) {
                    assertDefaultType(type, "long")
                }
            }
            assertInterface(interfaces.values.toList()[1], "Button") {
                assertOperation(operations[0], "isMouseOver") {
                    assertDefaultType(type, "boolean")
                }
                assertOperation(operations[1], "setDimensions") {
                    assertDefaultType(type, "undefined")
                    assertArgument(args[0], "size") {
                        assertDefaultType(type, "Dimensions")
                    }
                }
                assertOperation(operations[2], "setDimensions") {
                    assertDefaultType(type, "undefined")
                    assertArgument(args[0], "width") {
                        assertDefaultType(type, "long")
                    }
                    assertArgument(args[1], "height") {
                        assertDefaultType(type, "long")
                    }
                }
            }
        }
    }

    @Test
    fun variadic(){
        WebIDL.resolve("""
            [Exposed=Window]
            interface IntegerSet {
                readonly attribute long cardinality;
            
                undefined union(long... ints);
                undefined intersection(long... ints);
            };
        """.trimIndent()).apply {
            assertInterface(interfaces.values.toList()[0], "IntegerSet") {
                assertField(fields[0], "cardinality", isReadOnly = true, isAttribute = true) {
                    assertDefaultType(type, "long")
                }
                assertOperation(operations[0], "union") {
                    assertDefaultType(type, "undefined")
                    assertArgument(args[0], "ints", isVariadic = true) {
                        assertDefaultType(type, "long")
                    }
                }
                assertOperation(operations[1], "intersection") {
                    assertDefaultType(type, "undefined")
                    assertArgument(args[0], "ints", isVariadic = true) {
                        assertDefaultType(type, "long")
                    }
                }
            }
        }
    }

    @Test
    fun optional(){
        WebIDL.resolve("""
            interface interface_identifier {
                void identifier1(long arg1, optional DOMString arg2);
                void identifier2(long arg1, optional DOMString arg2 = "value");
            };
        """.trimIndent()).apply {
            assertInterface(interfaces.values.toList()[0], "interface_identifier") {
                assertOperation(operations[0], "identifier1") {
                    assertDefaultType(type, "void")
                    assertArgument(args[0], "arg1") {
                        assertDefaultType(type, "long")
                    }
                    assertArgument(args[1], "arg2", isOptional = true) {
                        assertDefaultType(type, "DOMString")
                    }
                }
                assertOperation(operations[1], "identifier2") {
                    assertDefaultType(type, "void")
                    assertArgument(args[0], "arg1") {
                        assertDefaultType(type, "long")
                    }
                    assertArgument(args[1], "arg2", isOptional = true, value = "\"value\"") {
                        assertDefaultType(type, "DOMString")
                    }
                }
            }
        }
    }

    @Test
    fun optionalDictionary(){
        WebIDL.resolve("""
            dictionary LookupOptions {
                boolean caseSensitive = false;
            };
            
            [Exposed=Window]
            interface AddressBook {
                boolean hasAddressForName(USVString name, optional LookupOptions options = {});
            };
        """.trimIndent()).apply {
            assertDictionary(dictionaries.values.toList()[0], "LookupOptions") {
                assertField(fields[0], "caseSensitive", value = "false") {
                    assertDefaultType(type, "boolean")
                }
            }
            assertInterface(interfaces.values.toList()[0], "AddressBook") {
                assertOperation(operations[0], "hasAddressForName") {
                    assertDefaultType(type, "boolean")
                    assertArgument(args[0], "name") {
                        assertDefaultType(type, "USVString")
                    }
                    assertArgument(args[1], "options", value = "{}", isOptional = true) {
                        assertDefaultType(type, "LookupOptions")
                    }
                }
            }
        }
    }

    @Test
    fun constructors(){
        WebIDL.resolve("""
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
            assertInterface(interfaces.values.toList()[0], "Circle") {
                assertConstructor(constructors[0]) {
                    assertEquals(0, args.size)
                }
                assertConstructor(constructors[1]) {
                    assertArgument(args[0], "radius") {
                        assertDefaultType(type, "double")
                    }
                }
                assertField(fields[0], "r", isAttribute = true) {
                    assertDefaultType(type, "double")
                }
                assertField(fields[1], "cx", isAttribute = true) {
                    assertDefaultType(type, "double")
                }
                assertField(fields[2], "cy", isAttribute = true) {
                    assertDefaultType(type, "double")
                }
                assertField(fields[3], "circumference", isReadOnly = true, isAttribute = true) {
                    assertDefaultType(type, "double")
                }
            }
        }
    }

    @Test
    fun stringifiers(){
        WebIDL.resolve("""
            interface interface_identifier1 {
                stringifier;
            };
            interface interface_identifier2 {
                stringifier attribute DOMString identifier;
            };
        """.trimIndent()).apply {
            assertInterface(interfaces.values.toList()[0], "interface_identifier1") {
                assertNull(stringifier)
                assertTrue(stringifierEnabled)
            }
            assertInterface(interfaces.values.toList()[1], "interface_identifier2") {
                assertNotNull(stringifier)
                assertField(stringifier!!, "identifier", isAttribute = true) {
                    assertDefaultType(type, "DOMString")
                }
            }
        }
    }

    @Test
    fun specialOperators(){
        WebIDL.resolve("""
            interface Dictionary {
                getter double (DOMString propertyName);
                setter undefined (DOMString propertyName, double propertyValue);
            };
            interface Dictionary1 {
                getter double getProperty(DOMString propertyName);
                setter undefined setProperty(DOMString propertyName, double propertyValue);
            };
        """.trimIndent()).apply {
            assertInterface(interfaces.values.toList()[0], "Dictionary") {
                assertNotNull(getter)
                assertOperation(getter!!, "") {
                    assertDefaultType(type, "double")
                    assertArgument(args[0], "propertyName") {
                        assertDefaultType(type, "DOMString")
                    }
                }
                assertNotNull(setter)
                assertOperation(setter!!, "") {
                    assertDefaultType(type, "undefined")
                    assertArgument(args[0], "propertyName") {
                        assertDefaultType(type, "DOMString")
                    }
                    assertArgument(args[1], "propertyValue") {
                        assertDefaultType(type, "double")
                    }
                }
            }
            assertInterface(interfaces.values.toList()[1], "Dictionary1") {
                assertNotNull(getter)
                assertOperation(getter!!, "getProperty") {
                    assertDefaultType(type, "double")
                    assertArgument(args[0], "propertyName") {
                        assertDefaultType(type, "DOMString")
                    }
                }
                assertNotNull(setter)
                assertOperation(setter!!, "setProperty") {
                    assertDefaultType(type, "undefined")
                    assertArgument(args[0], "propertyName") {
                        assertDefaultType(type, "DOMString")
                    }
                    assertArgument(args[1], "propertyValue") {
                        assertDefaultType(type, "double")
                    }
                }
            }
        }
    }

    @Test
    fun static(){
        WebIDL.resolve("""
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
            assertInterface(interfaces.values.toList()[0], "Point")
            assertInterface(interfaces.values.toList()[1], "Circle") {
                assertField(fields[0], "cx", isAttribute = true) {
                    assertDefaultType(type, "double")
                }
                assertField(fields[1], "cy", isAttribute = true) {
                    assertDefaultType(type, "double")
                }
                assertField(fields[2], "radius", isAttribute = true) {
                    assertDefaultType(type, "double")
                }
                assertField(staticFields[0], "triangulationCount", isReadOnly = true, isAttribute = true, isStatic = true) {
                    assertDefaultType(type, "long")
                }
                assertOperation(staticOperations[0], "triangulate", isStatic = true) {
                    assertDefaultType(type, "Point")
                    assertArgument(args[0], "c1") {
                        assertDefaultType(type, "Circle")
                    }
                    assertArgument(args[1], "c2") {
                        assertDefaultType(type, "Circle")
                    }
                    assertArgument(args[2], "c3") {
                        assertDefaultType(type, "Circle")
                    }
                }
            }
        }
    }

    @Test
    fun overloading(){
        WebIDL.resolve("""
            interface B {
                undefined f(DOMString w);
                undefined f(long w, double x, Node y, Node z);
            };
            
            // for resolver
            interface Node {};
        """.trimIndent()).apply {
            assertInterface(interfaces.values.toList()[0], "B") {
                assertOperation(operations[0], "f") {
                    assertDefaultType(type, "undefined")
                    assertArgument(args[0], "w") {
                        assertDefaultType(type, "DOMString")
                    }
                }
                assertOperation(operations[1], "f") {
                    assertDefaultType(type, "undefined")
                    assertArgument(args[0], "w") {
                        assertDefaultType(type, "long")
                    }
                    assertArgument(args[1], "x") {
                        assertDefaultType(type, "double")
                    }
                    assertArgument(args[2], "y") {
                        assertDefaultType(type, "Node")
                    }
                    assertArgument(args[3], "z") {
                        assertDefaultType(type, "Node")
                    }
                }
            }
        }
    }

    @Test
    fun iterable(){
        WebIDL.resolve("""
            interface interface_identifier1 {
                iterable<long>;
            };
            interface interface_identifier2 {
                iterable<DOMString, long>;
            };
        """.trimIndent()).apply {
            assertInterface(interfaces.values.toList()[0], "interface_identifier1") {
                assertTrue(isIterable)
                assertNotNull(iterableType)
                assertEquals("long", WebIDLPrinter.printResolvedType(iterableType!!.first))
            }
            assertInterface(interfaces.values.toList()[1], "interface_identifier2") {
                assertTrue(isIterable)
                assertNotNull(iterableType)
                assertNotNull(iterableType!!.second)
                assertEquals("DOMString", WebIDLPrinter.printResolvedType(iterableType!!.first))
                assertEquals("long", WebIDLPrinter.printResolvedType(iterableType!!.second!!))
            }
        }
    }

    @Test
    fun iterableAsync(){
        WebIDL.resolve("""
            interface interface_identifier1 {
                async_iterable<long>;
            };
            interface interface_identifier2 {
                async_iterable<DOMString, long>;
            };
        """.trimIndent()).apply {
            assertInterface(interfaces.values.toList()[0], "interface_identifier1") {
                assertTrue(isAsyncIterable)
                assertNotNull(asyncIterableType)
                assertEquals("long", WebIDLPrinter.printResolvedType(asyncIterableType!!.first))
            }
            assertInterface(interfaces.values.toList()[1], "interface_identifier2") {
                assertTrue(isAsyncIterable)
                assertNotNull(asyncIterableType)
                assertNotNull(asyncIterableType!!.second)
                assertEquals("DOMString", WebIDLPrinter.printResolvedType(asyncIterableType!!.first))
                assertEquals("long", WebIDLPrinter.printResolvedType(asyncIterableType!!.second!!))
            }
        }
    }

    @Test
    fun maplike(){
        WebIDL.resolve("""
            interface interface_identifier1 {
                readonly maplike<DOMString, long>;
            };
            interface interface_identifier2 {
                maplike<DOMString, long>;
            };
        """.trimIndent()).apply {
            assertInterface(interfaces.values.toList()[0], "interface_identifier1") {
                assertTrue(isMap)
                assertTrue(isReadOnlyMap)
                assertNotNull(mapType)
                assertEquals("DOMString", WebIDLPrinter.printResolvedType(mapType!!.first))
                assertEquals("long", WebIDLPrinter.printResolvedType(mapType!!.second))
            }
            assertInterface(interfaces.values.toList()[1], "interface_identifier2") {
                assertTrue(isMap)
                assertFalse(isReadOnlyMap)
                assertNotNull(mapType)
                assertEquals("DOMString", WebIDLPrinter.printResolvedType(mapType!!.first))
                assertEquals("long", WebIDLPrinter.printResolvedType(mapType!!.second))
            }
        }
    }

    @Test
    fun setlike(){
        WebIDL.resolve("""
            interface interface_identifier1 {
                readonly setlike<long>;
            };
            interface interface_identifier2 {
                setlike<long>;
            };
        """.trimIndent()).apply {
            assertInterface(interfaces.values.toList()[0], "interface_identifier1") {
                assertTrue(isSet)
                assertTrue(isReadOnlySet)
                assertNotNull(setType)
                assertEquals("long", WebIDLPrinter.printResolvedType(setType!!))
            }
            assertInterface(interfaces.values.toList()[1], "interface_identifier2") {
                assertTrue(isSet)
                assertFalse(isReadOnlySet)
                assertNotNull(setType)
                assertEquals("long", WebIDLPrinter.printResolvedType(setType!!))
            }
        }
    }
}