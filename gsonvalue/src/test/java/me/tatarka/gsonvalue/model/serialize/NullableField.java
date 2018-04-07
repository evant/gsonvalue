package me.tatarka.gsonvalue.model.serialize;

import me.tatarka.gsonvalue.annotations.GsonValue;

@GsonValue
public class NullableField {
    public PublicField arg;

    public NullableField(PublicField arg) {
        this.arg = arg;
    }
}
