package me.tatarka.gsonvalue.model.deserialize;

import me.tatarka.gsonvalue.annotations.GsonValue;

@GsonValue
public class MethodArg {
    public transient boolean factoryMethodCalled;
    public int arg;

    public static MethodArg create(int arg) {
        MethodArg methodArg = new MethodArg();
        methodArg.factoryMethodCalled = true;
        methodArg.arg = arg;
        return methodArg;
    }
}
