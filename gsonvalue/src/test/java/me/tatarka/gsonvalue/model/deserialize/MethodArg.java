package me.tatarka.gsonvalue.model.deserialize;

import me.tatarka.gsonvalue.annotations.GsonConstructor;

public class MethodArg {
    public transient boolean factoryMethodCalled;
    public int arg;

    @GsonConstructor
    public static MethodArg create(int arg) {
        MethodArg methodArg = new MethodArg();
        methodArg.factoryMethodCalled = true;
        methodArg.arg = arg;
        return methodArg;
    }
}
