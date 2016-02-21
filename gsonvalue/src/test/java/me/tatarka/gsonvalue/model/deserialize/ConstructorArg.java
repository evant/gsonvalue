package me.tatarka.gsonvalue.model.deserialize;

import me.tatarka.gsonvalue.annotations.GsonConstructor;

public class ConstructorArg {
    public transient boolean constructorCalled;
    public int arg;

    @GsonConstructor
    public ConstructorArg(int arg) {
        constructorCalled = true;
        this.arg = arg;
    }
}
