package com.huskerdev.webidl.parser

import com.huskerdev.webidl.WebIDL
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class Offsets {

    private fun String.range(def: IdlDefinition) =
        substring(def.bounds.startOffset, def.bounds.endOffset)

    private fun String.index(content: String) =
        indexOf(content).let { if (it == -1) error("substring not found: '$content'") else it }

    private fun List<IdlDefinition>.walkWith(parent: IdlDefinition?): List<Pair<IdlDefinition?, IdlDefinition>> =
        flatMap { def ->
            val children: List<IdlDefinition> = when (def) {
                is IdlInterface -> def.definitions
                is IdlNamespace -> def.definitions
                is IdlDictionary -> def.definitions
                is IdlEnum -> def.elements.map { it as IdlDefinition }
                else -> emptyList()
            }
            listOf(parent to def) + children.walkWith(def)
        }

    @Test
    fun rootCoversWholeSource() {
        val source = "interface A { undefined f(); };"
        val root = WebIDL.parseDefinitions(source).first
        assertEquals(0, root.bounds.startOffset)
        assertEquals(source.length, root.bounds.endOffset)
    }

    @Test
    fun interfaceAndMembers() {
        val source = """
            [Exposed=Window]
            interface Foo {
                attribute DOMString name;
                const long answer = 42;
                undefined doThing(DOMString id, optional long? count);
            };
        """.trimIndent()

        val root = WebIDL.parseDefinitions(source).first
        val iface = assertIs<IdlInterface>(root.definitions[0])

        assertEquals(0, iface.bounds.startOffset)
        assertTrue(source.range(iface).startsWith("[Exposed=Window]\ninterface Foo {"), source.range(iface))
        assertTrue(source.range(iface).endsWith("};"), source.range(iface))

        assertEquals("Exposed=Window", source.range(iface.attributes!![0]))

        val field = iface.definitions.first { it is IdlField && it.name.text == "name" }
        assertEquals(source.index("attribute DOMString name;"), field.bounds.startOffset)
        assertEquals("attribute DOMString name;", source.range(field))

        val constant = iface.definitions.first { it is IdlField && it.name.text == "answer" }
        assertEquals("const long answer = 42;", source.range(constant))

        val op = iface.definitions.first { it is IdlOperation && it.name.text == "doThing" }
        assertEquals("undefined doThing(DOMString id, optional long? count);", source.range(op))
    }

    @Test
    fun enumElementsPointAtTheirStrings() {
        val source = """
            enum Color {
                "red",
                "green",
                "blue"
            };
        """.trimIndent()

        val root = WebIDL.parseDefinitions(source).first
        val enum = assertIs<IdlEnum>(root.definitions[0])

        assertTrue(source.range(enum).startsWith("enum Color "), source.range(enum))
        assertTrue(source.range(enum).endsWith("};"), source.range(enum))

        assertEquals(listOf("\"red\"", "\"green\"", "\"blue\""), enum.elements.map { source.range(it) })
        assertEquals(listOf("red", "green", "blue"), enum.elements.map { it.name.text })
    }

    @Test
    fun extendedAttributesRanges() {
        val source = """
            [Exposed=(Window,Worker), LegacyFactoryFunction=Image(DOMString src), Constructor(double x), Reflect="popover"]
            interface A {
                undefined f();
            };
        """.trimIndent()

        val iface = assertIs<IdlInterface>(WebIDL.parseDefinitions(source).first.definitions[0])
        val attrs = iface.attributes

        assertEquals(4, attrs!!.size)
        assertEquals("Exposed=(Window,Worker)", source.range(attrs[0]))
        assertEquals("LegacyFactoryFunction=Image(DOMString src)", source.range(attrs[1]))
        assertEquals("Constructor(double x)", source.range(attrs[2]))
        assertEquals("Reflect=\"popover\"", source.range(attrs[3]))
    }

    @Test
    fun variousDefinitions() {
        val source = """
            typedef unsigned long long BigInt;
            
            callback SimpleCallback = short (long value);
            
            dictionary Point {
                required double x;
                double y = 0.0;
            };
            
            namespace Ops {
                void doIt();
            };
            
            interface A { };
            A includes B;
        """.trimIndent()

        val root = WebIDL.parseDefinitions(source).first

        val typedef = assertIs<IdlTypeDef>(root.definitions[0])
        assertEquals("typedef unsigned long long BigInt;", source.range(typedef))

        val callback = assertIs<IdlCallbackFunction>(root.definitions[1])
        assertEquals("callback SimpleCallback = short (long value);", source.range(callback))

        val dict = assertIs<IdlDictionary>(root.definitions[2])
        assertTrue(source.range(dict).startsWith("dictionary Point {"), source.range(dict))
        assertTrue(source.range(dict).endsWith("};"), source.range(dict))
        assertEquals("required double x;", source.range(dict.definitions.first { it is IdlField && it.name.text == "x" }))
        assertEquals("double y = 0.0;", source.range(dict.definitions.first { it is IdlField && it.name.text == "y" }))

        val ns = assertIs<IdlNamespace>(root.definitions[3])
        assertTrue(source.range(ns).endsWith("};"), source.range(ns))
        assertEquals("void doIt();", source.range(ns.definitions.first { it is IdlOperation && it.name.text == "doIt" }))

        val includes = assertIs<IdlIncludes>(root.definitions[5])
        assertEquals("A includes B;", source.range(includes))
    }

    @Test
    fun rangesWithinParents() {
        val source = """
            interface A {
                undefined f(long x);
                constructor(double r);
                readonly maplike<DOMString, long>;
                iterable<long>;
                stringifier;
                getter double (DOMString p);
                setter undefined (DOMString p, double v);
            };
            enum E {
                "a",
                "b"
            };
            namespace N {
                void g();
            };
            dictionary D {
                long z;
            };
        """.trimIndent()

        val root = WebIDL.parseDefinitions(source).first
        val pairs = root.definitions.walkWith(root)

        assertTrue(pairs.isNotEmpty())
        for ((parent, child) in pairs) {
            assertTrue(child.bounds.startOffset >= 0, child::class.simpleName.toString())
            assertTrue(child.bounds.endOffset >= child.bounds.startOffset, child::class.simpleName.toString())
            assertTrue(child.bounds.startOffset >= parent!!.bounds.startOffset, child::class.simpleName.toString())
            assertTrue(child.bounds.endOffset <= parent.bounds.endOffset, child::class.simpleName.toString())
            assertTrue(source.range(child).isNotBlank(), child::class.simpleName.toString())
        }
    }

    @Test
    fun operationArgumentsHaveOffsets() {
        val source = "interface A { undefined f(long a, long b, optional DOMString id = \"x\"); };"
        val root = WebIDL.parseDefinitions(source).first
        val iface = assertIs<IdlInterface>(root.definitions[0])
        val op = assertIs<IdlOperation>(iface.definitions.first { it is IdlOperation && it.name.text == "f" })

        assertEquals(3, op.args.size)
        assertEquals("long a", source.range(op.args[0]))
        assertEquals("long b", source.range(op.args[1]))
        assertEquals("optional DOMString id = \"x\"", source.range(op.args[2]))

        assertTrue(op.args.take(2).all { source[it.bounds.endOffset] == ',' })
        assertTrue(source[op.args[2].bounds.endOffset] == ')')
    }

    @Test
    fun callbackOperationRange() {
        val source = "callback SimpleCallback = short (long value);"
        val root = WebIDL.parseDefinitions(source).first
        val callback = assertIs<IdlCallbackFunction>(root.definitions[0])

        assertEquals("callback SimpleCallback = short (long value);", source.range(callback))
        assertEquals("short (long value)", source.range(callback.operation))
        assertEquals("long value", source.range(callback.operation.args.single()))
    }

    @Test
    fun specialMembersRanges() {
        val source = """
            interface A {
                stringifier attribute DOMString toJSON;
                getter double (DOMString p);
                setter undefined (DOMString p, double v);
                constructor(double r);
            };
        """.trimIndent()

        val root = WebIDL.parseDefinitions(source).first
        val iface = assertIs<IdlInterface>(root.definitions[0])

        val stringifier = iface.definitions.first { it is IdlStringifier } as IdlStringifier
        assertEquals("attribute DOMString toJSON", source.range(stringifier.field!!))

        val getter = iface.definitions.first { it is IdlGetter } as IdlGetter
        assertEquals("double (DOMString p)", source.range(getter.operation))
        assertEquals("DOMString p", source.range(getter.operation.args.single()))

        val setter = iface.definitions.first { it is IdlSetter } as IdlSetter
        assertEquals("undefined (DOMString p, double v)", source.range(setter.operation))
        assertEquals("DOMString p", source.range(setter.operation.args[0]))
        assertEquals("double v", source.range(setter.operation.args[1]))

        val constructor = iface.definitions.first { it is IdlConstructor } as IdlConstructor
        assertEquals("double r", source.range(constructor.args.single()))
    }
}