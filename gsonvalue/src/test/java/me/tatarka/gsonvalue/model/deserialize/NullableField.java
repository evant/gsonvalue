package me.tatarka.gsonvalue.model.deserialize;

import me.tatarka.gsonvalue.annotations.GsonConstructor;

public class NullableField {
    public ConstructorArg arg;

    @GsonConstructor
    public NullableField(ConstructorArg arg) {
        this.arg = arg;
    }
}
