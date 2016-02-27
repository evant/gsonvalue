package me.tatarka.gsonvalue.model.deserialize;

import me.tatarka.gsonvalue.annotations.GsonConstructor;

public class GenericConstructorArg<T> {
    public transient boolean constructorCalled;
    public T arg;

    @GsonConstructor
    public GenericConstructorArg(T arg) {
        constructorCalled = true;
        this.arg = arg;
    }
}
