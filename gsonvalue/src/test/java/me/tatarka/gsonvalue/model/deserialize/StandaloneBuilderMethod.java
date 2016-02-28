package me.tatarka.gsonvalue.model.deserialize;

import me.tatarka.gsonvalue.annotations.GsonBuilder;

public class StandaloneBuilderMethod {

    @GsonBuilder
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private int arg;

        public Builder arg(int arg) {
            this.arg = arg;
            return this;
        }

        public Class build() {
            return new Class(arg);
        }
    }

    public static class Class {
        private final int arg;

        public Class(int arg) {
            this.arg = arg;
        }

        public int arg() {
            return arg;
        }
    }
}
