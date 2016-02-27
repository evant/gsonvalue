package me.tatarka.gsonvalue.model.deserialize;

import me.tatarka.gsonvalue.annotations.GsonConstructor;

import java.util.List;

public class ComplexGenericConstructorArg<T> {
    public transient boolean constructorCalled;
    public List<T> arg;

    @GsonConstructor
    public ComplexGenericConstructorArg(List<T> arg) {
        constructorCalled = true;
        this.arg = arg;
    }
}
