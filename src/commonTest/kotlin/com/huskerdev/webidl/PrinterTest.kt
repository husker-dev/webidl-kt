package com.huskerdev.webidl

import com.huskerdev.webidl.parser.*
import kotlin.test.Test
import kotlin.test.assertEquals

class PrinterTest {

    @Test
    fun test(){
        val attrs = IdlAttributes(mutableListOf(
            IdlExtendedAttribute.IdentifierValue(IdlName("Exposed"), IdlName("Window"))
        ))
        val args = listOf(
            IdlField(
                IdlName("a"),
                IdlType.Default(IdlName("DOMString"), true),
                IdlValue.StringValue("text"),
                isOptional = true,
                attributes = attrs
            ),
            IdlField(
                IdlName("b"),
                IdlType.Default(IdlName("long long"), false),
                isVariadic = true,
            )
        )

        val root = IdlRoot(definitions = arrayListOf(
            IdlInterface(IdlName("A"), attributes = attrs),
            IdlInterface(
                IdlName("TestInterface"),
                isCallback = true,
                isPartial = true,
                isMixin = true,
                implements = IdlName("A"),
                attributes = attrs,
                definitions = arrayListOf(
                    IdlConstructor(
                        args = args,
                        attributes = attrs
                    ),
                    IdlOperation(
                        IdlName("testOperation"),
                        type = IdlType.Default(IdlName("void")),
                        args = args,
                        isStatic = true,
                        attributes = attrs
                    ),
                    IdlField(
                        IdlName("testField"),
                        type = IdlType.Default(IdlName("DOMString")),
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
                        keyType = IdlType.Default(IdlName("Test"))
                    ),
                    IdlIterable(
                        keyType = IdlType.Default(IdlName("Test")),
                        valueType = IdlType.Default(IdlName("Test2"))
                    ),
                    IdlAsyncIterableLike(
                        keyType = IdlType.Default(IdlName("Test"))
                    ),
                    IdlAsyncIterableLike(
                        keyType = IdlType.Default(IdlName("Test")),
                        valueType = IdlType.Default(IdlName("Test2"))
                    ),
                    IdlMapLike(
                        keyType = IdlType.Default(IdlName("Test")),
                        valueType = IdlType.Default(IdlName("Test2")),
                        isReadOnly = true
                    ),
                    IdlSetLike(
                        type = IdlType.Default(IdlName("Test")),
                        isReadOnly = true
                    ),
                    IdlStringifier(),
                    IdlStringifier(IdlField(IdlName("a"), IdlType.Default(IdlName("DOMString")))),
                    IdlGetter(IdlOperation(IdlName("a"), IdlType.Default(IdlName("DOMString")))),
                    IdlSetter(IdlOperation(IdlName("a"), IdlType.Default(IdlName("DOMString")))),
                )
            ),

            IdlNamespace(
                name = IdlName("TestNamespace"),
                isPartial = true,
                attributes = attrs,
                definitions = arrayListOf(
                    IdlOperation(
                        IdlName("testOperation"),
                        type = IdlType.Default(IdlName("void")),
                        args = args,
                        attributes = attrs
                    ),
                    IdlField(
                        IdlName("testField"),
                        type = IdlType.Default(IdlName("DOMString")),
                        value = IdlValue.StringValue("text"),
                        attributes = attrs
                    ),
                )
            ),

            IdlDictionary(IdlName("B")),
            IdlDictionary(
                name = IdlName("TestDictionary"),
                implements = IdlName("B"),
                isPartial = true,
                attributes = attrs,
                definitions = arrayListOf(
                    IdlOperation(
                        IdlName("testOperation"),
                        type = IdlType.Default(IdlName("void")),
                        args = args,
                        attributes = attrs
                    ),
                    IdlField(
                        IdlName("testField"),
                        type = IdlType.Default(IdlName("DOMString")),
                        value = IdlValue.StringValue("text"),
                        attributes = attrs
                    )
                )
            ),

            IdlCallbackFunction(
                name = IdlName("testCallback"),
                operation = IdlOperation(IdlName(""), IdlType.Default(IdlName("DOMString"))),
                attributes = attrs
            ),

            IdlTypeDef(
                name = IdlName("Test"),
                type = IdlType.Default(IdlName("DOMString")),
                attributes = attrs
            ),

            IdlEnum(
                name = IdlName("TestEnum"),
                attributes = attrs,
                elements = arrayListOf(
                    IdlEnumElement(IdlName("first")),
                    IdlEnumElement(IdlName("second")),
                )
            ),

            IdlIncludes(IdlName("A"), IdlName("B")),
            IdlImplements(IdlName("A"), IdlName("B")),
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