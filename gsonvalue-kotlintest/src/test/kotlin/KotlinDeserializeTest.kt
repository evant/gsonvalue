import com.google.gson.Gson
import com.google.gson.GsonBuilder
import me.tatarka.gsonvalue.ValueTypeAdapterFactory
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class KotlinDeserializeTest {

    lateinit var gson: Gson

    @Before
    fun setup() {
        gson = GsonBuilder()
                .registerTypeAdapterFactory(ValueTypeAdapterFactory())
                .create()
    }

    @Test
    fun deserializeEmpty() {
        val empty = gson.fromJson("{}", Empty::class.java)

        assertNotNull(empty)
    }

    @Test
    fun deserializeOneArgConstructor() {
        val constructor = gson.fromJson("{\"arg\":1}", OneArgConstructor::class.java)

        assertNotNull(constructor)
        assertEquals(1, constructor.arg)
        assertTrue(constructor.calledConstructor)
    }

    @Test
    fun deserializeDataOneArgConstructor() {
        val constructor = gson.fromJson("{\"arg\":1}", DataOneArgConstructor::class.java)

        assertNotNull(constructor)
        assertEquals(1, constructor.arg)
    }
}
