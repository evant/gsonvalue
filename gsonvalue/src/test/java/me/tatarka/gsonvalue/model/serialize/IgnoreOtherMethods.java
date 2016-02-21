package me.tatarka.gsonvalue.model.serialize;

import me.tatarka.gsonvalue.annotations.GsonConstructor;

public class IgnoreOtherMethods {
    private final int arg;

    @GsonConstructor
    public IgnoreOtherMethods(int arg) {
        this.arg = arg;
    }

    public int getArg() {
        return arg;
    }

    public String ignore() {
        return "nope";
    }
}
