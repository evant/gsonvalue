package me.tatarka.gsonvalue;

import com.squareup.javapoet.ClassName;

final class GsonClassNames {
    static final ClassName GSON = ClassName.get("com.google.gson", "Gson");
    static final ClassName TYPE_ADAPTER = ClassName.get("com.google.gson", "TypeAdapter");
    static final ClassName TYPE_ADAPTER_FACTORY = ClassName.get("com.google.gson", "TypeAdapterFactory");
    static final ClassName JSON_WRITER = ClassName.get("com.google.gson.stream", "JsonWriter");
    static final ClassName JSON_READER = ClassName.get("com.google.gson.stream", "JsonReader");
    static final ClassName JSON_TOKEN = ClassName.get("com.google.gson.stream", "JsonToken");
    static final ClassName TYPE_TOKEN = ClassName.get("com.google.gson.reflect", "TypeToken");
    static final ClassName JSON_ADAPTER = ClassName.get("com.google.gson.annotations", "JsonAdapter");
    static final ClassName JSON_ADAPTER_METHOD = ClassName.get("me.tatarka.gsonvalue.annotations", "JsonAdapter");
}
