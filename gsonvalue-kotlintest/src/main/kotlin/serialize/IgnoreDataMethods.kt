package serialize

import me.tatarka.gsonvalue.annotations.GsonValue

@GsonValue
data class IgnoreDataMethods constructor(val arg1: Int, val arg2: Int)
