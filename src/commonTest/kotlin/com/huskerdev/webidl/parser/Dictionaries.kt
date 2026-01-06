package com.huskerdev.webidl.parser

import com.huskerdev.webidl.WebIDL
import kotlin.test.Test
import kotlin.test.assertEquals


class Dictionaries {

    @Test
    fun test1(){
        WebIDL.parseDefinitions("""
            dictionary Base {
                /* dictionary_members... */
            };
            
            dictionary Derived : Base {
                /* dictionary_members... */
            };
        """.trimIndent()).apply {
            assertEquals(definitions.size, 2)

            assertDictionary(definitions[0],
                name = "Base",
                implements = null,
                definitions = 0
            )
            assertDictionary(definitions[1],
                name = "Derived",
                implements = "Base",
                definitions = 0
            )
        }
    }

    @Test
    fun test2(){
        WebIDL.parseDefinitions("""
            dictionary Descriptor {
                DOMString name;
                sequence<unsigned long> serviceIdentifiers;
            };
        """.trimIndent()).apply {
            assertEquals(definitions.size, 1)

            assertDictionary(definitions[0],
                name = "Descriptor",
                implements = null,
                definitions = 2
            ) {
                assertField(definitions[0], "name", "DOMString")
                assertField(definitions[1], "serviceIdentifiers", "sequence<unsigned long>")
            }
        }
    }

    @Test
    fun test3(){
        WebIDL.parseDefinitions("""
            dictionary SomeDictionary {
                /* dictionary_members... */
            };
            
            partial dictionary SomeDictionary {
                /* dictionary_members... */
            };
        """.trimIndent()).apply {
            assertEquals(definitions.size, 2)

            assertDictionary(definitions[0],
                name = "SomeDictionary",
                implements = null,
                definitions = 0
            )
            assertDictionary(definitions[1],
                name = "SomeDictionary",
                implements = null,
                definitions = 0,
                isPartial = true
            )
        }
    }

}