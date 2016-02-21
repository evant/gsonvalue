package me.tatarka.gsonvalue.model.serialize;

import me.tatarka.gsonvalue.annotations.GsonConstructor;

public class Getter {
    private final int arg;

    @GsonConstructor
    public Getter(int arg) {
        this.arg = arg;
    }

    public int arg() {
        return arg;
    }
}
