package deserialize

import me.tatarka.gsonvalue.annotations.GsonConstructor

@GsonConstructor class OneArgConstructor(val arg: Int) {
    @Transient
    val calledConstructor = true
}
