package com.huskerdev.webidl.resolver

import com.huskerdev.webidl.WebIDL
import kotlin.test.Test
import kotlin.test.assertEquals


class Typedefs {

    @Test
    fun test1(){
        WebIDL.resolve("""
            typedef long identifier;
        """.trimIndent()).apply {
            assertEquals(1, typeDefs.size)

            assertTypedef(typeDefs.values.first(), "identifier") {
                assertDefaultType(type, "long")
            }
        }
    }

    @Test
    fun test2(){
        WebIDL.resolve("""
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
            assertEquals(2, interfaces.size)
            assertEquals(1, typeDefs.size)

            assertInterface(interfaces.values.toList()[0], "Point") {
                assertField(fields[0], "x", isAttribute = true) {
                    assertDefaultType(type, "double")
                }
                assertField(fields[1], "y", isAttribute = true) {
                    assertDefaultType(type, "double")
                }
            }

            assertTypedef(typeDefs.values.first(), "Points") {
                assertDefaultType(type, "sequence<Point>", parameters = 1)
            }

            assertInterface(interfaces.values.toList()[1], "Widget") {
                assertOperation(operations[0], "pointWithinBounds") {
                    assertDefaultType(type, "boolean")
                    assertArgument(args[0], "p") {
                        assertDefaultType(type, "Point")
                    }
                }
                assertOperation(operations[1], "allPointsWithinBounds") {
                    assertDefaultType(type, "boolean")
                    assertArgument(args[0], "ps") {
                        assertDefaultType(type, "Points")
                    }
                }
            }
        }
    }
}