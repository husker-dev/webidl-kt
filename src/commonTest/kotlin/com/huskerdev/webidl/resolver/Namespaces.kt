package com.huskerdev.webidl.resolver

import com.huskerdev.webidl.WebIDL
import kotlin.test.Test
import kotlin.test.assertEquals


class Namespaces {

    @Test
    fun test1(){
        WebIDL.resolve("""
            namespace SomeNamespace {
                /* namespace_members... */
            };
            
            partial namespace SomeNamespace {
                /* namespace_members... */
            };
        """.trimIndent()).apply {
            assertEquals(1, namespaces.size)
            assertNamespace(namespaces.values.toList()[0], "SomeNamespace")
        }
    }

    @Test
    fun test2(){
        WebIDL.resolve("""
            interface Vector {
            };
            
            namespace VectorUtils {
                readonly attribute Vector unit;
                double dotProduct(Vector x, Vector y);
                Vector crossProduct(Vector x, Vector y);
            };
        """.trimIndent()).apply {
            assertInterface(interfaces.values.toList()[0], "Vector")
            assertNamespace(namespaces.values.toList()[0], "VectorUtils") {
                assertField(fields[0], "unit", isReadOnly = true, isAttribute = true) {
                    assertDefaultType(type, "Vector")
                }
                assertOperation(operations[0], "dotProduct") {
                    assertDefaultType(type, "double")
                    assertArgument(args[0], "x") {
                        assertDefaultType(type, "Vector")
                    }
                    assertArgument(args[1], "y") {
                        assertDefaultType(type, "Vector")
                    }
                }
                assertOperation(operations[1], "crossProduct") {
                    assertDefaultType(type, "Vector")
                    assertArgument(args[0], "x") {
                        assertDefaultType(type, "Vector")
                    }
                    assertArgument(args[1], "y") {
                        assertDefaultType(type, "Vector")
                    }
                }
            }
        }
    }
}