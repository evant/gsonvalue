package me.tatarka.gsonvalue.model.deserialize;

import me.tatarka.gsonvalue.annotations.GsonConstructor;

public class StandaloneMethod {

    @GsonConstructor
    public static Class create(int arg) {
        return new Class(arg);
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
