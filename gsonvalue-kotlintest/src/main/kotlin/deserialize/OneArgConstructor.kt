package deserialize

import me.tatarka.gsonvalue.annotations.GsonValue

@GsonValue class OneArgConstructor(val arg: Int) {
    @Transient
    val calledConstructor = true
}
