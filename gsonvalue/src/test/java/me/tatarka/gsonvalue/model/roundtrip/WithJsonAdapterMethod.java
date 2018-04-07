package me.tatarka.gsonvalue.model.roundtrip;

import me.tatarka.gsonvalue.annotations.GsonValue;
import me.tatarka.gsonvalue.annotations.JsonAdapter;
import me.tatarka.gsonvalue.model.adapters.StringToIntTypeAdapter;

@GsonValue
public class WithJsonAdapterMethod {
    private final int arg;

    public WithJsonAdapterMethod(int arg) {
        this.arg = arg;
    }

    @JsonAdapter(StringToIntTypeAdapter.class)
    public int arg() {
        return arg;
    }
}
