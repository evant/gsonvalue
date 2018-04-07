package me.tatarka.gsonvalue.model.roundtrip;

import com.google.gson.annotations.JsonAdapter;

import me.tatarka.gsonvalue.annotations.GsonValue;
import me.tatarka.gsonvalue.model.adapters.StringToIntTypeAdapter;

@GsonValue
public class WithJsonAdapterField {
    @JsonAdapter(StringToIntTypeAdapter.class)
    private final int arg;

    public WithJsonAdapterField(int arg) {
        this.arg = arg;
    }

    public int arg() {
        return arg;
    }
}
