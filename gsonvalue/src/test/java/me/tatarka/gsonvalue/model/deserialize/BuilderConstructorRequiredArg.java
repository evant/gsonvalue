package me.tatarka.gsonvalue.model.deserialize;

import me.tatarka.gsonvalue.annotations.GsonBuilder;

public class BuilderConstructorRequiredArg {
    public transient boolean builderCalled;
    public int arg;

    public static class Builder {
        private boolean builderCalled;
        private int arg;

        @GsonBuilder
        public Builder(int arg) {
            builderCalled = true;
            this.arg = arg;
        }

        public BuilderConstructorRequiredArg build() {
            BuilderConstructorRequiredArg builderArg = new BuilderConstructorRequiredArg();
            builderArg.builderCalled = builderCalled;
            builderArg.arg = arg;
            return builderArg;
        }
    }
}
