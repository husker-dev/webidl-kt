package com.huskerdev.webidl.resolver

import com.huskerdev.webidl.WebIDL
import kotlin.test.Test
import kotlin.test.assertEquals


class Dictionaries {

    @Test
    fun test1(){
        WebIDL.resolve("""
            dictionary Base {
                /* dictionary_members... */
            };
            
            dictionary Derived : Base {
                /* dictionary_members... */
            };
        """.trimIndent()).apply {
            assertDictionary(dictionaries.values.toList()[0], "Base")
            assertDictionary(dictionaries.values.toList()[1], "Derived", implements = dictionaries.values.toList()[0])
        }
    }

    @Test
    fun test2(){
        WebIDL.resolve("""
            dictionary Descriptor {
                DOMString name;
                sequence<unsigned long> serviceIdentifiers;
            };
        """.trimIndent()).apply {
            assertDictionary(dictionaries.values.toList()[0], "Descriptor") {
                assertField(fields[0], "name") {
                    assertDefaultType(type, "DOMString")
                }
                assertField(fields[1], "serviceIdentifiers") {
                    assertDefaultType(type, "sequence<unsigned long>", parameters = 1)
                }
            }
        }
    }

    @Test
    fun test3(){
        WebIDL.resolve("""
            dictionary SomeDictionary {
                /* dictionary_members... */
            };
            
            partial dictionary SomeDictionary {
                /* dictionary_members... */
            };
        """.trimIndent()).apply {
            assertEquals(1, dictionaries.size)
            assertDictionary(dictionaries.values.toList()[0], "SomeDictionary")
        }
    }

}