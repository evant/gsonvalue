package me.tatarka.gsonvalue.model.serialize;

import com.google.gson.annotations.SerializedName;
import me.tatarka.gsonvalue.annotations.GsonConstructor;

public class NamedMethod {
    private final int arg;

    @GsonConstructor
    public NamedMethod(int arg) {
        this.arg = arg;
    }

    @SerializedName("named")
    public int arg() {
        return arg;
    }
}
