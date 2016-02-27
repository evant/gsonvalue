package me.tatarka.gsonvalue.model.deserialize;

import me.tatarka.gsonvalue.annotations.GsonBuilder;

public class BuilderMethodArg {
    public transient boolean builderCalled;
    public int arg;

    @GsonBuilder
    public static Builder builder() {
        Builder builder = new Builder();
        builder.builderCalled = true;
        return builder;
    }

    public static class Builder {
        private boolean builderCalled;
        private int arg;

        public Builder arg(int arg) {
            this.arg = arg;
            return this;
        }

        public BuilderMethodArg build() {
            BuilderMethodArg builderArg = new BuilderMethodArg();
            builderArg.builderCalled = builderCalled;
            builderArg.arg = arg;
            return builderArg;
        }
    }
}
