package me.tatarka.gsonvalue.model.serialize;

import me.tatarka.gsonvalue.annotations.GsonConstructor;

import java.util.List;

public class ComplexArg {
    final List<String> args;

    @GsonConstructor
    public ComplexArg(List<String> args) {
        this.args = args;
    }
}
