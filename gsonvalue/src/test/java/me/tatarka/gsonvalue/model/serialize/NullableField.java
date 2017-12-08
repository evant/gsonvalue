package me.tatarka.gsonvalue.model.serialize;

import me.tatarka.gsonvalue.annotations.GsonConstructor;

public class NullableField {
    public PublicField arg;

    @GsonConstructor
    public NullableField(PublicField arg) {
        this.arg = arg;
    }
}
