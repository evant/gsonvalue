package me.tatarka.gsonvalue.model.deserialize;

import com.google.gson.annotations.SerializedName;
import me.tatarka.gsonvalue.annotations.GsonBuilder;

public class BuilderNamedField {
    private final int arg;

    private BuilderNamedField(int arg) {
        this.arg = arg;
    }

    @SerializedName("named")
    public int arg() {
        return arg;
    }

    public static class Builder {
        private int arg;

        @GsonBuilder
        public Builder() {
        }

        public Builder arg(int arg) {
            this.arg = arg;
            return this;
        }

        public BuilderNamedField build() {
            return new BuilderNamedField(arg);
        }
    }
}
