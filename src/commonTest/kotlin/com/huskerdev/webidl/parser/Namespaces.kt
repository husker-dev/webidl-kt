package com.huskerdev.webidl.parser

import com.huskerdev.webidl.WebIDL
import kotlin.test.Test
import kotlin.test.assertEquals


class Namespaces {

    @Test
    fun test1(){
        WebIDL.parseDefinitions("""
            namespace SomeNamespace {
                /* namespace_members... */
            };
            
            partial namespace SomeNamespace {
                /* namespace_members... */
            };
        """.trimIndent()).apply {
            assertEquals(definitions.size, 2)

            assertNamespace(definitions[0],
                name = "SomeNamespace",
                definitions = 0
            )
            assertNamespace(definitions[1],
                name = "SomeNamespace",
                definitions = 0,
                isPartial = true
            )
        }
    }

    @Test
    fun test2(){
        WebIDL.parseDefinitions("""
            interface Vector {
            };
            
            namespace VectorUtils {
                readonly attribute Vector unit;
                double dotProduct(Vector x, Vector y);
                Vector crossProduct(Vector x, Vector y);
            };
        """.trimIndent()).apply {
            assertEquals(definitions.size, 2)

            assertInterface(definitions[0],
                name = "Vector",
                implements = null,
                definitions = 0
            )
            assertNamespace(definitions[1],
                name = "VectorUtils",
                definitions = 3
            ) {
                assertField(definitions[0], "unit", "Vector", isAttribute = true, isReadOnly = true)
                assertFunction(definitions[1], "dotProduct", "double", argsCount = 2) {
                    assertField(args[0], "x", "Vector")
                    assertField(args[1], "y", "Vector")
                }
                assertFunction(definitions[2], "crossProduct", "Vector", argsCount = 2) {
                    assertField(args[0], "x", "Vector")
                    assertField(args[1], "y", "Vector")
                }
            }
        }
    }

}