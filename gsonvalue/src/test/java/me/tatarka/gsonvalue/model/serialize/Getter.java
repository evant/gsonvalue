package me.tatarka.gsonvalue.model.serialize;

import me.tatarka.gsonvalue.annotations.GsonValue;

@GsonValue
public class Getter {
    private final int arg;

    public Getter(int arg) {
        this.arg = arg;
    }

    public int arg() {
        return arg;
    }
}
