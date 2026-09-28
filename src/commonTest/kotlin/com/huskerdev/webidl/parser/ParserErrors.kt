package com.huskerdev.webidl.parser

import com.huskerdev.webidl.WebIDL
import com.huskerdev.webidl.WebIDLSyntaxErrorException
import com.huskerdev.webidl.WebIDLUnexpectedSymbolException
import com.huskerdev.webidl.WebIDLWrongSymbolException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue


class ParserErrors {

    private fun parseWithErrors(source: String): IdlParserConsumer.Collector =
        IdlParserConsumer.Collector().apply {
            WebIDL.streamDefinitions(source, this)
        }

    private fun IdlDefinition.nameIfAny(): String? = when (this) {
        is IdlInterface -> name
        is IdlNamespace -> name
        is IdlEnum -> name
        is IdlDictionary -> name
        is IdlTypeDef -> name
        is IdlCallbackFunction -> name
        else -> null
    }?.text

    private fun List<IdlDefinition>.names(): List<String> =
        mapNotNull { it.nameIfAny() }

    @Test
    fun validFileCollectsNoErrors() {
        val source = """
            namespace Ops {
                void doIt();
            };
            enum Color {
                "red",
                "green",
                "blue"
            };
            interface A {
                undefined f(long a, long b);
            };
        """.trimIndent()

        val collector = parseWithErrors(source)

        assertTrue(collector.errors.isEmpty())
        assertEquals(3, collector.root.definitions.size)
    }

    @Test
    fun unknownTopLevelDefinitionsReportedAndParsingContinues() {
        val source = """
            interface A {
                undefined f();
            };
            garbage1;
            garbage2;
            interface B { };
        """.trimIndent()

        val collector = parseWithErrors(source)

        assertEquals(2, collector.errors.size)
        for (error in collector.errors) {
            assertIs<WebIDLWrongSymbolException>(error)
            assertTrue(error.errorMessage.contains("includes"), error.errorMessage)
        }

        assertEquals(listOf("A", "B"), collector.root.definitions.names())
    }

    @Test
    fun invalidMembersCollectedInsideInterface() {
        val source = """
            interface A {
                123;
                true;
                undefined ok();
            };
        """.trimIndent()

        val collector = parseWithErrors(source)

        assertEquals(2, collector.errors.size)
        assertEquals("Expected 'type' but found: '123'.", collector.errors[0].errorMessage)
        assertEquals("Expected 'type' but found: 'true'.", collector.errors[1].errorMessage)

        val iface = assertIs<IdlInterface>(collector.root.definitions.single())
        assertEquals(listOf("ok"), iface.definitions.filterIsInstance<IdlOperation>().map { it.name.text })
    }

    @Test
    fun missingOpeningBraceReportsErrorAndParsingContinues() {
        val source = """
            interface A
                undefined f();
            interface B { };
        """.trimIndent()

        val collector = parseWithErrors(source)

        assertEquals(2, collector.errors.size)
        assertEquals("Expected '{' but found: 'undefined'.", collector.errors[0].errorMessage)
        assertEquals(listOf("B"), collector.root.definitions.names())
    }

    @Test
    fun missingOpeningBraceWithLeadingTypeDoesNotHang() {
        val source = """
            interface A
                undefined f();
                undefined g();
            interface B { };
        """.trimIndent()

        val collector = parseWithErrors(source)

        assertEquals(4, collector.errors.size)
        assertEquals("Expected '{' but found: 'undefined'.", collector.errors[0].errorMessage)
        assertEquals(listOf("B"), collector.root.definitions.names())
    }

    @Test
    fun missingSemicolonInInterfaceMembersRecoversAtTopLevel() {
        val source = """
            interface A {
                undefined f()
                undefined g()
            };
            interface B { };
        """.trimIndent()

        val collector = parseWithErrors(source)

        assertEquals(2, collector.errors.size)
        assertEquals("Expected ';' but found: 'undefined'.", collector.errors[0].errorMessage)
        assertEquals("Expected ';' but found: '}'.", collector.errors[1].errorMessage)
        assertEquals(listOf("A", "B"), collector.root.definitions.names())
        assertEquals(listOf("f", "g"), assertIs<IdlInterface>(collector.root.definitions[0])
            .definitions.filterIsInstance<IdlOperation>().map { it.name.text })
    }

    @Test
    fun unknownKeywordIsReported() {
        val source = "or;\ninterface Fine { };"

        val collector = parseWithErrors(source)

        assertEquals(1, collector.errors.size)
        val error = collector.errors.single()
        assertIs<WebIDLUnexpectedSymbolException>(error)
        assertEquals("Unexpected symbol: or.", error.errorMessage)
        assertTrue(error.message!!.startsWith("Syntax error | Unexpected symbol: or"))
        assertEquals(listOf("Fine"), collector.root.definitions.names())
    }

    @Test
    fun multipleErrorsAccumulateAcrossFile() {
        val source = """
            garbage1;
            garbage2;
            interface B { };
            garbage3;
        """.trimIndent()

        val collector = parseWithErrors(source)

        assertEquals(3, collector.errors.size)
        assertEquals(listOf("B"), collector.root.definitions.names())
    }

    @Test
    fun errorsAreWebIDLParserExceptions() {
        val collector = parseWithErrors("garbage;")

        assertEquals(1, collector.errors.size)
        assertIs<WebIDLSyntaxErrorException>(collector.errors.single())
    }

    @Test
    fun enumWithInvalidElementTerminatesAndKeepsValidElements() {
        val source = """
            enum Color {
                "red",
                42,
                "green"
            };
        """.trimIndent()

        val collector = parseWithErrors(source)

        assertEquals(1, collector.errors.size)

        val enum = assertIs<IdlEnum>(collector.root.definitions.single())
        assertEquals(listOf("red", "green"), enum.elements.map { it.name.text })
    }

    @Test
    fun unterminatedArgumentsTerminate() {
        val source = "interface A { undefined f(long a, };"

        val collector = parseWithErrors(source)

        assertTrue(collector.errors.isNotEmpty())
        val iface = assertIs<IdlInterface>(collector.root.definitions.single())
        val op = assertIs<IdlOperation>(iface.definitions.single())
        assertEquals(listOf("a"), op.args.map { it.name.text })
    }
}