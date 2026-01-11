package com.huskerdev.webidl.resolver

import com.huskerdev.webidl.WebIDL
import kotlin.test.Test


class Enum {

    @Test
    fun test1(){
        WebIDL.resolve("""
            enum identifier { "enum", "values" /* , ... */ };
        """.trimIndent()).apply {
            assertEnum(enums.values.toList()[0], "identifier", listOf("enum", "values"))
        }
    }

    @Test
    fun test2(){
        WebIDL.resolve("""
            enum MealType { "rice", "noodles", "other" };

            [Exposed=Window]
            interface Meal {
                attribute MealType type;
                attribute double size;     // in grams
            
                undefined initialize(MealType type, double size);
            };
        """.trimIndent()).apply {
            assertEnum(enums.values.toList()[0], "MealType", listOf("rice", "noodles", "other"))

            assertInterface(interfaces.values.toList()[0], "Meal") {
                assertField(fields[0], "type", isAttribute = true) {
                    assertDefaultType(type, "MealType")
                }
                assertField(fields[1], "size", isAttribute = true) {
                    assertDefaultType(type, "double")
                }
                assertOperation(operations[0], "initialize") {
                    assertDefaultType(type, "undefined")
                    assertArgument(args[0], "type") {
                        assertDefaultType(type, "MealType")
                    }
                    assertArgument(args[1], "size") {
                        assertDefaultType(type, "double")
                    }
                }
            }
        }
    }
}