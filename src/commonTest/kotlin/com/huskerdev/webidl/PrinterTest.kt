package com.huskerdev.webidl

import com.huskerdev.webidl.parser.IdlAsyncIterableLike
import com.huskerdev.webidl.parser.IdlCallbackFunction
import com.huskerdev.webidl.parser.IdlConstructor
import com.huskerdev.webidl.parser.IdlDefinitionRoot
import com.huskerdev.webidl.parser.IdlDictionary
import com.huskerdev.webidl.parser.IdlEnum
import com.huskerdev.webidl.parser.IdlEnumElement
import com.huskerdev.webidl.parser.IdlExtendedAttribute
import com.huskerdev.webidl.parser.IdlField
import com.huskerdev.webidl.parser.IdlGetter
import com.huskerdev.webidl.parser.IdlImplements
import com.huskerdev.webidl.parser.IdlIncludes
import com.huskerdev.webidl.parser.IdlInterface
import com.huskerdev.webidl.parser.IdlIterable
import com.huskerdev.webidl.parser.IdlMapLike
import com.huskerdev.webidl.parser.IdlNamespace
import com.huskerdev.webidl.parser.IdlOperation
import com.huskerdev.webidl.parser.IdlSetLike
import com.huskerdev.webidl.parser.IdlSetter
import com.huskerdev.webidl.parser.IdlStringifier
import com.huskerdev.webidl.parser.IdlTypeDef
import com.huskerdev.webidl.parser.IdlValue
import com.huskerdev.webidl.parser.IdlType
import kotlin.test.Test
import kotlin.test.assertEquals

class PrinterTest {

    @Test
    fun test(){
        val attrs = listOf(
            IdlExtendedAttribute.IdentifierValue("Exposed", "Window")
        )
        val args = listOf(
            IdlField(
                "a",
                IdlType.Default("DOMString", true),
                IdlValue.StringValue("text"),
                isOptional = true,
                attributes = attrs
            ),
            IdlField(
                "b",
                IdlType.Default("long long", false),
                isVariadic = true,
            )
        )

        val root = IdlDefinitionRoot(arrayListOf(
            IdlInterface("A", attributes = attrs),
            IdlInterface(
                "TestInterface",
                isCallback = true,
                isPartial = true,
                isMixin = true,
                implements = "A",
                attributes = attrs,
                definitions = arrayListOf(
                    IdlConstructor(
                        args = args,
                        attributes = attrs
                    ),
                    IdlOperation(
                        "testOperation",
                        type = IdlType.Default("void"),
                        args = args,
                        isStatic = true,
                        attributes = attrs
                    ),
                    IdlField(
                        "testField",
                        type = IdlType.Default("DOMString"),
                        value = IdlValue.StringValue("text"),
                        isAttribute = true,
                        isStatic = true,
                        isReadOnly = true,
                        isInherit = true,
                        isOptional = true,
                        isConst = true,
                        isRequired = true,
                        attributes = attrs
                    ),
                    IdlIterable(
                        keyType = IdlType.Default("Test")
                    ),
                    IdlIterable(
                        keyType = IdlType.Default("Test"),
                        valueType = IdlType.Default("Test2")
                    ),
                    IdlAsyncIterableLike(
                        keyType = IdlType.Default("Test")
                    ),
                    IdlAsyncIterableLike(
                        keyType = IdlType.Default("Test"),
                        valueType = IdlType.Default("Test2")
                    ),
                    IdlMapLike(
                        keyType = IdlType.Default("Test"),
                        valueType = IdlType.Default("Test2"),
                        isReadOnly = true
                    ),
                    IdlSetLike(
                        type = IdlType.Default("Test"),
                        isReadOnly = true
                    ),
                    IdlStringifier(),
                    IdlStringifier(IdlField("a", IdlType.Default("DOMString"))),
                    IdlGetter(IdlOperation("a", IdlType.Default("DOMString"))),
                    IdlSetter(IdlOperation("a", IdlType.Default("DOMString"))),
                )
            ),

            IdlNamespace(
                name = "TestNamespace",
                isPartial = true,
                attributes = attrs,
                definitions = arrayListOf(
                    IdlOperation(
                        "testOperation",
                        type = IdlType.Default("void"),
                        args = args,
                        attributes = attrs
                    ),
                    IdlField(
                        "testField",
                        type = IdlType.Default("DOMString"),
                        value = IdlValue.StringValue("text"),
                        attributes = attrs
                    ),
                )
            ),

            IdlDictionary("B"),
            IdlDictionary(
                name = "TestDictionary",
                implements = "B",
                isPartial = true,
                attributes = attrs,
                definitions = arrayListOf(
                    IdlOperation(
                        "testOperation",
                        type = IdlType.Default("void"),
                        args = args,
                        attributes = attrs
                    ),
                    IdlField(
                        "testField",
                        type = IdlType.Default("DOMString"),
                        value = IdlValue.StringValue("text"),
                        attributes = attrs
                    )
                )
            ),

            IdlCallbackFunction(
                name = "testCallback",
                operation = IdlOperation("", IdlType.Default("DOMString")),
                attributes = attrs
            ),

            IdlTypeDef(
                name = "Test",
                type = IdlType.Default("DOMString"),
                attributes = attrs
            ),

            IdlEnum(
                name = "TestEnum",
                attributes = attrs,
                definitions = arrayListOf(
                    IdlEnumElement("first"),
                    IdlEnumElement("second"),
                )
            ),

            IdlIncludes("A", "B"),
            IdlImplements("A", "B"),
        ))

        val string = WebIDLPrinter.print(root)

        println(string)

        assertEquals(string, """
            [Exposed=Window]
            interface A {
            };
            
            [Exposed=Window]
            partial callback interface mixin TestInterface: A {
              
              [Exposed=Window]
              constructor([Exposed=Window] optional DOMString? a = "text", long long... b);
              
              [Exposed=Window]
              static void testOperation([Exposed=Window] optional DOMString? a = "text", long long... b);
              
              [Exposed=Window]
              static readonly inherit optional const attribute required DOMString testField = "text";
              
              iterable<Test>;
              
              iterable<Test, Test2>;
              
              async_iterable<Test>;
              
              async_iterable<Test, Test2>;
              
              readonly maplike<Test, Test2>;
              
              readonly setlike<Test>;
              
              stringifier;
              
              stringifier DOMString a;
              
              getter DOMString a();
              
              setter DOMString a();
            };
            
            [Exposed=Window]
            partial namespace TestNamespace {
              
              [Exposed=Window]
              void testOperation([Exposed=Window] optional DOMString? a = "text", long long... b);
              
              [Exposed=Window]
              DOMString testField = "text";
            };
            
            dictionary B {
            };
            
            [Exposed=Window]
            partial dictionary TestDictionary: B {
              
              [Exposed=Window]
              void testOperation([Exposed=Window] optional DOMString? a = "text", long long... b);
              
              [Exposed=Window]
              DOMString testField = "text";
            };
            
            [Exposed=Window]
            callback testCallback = DOMString();
            
            [Exposed=Window]
            typedef DOMString Test;
            
            [Exposed=Window]
            enum TestEnum {
              "first",
              "second"
            };
            
            A includes B;
            
            A implements B;
        """.trimIndent())
    }
}