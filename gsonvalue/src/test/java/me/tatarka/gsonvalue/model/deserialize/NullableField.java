package me.tatarka.gsonvalue.model.deserialize;

import me.tatarka.gsonvalue.annotations.GsonValue;

@GsonValue
public class NullableField {
    public ConstructorArg arg;

    public NullableField(ConstructorArg arg) {
        this.arg = arg;
    }
}
