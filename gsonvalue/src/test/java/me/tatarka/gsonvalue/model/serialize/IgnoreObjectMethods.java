package me.tatarka.gsonvalue.model.serialize;

import me.tatarka.gsonvalue.annotations.GsonValue;

@GsonValue
public class IgnoreObjectMethods {
    private final int arg;

    public IgnoreObjectMethods(int arg) {
        this.arg = arg;
    }

    public int getArg() {
        return arg;
    }

    @Override
    public String toString() {
        return "nope";
    }

    @Override
    public int hashCode() {
        return 10;
    }

    @Override
    protected Object clone() throws CloneNotSupportedException {
        throw new CloneNotSupportedException();
    }
}
