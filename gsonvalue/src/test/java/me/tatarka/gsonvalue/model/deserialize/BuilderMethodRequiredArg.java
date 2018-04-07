package me.tatarka.gsonvalue.model.deserialize;

import me.tatarka.gsonvalue.annotations.GsonValue;

@GsonValue
public class BuilderMethodRequiredArg {
    public transient boolean builderCalled;
    public int arg;

    public static Builder builder(int arg) {
        Builder builder = new Builder();
        builder.arg = arg;
        builder.builderCalled = true;
        return builder;
    }

    public static class Builder {
        private boolean builderCalled;
        private int arg;

        public BuilderMethodRequiredArg build() {
            BuilderMethodRequiredArg builderArg = new BuilderMethodRequiredArg();
            builderArg.builderCalled = builderCalled;
            builderArg.arg = arg;
            return builderArg;
        }
    }
}
