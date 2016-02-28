package me.tatarka.gsonvalue.model.deserialize;

import com.google.gson.annotations.SerializedName;
import me.tatarka.gsonvalue.annotations.GsonConstructor;

public abstract class AbstractNamedField {

    @GsonConstructor
    public static AbstractNamedField create(int arg) {
        return new Impl(arg);
    }

    @SerializedName("named")
    public abstract int arg();

    static class Impl extends AbstractNamedField {
        private final int arg;

        public Impl(int arg) {
            this.arg = arg;
        }

        @Override
        public int arg() {
            return arg;
        }
    }
}
