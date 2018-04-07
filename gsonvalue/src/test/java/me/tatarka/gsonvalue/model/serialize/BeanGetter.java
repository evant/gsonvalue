package me.tatarka.gsonvalue.model.serialize;

import me.tatarka.gsonvalue.annotations.GsonValue;

@GsonValue
public class BeanGetter {
    private final int arg;

    public BeanGetter(int arg) {
        this.arg = arg;
    }

    public int getArg() {
        return arg;
    }
}
