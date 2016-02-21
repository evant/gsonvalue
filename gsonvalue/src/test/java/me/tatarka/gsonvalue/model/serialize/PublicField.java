package me.tatarka.gsonvalue.model.serialize;

import me.tatarka.gsonvalue.annotations.GsonConstructor;

public class PublicField {
    public final int arg;

    @GsonConstructor
    public PublicField(int arg) {
        this.arg = arg;
    }
}
