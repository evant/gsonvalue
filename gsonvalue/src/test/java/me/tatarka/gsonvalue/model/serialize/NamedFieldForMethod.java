package me.tatarka.gsonvalue.model.serialize;

import com.google.gson.annotations.SerializedName;
import me.tatarka.gsonvalue.annotations.GsonConstructor;

public class NamedFieldForMethod {
    @SerializedName("named")
    private final int arg;

    @GsonConstructor
    public NamedFieldForMethod(int arg) {
        this.arg = arg;
    }

    public int arg() {
        return arg;
    }
}
