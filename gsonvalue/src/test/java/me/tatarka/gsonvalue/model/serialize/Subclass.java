package me.tatarka.gsonvalue.model.serialize;

import me.tatarka.gsonvalue.annotations.GsonConstructor;

public class Subclass {

    public static class Parent {
        public final int arg2;
        private final int arg3;


        public Parent(int arg2, int arg3) {
            this.arg2 = arg2;
            this.arg3 = arg3;
        }

        public int arg3() {
            return arg3;
        }
    }

    public static class Child extends Parent {
        public final int arg1;

        @GsonConstructor
        public Child(int arg1, int arg2, int arg3) {
            super(arg2, arg3);
            this.arg1 = arg1;
        }
    }
}
