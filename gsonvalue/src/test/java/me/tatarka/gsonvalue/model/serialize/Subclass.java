package me.tatarka.gsonvalue.model.serialize;

import me.tatarka.gsonvalue.annotations.GsonConstructor;

public class Subclass {

    public static class Parent {
        public final int arg1;
        private final int arg2;


        public Parent(int arg1, int arg2) {
            this.arg1 = arg1;
            this.arg2 = arg2;
        }

        public int arg2() {
            return arg2;
        }
    }

    public static class Child extends Parent {
        public final int arg3;

        @GsonConstructor
        public Child(int arg1, int arg2, int arg3) {
            super(arg1, arg2);
            this.arg3 = arg3;
        }
    }
}
