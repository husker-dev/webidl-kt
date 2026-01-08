package com.huskerdev.webidl.parser

import com.huskerdev.webidl.WebIDL
import kotlin.test.Test
import kotlin.test.assertEquals


class Typedefs {

    @Test
    fun test1(){
        WebIDL.parseDefinitions("""
            typedef int identifier;
        """.trimIndent()).apply {
            assertEquals(definitions.size, 1)

            assertTypedef(definitions[0],
                type = "int",
                identifier = "identifier"
            )
        }
    }

    @Test
    fun test2(){
        WebIDL.parseDefinitions("""
            [Exposed=Window]
            interface Point {
                attribute double x;
                attribute double y;
            };
            
            typedef sequence<Point> Points;
            
            [Exposed=Window]
            interface Widget {
                boolean pointWithinBounds(Point p);
                boolean allPointsWithinBounds(Points ps);
            };
        """.trimIndent()).apply {
            assertEquals(definitions.size, 3)

            assertInterface(definitions[0],
                name = "Point",
                implements = null,
                definitions = 2,
                attributes = 1
            ) {
                assertAttributeIdent(attributes[0], "Exposed", "Window")

                assertField(definitions[0], "x", "double", isAttribute = true)
                assertField(definitions[1], "y", "double", isAttribute = true)
            }

            assertTypedef(definitions[1],
                type = "sequence<Point>",
                identifier = "Points"
            )

            assertInterface(definitions[2],
                name = "Widget",
                implements = null,
                definitions = 2,
                attributes = 1
            ) {
                assertAttributeIdent(attributes[0], "Exposed", "Window")

                assertOperation(definitions[0], "pointWithinBounds", "boolean", argsCount = 1) {
                    assertField(args[0], "p", "Point")
                }
                assertOperation(definitions[1], "allPointsWithinBounds", "boolean", argsCount = 1) {
                    assertField(args[0], "ps", "Points")
                }
            }
        }
    }
}