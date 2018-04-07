package me.tatarka.gsonvalue.model.deserialize;

import me.tatarka.gsonvalue.annotations.GsonValue;

@GsonValue
public class GenericConstructorArg<T> {
    public transient boolean constructorCalled;
    public T arg;

    public GenericConstructorArg(T arg) {
        constructorCalled = true;
        this.arg = arg;
    }
}
