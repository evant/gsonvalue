import me.tatarka.gsonvalue.annotations.GsonConstructor

class OneArgConstructor @GsonConstructor constructor(val arg: Int) {
    @Transient
    val calledConstructor = true
}
