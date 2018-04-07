package me.tatarka.gsonvalue.model.roundtrip;

import me.tatarka.gsonvalue.annotations.GsonValue;

@GsonValue
public class PublicField {
    public final int arg;

    public PublicField(int arg) {
        this.arg = arg;
    }
}
