import com.huskerdev.webidl.WebIDL
import com.huskerdev.webidl.jvm.iterator
import com.huskerdev.webidl.parser.assertField
import com.huskerdev.webidl.parser.assertInterface
import kotlin.test.Test
import kotlin.test.assertEquals


class Files {

    @Test
    fun test() {
        val stream = this::class.java.getResourceAsStream("/someFile.idl")!!

        WebIDL.parseDefinitions(
            stream.reader().buffered().iterator()
        ).apply {
            assertEquals(1, definitions.size)

            assertInterface(definitions[0],
                name = "TestInterface",
                implements = null,
                definitions = 1
            ) {
                assertField(definitions[0], "field", "long")
            }
        }
    }
}

