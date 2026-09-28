package com.huskerdev.webidl.parser

import com.huskerdev.webidl.WebIDL
import kotlin.test.Test
import kotlin.test.assertEquals


class Typedefs {

    @Test
    fun test1(){
        WebIDL.parseDefinitions("""
            typedef long identifier;
        """.trimIndent()).first.apply {
            assertEquals(1, this.definitions.size)

            assertTypedef(
                this.definitions[0],
                type = "long",
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
        """.trimIndent()).first.apply {
            assertEquals(3, this.definitions.size)

            assertInterface(
                this.definitions[0],
                name = "Point",
                implements = null,
                definitions = 2,
                attributes = 1
            ) {
                assertAttributeIdent(attributes!![0], "Exposed", "Window")

                assertField(this.definitions[0], "x", "double", isAttribute = true)
                assertField(this.definitions[1], "y", "double", isAttribute = true)
            }

            assertTypedef(
                this.definitions[1],
                type = "sequence<Point>",
                identifier = "Points"
            )

            assertInterface(
                this.definitions[2],
                name = "Widget",
                implements = null,
                definitions = 2,
                attributes = 1
            ) {
                assertAttributeIdent(attributes!![0], "Exposed", "Window")

                assertOperation(this.definitions[0], "pointWithinBounds", "boolean", argsCount = 1) {
                    assertField(args[0], "p", "Point")
                }
                assertOperation(this.definitions[1], "allPointsWithinBounds", "boolean", argsCount = 1) {
                    assertField(args[0], "ps", "Points")
                }
            }
        }
    }
}