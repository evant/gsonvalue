package me.tatarka.gsonvalue.model.deserialize;

import me.tatarka.gsonvalue.annotations.GsonValue;

@GsonValue
public class ConstructorArg {
    public transient boolean constructorCalled;
    public int arg;

    public ConstructorArg(int arg) {
        constructorCalled = true;
        this.arg = arg;
    }
}
