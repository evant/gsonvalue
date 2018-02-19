import com.google.gson.TypeAdapterFactory
import me.tatarka.gsonvalue.annotations.GsonValueTypeAdapterFactory

@GsonValueTypeAdapterFactory
abstract class ValueTypeAdapterFactory : TypeAdapterFactory {
    companion object {
        fun create(): ValueTypeAdapterFactory = GsonValue_ValueTypeAdapterFactory()
    }
}