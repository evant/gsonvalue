package me.tatarka.gsonvalue.model.deserialize;

import me.tatarka.gsonvalue.annotations.GsonValue;

@GsonValue
public class BuilderConstructorArg {
    public transient boolean builderCalled;
    public int arg;

    public static class Builder {
        private boolean builderCalled;
        private int arg;

        public Builder() {
            builderCalled = true;
        }

        public Builder arg(int arg) {
            this.arg = arg;
            return this;
        }

        public BuilderConstructorArg build() {
            BuilderConstructorArg builderArg = new BuilderConstructorArg();
            builderArg.builderCalled = builderCalled;
            builderArg.arg = arg;
            return builderArg;
        }
    }
}
