package me.tatarka.gsonvalue.model.deserialize;

import com.google.gson.annotations.SerializedName;
import me.tatarka.gsonvalue.annotations.GsonConstructor;

public class NamedField {
    @SerializedName("named")
    public final int arg;

    @GsonConstructor
    public NamedField(int arg) {
        this.arg = arg;
    }
}

