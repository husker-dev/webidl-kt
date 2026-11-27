package com.huskerdev.webidl

import com.huskerdev.webidl.lexer.WebIDLLexer
import com.huskerdev.webidl.parser.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class CommentsTest {
    private fun parse(source: String): IdlRoot {
        val (root, errors) = WebIDL.parseDefinitions(source.trimIndent())
        assertTrue(errors.isEmpty(), errors.joinToString())
        return root
    }

    private fun IdlDefinition.comments() = (this as IdlCommentHolder).leadingComments.map { it.rawText }

    @Test
    fun declarationsAndMembers() {
        val root = parse("""
            /** Interface */
            [Exposed=Window]
            interface Example {
              // Constructor
              constructor();
              /* Field */
              [SameObject]
              readonly attribute DOMString name;
              // Operation
              undefined run(
                // Argument
                [Clamp] long count
              );
              // Getter
              getter DOMString (long index);
              // Setter
              setter undefined (long index, DOMString value);
              // Stringifier
              stringifier;
              // Iterable
              iterable<DOMString>;
              // Async iterable
              async_iterable<DOMString>;
              // Map
              readonly maplike<DOMString, long>;
              // Set
              readonly setlike<DOMString>;
            };
            // Dictionary
            dictionary Options {
              // Dictionary field
              long count;
            };
            // Namespace
            namespace Tools {
              // Namespace method
              undefined run();
            };
            // Enum
            enum Mode {
              // Value
              "first"
            };
            // Typedef
            typedef long Count;
            // Callback
            callback Handler = undefined();
            // Mixin
            interface mixin Extra {};
            // Includes
            Example includes Extra;
            // Implements
            Example implements Extra;
            // Callback interface
            callback interface Listener {};
            // Partial
            partial interface Example {};
        """)
        assertEquals(listOf("/** Interface */"), root.definitions.first().comments())
        val inter = assertIs<IdlInterface>(root.definitions.first())
        assertEquals(listOf("Constructor", "Field", "Operation", "Getter", "Setter", "Stringifier",
            "Iterable", "Async iterable", "Map", "Set"), inter.definitions.map {
            it.comments().single().removePrefix("// ").removePrefix("/* ").removeSuffix(" */")
        })
        val operation = assertIs<IdlOperation>(inter.definitions[2])
        assertEquals(listOf("// Argument"), operation.args.single().comments())
        assertEquals(listOf("// Dictionary field"), assertIs<IdlDictionary>(root.definitions[1]).definitions.single().comments())
        assertEquals(listOf("// Namespace method"), assertIs<IdlNamespace>(root.definitions[2]).definitions.single().comments())
        assertEquals(listOf("// Value"), assertIs<IdlEnum>(root.definitions[3]).elements.single().comments())
        assertTrue(root.definitions.all { it.comments().size == 1 })
        // Annotations and names are not documentation holders.
        assertFalse((inter.attributes as IdlDefinition) is IdlCommentHolder)
        assertFalse((inter.name as IdlDefinition) is IdlCommentHolder)
    }

    @Test
    fun blankLinesBreakAttachment() {
        for (newline in listOf("\n", "\r\n", "\r")) {
            for (comment in listOf("// old", "/* old */")) {
                val root = parse("$comment$newline  $newline interface A {};")
                assertTrue(root.definitions.single().comments().isEmpty())
            }
            val root = parse("// old$newline$newline// kept$newline/* also kept */${newline}interface A {};")
            assertEquals(listOf("// kept", "/* also kept */"), root.definitions.single().comments())
        }
        val root = parse("/* inline */interface A {}; // orphan\n\ninterface B {};")
        assertEquals(listOf("/* inline */"), root.definitions[0].comments())
        assertTrue(root.definitions[1].comments().isEmpty())
        val annotated = parse("// Detached\n[Exposed=Window]\n\ninterface A {};")
        assertTrue(annotated.definitions.single().comments().isEmpty())
    }

    @Test
    fun annotationsBelongToDeclaration() {
        val root = parse("""
            // Before annotations
            [/* Not documentation */ Exposed=Window]
            /* After annotations */
            interface A {
              // Before annotations
              [Clamp]
              /* After annotations */
              attribute long count;
            };
        """)
        val inter = assertIs<IdlInterface>(root.definitions.single())
        val expected = listOf("// Before annotations", "/* After annotations */")
        assertEquals(expected, inter.comments())
        assertEquals(expected, inter.definitions.single().comments())
    }

    @Test
    fun commentsAreAvailableWhenStreaming() {
        val received = mutableListOf<List<String>>()
        WebIDL.streamDefinitions("// A\n[Exposed=Window] interface A { // field\nattribute long count; };",
            object: IdlParserConsumer {
                override fun enter(definition: IdlDefinition) {
                    if (definition is IdlCommentHolder)
                        received += definition.leadingComments.map { it.rawText }
                }
                override fun exit() = Unit
                override fun error(exception: WebIDLSyntaxErrorException) = throw exception
            })
        assertEquals(listOf(listOf("// A"), listOf("// field")), received)
    }

    @Test
    fun lexerPreservesTriviaAndBounds() {
        val lexer = WebIDLLexer("Foo/* doc */ Bar".iterator())
        assertEquals("Foo", lexer.current.content)
        val next = lexer.next()
        assertEquals("Bar", next.content)
        assertEquals(listOf("/* doc */", " "), next.leadingTrivia.map { it.content })
        assertEquals(3, next.leadingTrivia.first().bounds.startOffset)
        assertEquals(12, next.leadingTrivia.first().bounds.endOffset)
        assertEquals(13, next.bounds.startOffset)
        assertTrue(lexer.errors.isEmpty())
        val (_, errors) = WebIDL.parseDefinitions("interface A {}; /* unfinished")
        assertTrue(errors.any { "Unterminated block comment" in it.message.orEmpty() })
    }

    @Test
    fun printerRoundTrip() {
        val root = parse("""
            /** Interface */
            [Exposed=Window]
            interface A {
              /**
               * Field documentation
               */
              [Clamp]
              attribute long count;
              // Method
              undefined run(
                // Argument
                long count
              );
            };
            // Enum
            enum Mode {
              // Value
              "first"
            };
        """)
        val printed = WebIDLPrinter.print(root)
        val reparsed = parse(printed)
        fun comments(root: IdlDefinition): List<String> = buildList {
            if (root is IdlCommentHolder) addAll(root.leadingComments.map { it.rawText.trimIndent() })
            root.children.forEach { addAll(comments(it)) }
        }
        assertEquals(comments(root), comments(reparsed))
        assertEquals(printed, WebIDLPrinter.print(reparsed))
        assertTrue(printed.startsWith("/** Interface */\n[Exposed=Window]\ninterface"))
    }

    @Test
    fun resolverPreservesDocumentation() {
        val resolver = WebIDL.resolve("""
            // Interface
            interface A {
              // Constructor
              constructor();
              // Field
              attribute long count;
              // Method
              undefined run(
                // Argument
                long count
              );
              // Getter
              getter DOMString (long index);
              // Setter
              setter undefined (long index, DOMString value);
              // Stringifier
              stringifier;
              // Iterable
              iterable<DOMString>;
              // Async iterable
              async_iterable<DOMString>;
              // Map
              maplike<DOMString, long>;
              // Set
              setlike<DOMString>;
            };
            // Partial documentation does not replace the main declaration
            partial interface A {
              // Partial member
              undefined extra();
            };
            // Mixin
            interface mixin Extra {
              // Mixin member
              undefined mixed();
            };
            A includes Extra;
            // Dictionary
            dictionary Options {
              // Dictionary field
              long count;
            };
            // Namespace
            namespace Tools {
              // Namespace method
              undefined run();
            };
            // Typedef
            typedef long Count;
            // Callback
            callback Handler = undefined();
            // Enum
            enum Mode {
              // Value
              "first"
            };
        """.trimIndent())
        assertTrue(resolver.mergedErrors.isEmpty(), resolver.mergedErrors.joinToString())
        val inter = resolver.interfaces.getValue("A")
        assertEquals("// Interface", inter.leadingComments.single().rawText)
        assertEquals("// Field", inter.fields.single().leadingComments.single().rawText)
        assertEquals("// Constructor", inter.constructors.single().leadingComments.single().rawText)
        val operation = inter.operations.first()
        assertEquals("// Method", operation.leadingComments.single().rawText)
        assertEquals("// Argument", operation.args.single().leadingComments.single().rawText)
        assertEquals("// Partial member", inter.operations[1].leadingComments.single().rawText)
        assertEquals("// Mixin member", inter.operations[2].leadingComments.single().rawText)
        assertEquals("// Getter", inter.getter!!.leadingComments.single().rawText)
        assertEquals("// Setter", inter.setter!!.leadingComments.single().rawText)
        assertEquals("// Stringifier", inter.stringifierComments.single().rawText)
        assertEquals("// Iterable", inter.iterableComments.single().rawText)
        assertEquals("// Async iterable", inter.asyncIterableComments.single().rawText)
        assertEquals("// Map", inter.mapComments.single().rawText)
        assertEquals("// Set", inter.setComments.single().rawText)
        assertEquals("// Dictionary", resolver.dictionaries.getValue("Options").leadingComments.single().rawText)
        assertEquals("// Dictionary field", resolver.dictionaries.getValue("Options").fields.single().leadingComments.single().rawText)
        assertEquals("// Namespace", resolver.namespaces.getValue("Tools").leadingComments.single().rawText)
        assertEquals("// Namespace method", resolver.namespaces.getValue("Tools").operations.single().leadingComments.single().rawText)
        assertEquals("// Typedef", resolver.typeDefs.getValue("Count").leadingComments.single().rawText)
        assertEquals("// Callback", resolver.callbacks.getValue("Handler").leadingComments.single().rawText)
        assertEquals("// Enum", resolver.enums.getValue("Mode").leadingComments.single().rawText)
        assertEquals("// Value", resolver.enums.getValue("Mode").elementComments.getValue("first").single().rawText)
    }
}
