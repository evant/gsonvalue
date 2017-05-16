package deserialize

import me.tatarka.gsonvalue.annotations.GsonConstructor

@GsonConstructor data class DataOneArgConstructor(val arg: Int)