package me.tatarka.gsonvalue.model.deserialize;

import me.tatarka.gsonvalue.annotations.GsonConstructor;

import java.util.List;

public class ComplexArg {
    public final List<String> args;

    @GsonConstructor
    public ComplexArg(List<String> args) {
        this.args = args;
    }
}
