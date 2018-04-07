package me.tatarka.gsonvalue.model.serialize;

import me.tatarka.gsonvalue.annotations.GsonValue;

@GsonValue
public class PublicField {
    public final int arg;

    public PublicField(int arg) {
        this.arg = arg;
    }
}
