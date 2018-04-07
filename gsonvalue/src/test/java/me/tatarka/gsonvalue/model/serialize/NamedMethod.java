package me.tatarka.gsonvalue.model.serialize;

import com.google.gson.annotations.SerializedName;
import me.tatarka.gsonvalue.annotations.GsonValue;

@GsonValue
public class NamedMethod {
    private final int arg;

    public NamedMethod(int arg) {
        this.arg = arg;
    }

    @SerializedName("named")
    public int arg() {
        return arg;
    }
}
