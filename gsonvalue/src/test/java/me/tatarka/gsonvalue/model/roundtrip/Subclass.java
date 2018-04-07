package me.tatarka.gsonvalue.model.roundtrip;

import me.tatarka.gsonvalue.annotations.GsonConstructor;

public class Subclass {

    public static class Parent {
        private final String foo;

        public Parent(String foo) {
            this.foo = foo;
        }

        public String foo() {
            return foo;
        }
    }

    public static class Child extends Parent {
        private final String bar;

        @GsonConstructor
        public Child(String foo, String bar) {
            super(foo);
            this.bar = bar;
        }

        @Override
        public String foo() {
            return super.foo();
        }

        public String bar() {
            return bar;
        }
    }
}
