package me.tatarka.gsonvalue.model.deserialize;

import me.tatarka.gsonvalue.annotations.GsonConstructor;

public class GenericMethodArg<T> {
    public transient boolean factoryMethodCalled;
    public T arg;

    @GsonConstructor
    public static <T> GenericMethodArg create(T arg) {
        GenericMethodArg<T> methodArg = new GenericMethodArg<>();
        methodArg.factoryMethodCalled = true;
        methodArg.arg = arg;
        return methodArg;
    }
}
