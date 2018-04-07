package me.tatarka.gsonvalue.model.serialize;

import com.google.gson.annotations.SerializedName;
import me.tatarka.gsonvalue.annotations.GsonValue;

@GsonValue
public class NamedFieldForMethod {
    @SerializedName("named")
    private final int arg;

    public NamedFieldForMethod(int arg) {
        this.arg = arg;
    }

    public int arg() {
        return arg;
    }
}
