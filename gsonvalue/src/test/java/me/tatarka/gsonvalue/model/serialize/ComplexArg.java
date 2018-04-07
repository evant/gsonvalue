package me.tatarka.gsonvalue.model.serialize;

import me.tatarka.gsonvalue.annotations.GsonValue;

import java.util.List;

@GsonValue
public class ComplexArg {
    final List<String> args;

    public ComplexArg(List<String> args) {
        this.args = args;
    }
}
