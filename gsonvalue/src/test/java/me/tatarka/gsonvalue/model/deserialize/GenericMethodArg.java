package me.tatarka.gsonvalue.model.deserialize;

import me.tatarka.gsonvalue.annotations.GsonValue;

@GsonValue
public class GenericMethodArg<T> {
    public transient boolean factoryMethodCalled;
    public T arg;

    public static <T> GenericMethodArg create(T arg) {
        GenericMethodArg<T> methodArg = new GenericMethodArg<>();
        methodArg.factoryMethodCalled = true;
        methodArg.arg = arg;
        return methodArg;
    }
}
