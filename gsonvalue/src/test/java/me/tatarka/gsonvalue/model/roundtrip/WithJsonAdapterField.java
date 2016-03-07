package me.tatarka.gsonvalue.model.roundtrip;

import com.google.gson.annotations.JsonAdapter;
import me.tatarka.gsonvalue.annotations.GsonConstructor;
import me.tatarka.gsonvalue.model.adapters.StringToIntTypeAdapter;

public class WithJsonAdapterField {
    @JsonAdapter(StringToIntTypeAdapter.class)
    private final int arg;

    @GsonConstructor
    public WithJsonAdapterField(int arg) {
        this.arg = arg;
    }

    public int arg() {
        return arg;
    }
}
