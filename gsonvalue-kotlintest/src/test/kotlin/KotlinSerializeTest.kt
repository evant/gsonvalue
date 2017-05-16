import com.google.gson.Gson
import com.google.gson.GsonBuilder
import me.tatarka.gsonvalue.ValueTypeAdapterFactory
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import serialize.IgnoreDataMethods

@RunWith(JUnit4::class)
class KotlinSerializeTest {

    lateinit var gson: Gson

    @Before
    fun setup() {
        gson = GsonBuilder()
                .registerTypeAdapterFactory(ValueTypeAdapterFactory())
                .create()
    }

    @Test
    fun serializeIgnoreDataMethods() {
        val ignoreDataMethods = IgnoreDataMethods(1, 2)
        val json = gson.toJson(ignoreDataMethods)

        assertEquals("{\"arg1\":1,\"arg2\":2}", json)
    }
}