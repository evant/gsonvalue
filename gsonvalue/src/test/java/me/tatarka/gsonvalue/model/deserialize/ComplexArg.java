package me.tatarka.gsonvalue.model.deserialize;

import me.tatarka.gsonvalue.annotations.GsonValue;

import java.util.List;

@GsonValue
public class ComplexArg {
    public final List<String> args;

    public ComplexArg(List<String> args) {
        this.args = args;
    }
}
