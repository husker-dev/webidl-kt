import com.huskerdev.webidl.WebIDL
import com.huskerdev.webidl.jvm.iterator
import com.huskerdev.webidl.parser.assertField
import com.huskerdev.webidl.parser.assertInterface
import kotlin.test.Test
import kotlin.test.assertEquals


class Files {

    @Test
    fun test() {
        WebIDL.parseDefinitions(
            this::class.java.getResourceAsStream("/someFile.idl")!!.iterator()
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

