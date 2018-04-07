package me.tatarka.gsonvalue.model.deserialize;

import com.google.gson.annotations.SerializedName;
import me.tatarka.gsonvalue.annotations.GsonValue;

@GsonValue
public class NamedField {
    @SerializedName("named")
    public final int arg;

    public NamedField(int arg) {
        this.arg = arg;
    }
}

