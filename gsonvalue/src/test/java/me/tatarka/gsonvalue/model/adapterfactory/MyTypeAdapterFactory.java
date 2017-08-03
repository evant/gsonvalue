package me.tatarka.gsonvalue.model.adapterfactory;

import com.google.gson.TypeAdapterFactory;
import me.tatarka.gsonvalue.annotations.GsonValueTypeAdapterFactory;

@GsonValueTypeAdapterFactory
public abstract class MyTypeAdapterFactory implements TypeAdapterFactory {

    public static MyTypeAdapterFactory create() {
        return new GsonValue_MyTypeAdapterFactory();
    }
}
