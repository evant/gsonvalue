package me.tatarka.gsonvalue.model.serialize;

import me.tatarka.gsonvalue.annotations.GsonConstructor;

public class BeanGetter {
    private final int arg;

    @GsonConstructor
    public BeanGetter(int arg) {
        this.arg = arg;
    }

    public int getArg() {
        return arg;
    }
}
