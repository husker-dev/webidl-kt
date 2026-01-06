package com.huskerdev.webidl.parser

import com.huskerdev.webidl.WebIDL
import kotlin.test.Test
import kotlin.test.assertEquals


class Enum {

    @Test
    fun test1(){
        WebIDL.parseDefinitions("""
            enum identifier { "enum", "values" /* , ... */ };
        """.trimIndent()).apply {
            assertEquals(definitions.size, 1)

            assertEnum(definitions[0],
                name = "identifier",
                elements = listOf("enum", "values")
            )
        }
    }

    @Test
    fun test2(){
        WebIDL.parseDefinitions("""
            enum MealType { "rice", "noodles", "other" };

            [Exposed=Window]
            interface Meal {
                attribute MealType type;
                attribute double size;     // in grams
            
                undefined initialize(MealType type, double size);
            };
        """.trimIndent()).apply {
            assertEquals(definitions.size, 2)

            assertEnum(definitions[0],
                name = "MealType",
                elements = listOf("rice", "noodles", "other")
            )
            assertInterface(definitions[1],
                name = "Meal",
                implements = null,
                definitions = 3,
                attributes = 1
            ) {
                assertAttributeIdent(attributes[0], "Exposed", "Window")

                assertField(definitions[0], "type", "MealType", isAttribute = true)
                assertField(definitions[1], "size", "double", isAttribute = true)
                assertFunction(definitions[2], "initialize", "undefined", argsCount = 2) {
                    assertField(args[0], "type", "MealType")
                    assertField(args[1], "size", "double")
                }
            }
        }
    }
}