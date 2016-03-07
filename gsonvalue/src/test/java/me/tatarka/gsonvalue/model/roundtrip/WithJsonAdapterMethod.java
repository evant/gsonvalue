package me.tatarka.gsonvalue.model.roundtrip;

import me.tatarka.gsonvalue.annotations.GsonConstructor;
import me.tatarka.gsonvalue.annotations.JsonAdapter;
import me.tatarka.gsonvalue.model.adapters.StringToIntTypeAdapter;

public class WithJsonAdapterMethod {
    private final int arg;

    @GsonConstructor
    public WithJsonAdapterMethod(int arg) {
        this.arg = arg;
    }

    @JsonAdapter(StringToIntTypeAdapter.class)
    public int arg() {
        return arg;
    }
}
