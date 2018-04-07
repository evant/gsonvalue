package me.tatarka.gsonvalue.model.deserialize;

import me.tatarka.gsonvalue.annotations.GsonValue;

import java.util.List;

@GsonValue
public class ComplexGenericConstructorArg<T> {
    public transient boolean constructorCalled;
    public List<T> arg;

    public ComplexGenericConstructorArg(List<T> arg) {
        constructorCalled = true;
        this.arg = arg;
    }
}
