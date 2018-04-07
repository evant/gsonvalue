package me.tatarka.gsonvalue.processor;

import com.google.testing.compile.JavaFileObjects;
import me.tatarka.gsonvalue.GsonValueProcessor;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import static com.google.common.truth.Truth.assertAbout;
import static com.google.testing.compile.JavaSourceSubjectFactory.javaSource;

@RunWith(JUnit4.class)
public class GsonValueProcessorTest {
    private static final String VALUE_TYPE_ADAPTER_IMPORTS =
            "import com.google.gson.Gson;\n" +
            "import com.google.gson.TypeAdapter;\n" +
            "import com.google.gson.reflect.TypeToken;\n" +
            "import com.google.gson.stream.JsonReader;\n" +
            "import com.google.gson.stream.JsonToken;\n" +
            "import com.google.gson.stream.JsonWriter;\n" +
            "import java.io.IOException;\n";

    private static final String READ_NULL_CHECK =
            "        if (in.peek() == JsonToken.NULL) {\n" +
            "            in.nextNull();\n" +
            "            return null;\n" +
            "        }\n";

    private static final String WRITE_NULL_CHECK =
            "        if (value == null) {\n" +
            "          out.nullValue();\n" +
            "          return;\n" +
            "        }\n";

    @Test
    public void emptyConstructor() {
        assertAbout(javaSource()).that(JavaFileObjects.forSourceString("test.Test",
                "package test;\n" +
                        "\n" +
                        "import me.tatarka.gsonvalue.annotations.GsonValue;\n" +
                        "\n" +
                        "@GsonValue\n" +
                        "public class Test {\n" +
                        "    public Test() {\n" +
                        "    }\n" +
                        "}"))
                .processedWith(new GsonValueProcessor())
                .compilesWithoutError()
                .and().generatesSources(JavaFileObjects.forSourceString("test.ValueTypeAdapter_Test",
                "package test;\n" +
                        "\n" +
                        VALUE_TYPE_ADAPTER_IMPORTS +
                        "\n" +
                        "public class ValueTypeAdapter_Test extends TypeAdapter<Test> {\n" +
                        "    public ValueTypeAdapter_Test(Gson gson, TypeToken<Test> typeToken) {\n" +
                        "    }\n" +
                        "    @Override\n" +
                        "    public void write(JsonWriter out, Test value) throws IOException {\n" +
                        WRITE_NULL_CHECK +
                        "        out.beginObject();\n" +
                        "        out.endObject();\n" +
                        "    }\n" +
                        "\n" +
                        "    @Override\n" +
                        "    public Test read(JsonReader in) throws IOException {\n" +
                        READ_NULL_CHECK +
                        "        in.skipValue();\n" +
                        "        return new Test();\n" +
                        "    }\n" +
                        "}"));
    }

    @Test
    public void oneArgConstructor() {
        assertAbout(javaSource()).that(JavaFileObjects.forSourceString("test.Test",
                "package test;\n" +
                        "\n" +
                        "import me.tatarka.gsonvalue.annotations.GsonValue;\n" +
                        "\n" +
                        "@GsonValue\n" +
                        "public class Test {\n" +
                        "    private final int arg;\n" +
                        "    public Test(int arg) {\n" +
                        "        this.arg = arg;\n" +
                        "    }\n" +
                        "    \n" +
                        "    public int arg() {\n" +
                        "        return arg;\n" +
                        "    }\n" +
                        "}"))
                .processedWith(new GsonValueProcessor())
                .compilesWithoutError()
                .and().generatesSources(JavaFileObjects.forSourceString("test.ValueTypeAdapter_Test",
                "package test;\n" +
                        "\n" +
                        VALUE_TYPE_ADAPTER_IMPORTS +
                        "\n" +
                        "public class ValueTypeAdapter_Test extends TypeAdapter<Test> {\n" +
                        "    private final TypeAdapter<Integer> adapter_arg;\n" +
                        "    public ValueTypeAdapter_Test(Gson gson, TypeToken<Test> typeToken) {\n" +
                        "        this.adapter_arg = gson.getAdapter(int.class);\n" +
                        "    }\n" +
                        "    @Override\n" +
                        "    public void write(JsonWriter out, Test value) throws IOException {\n" +
                        WRITE_NULL_CHECK +
                        "        out.beginObject();\n" +
                        "        out.name(\"arg\");\n" +
                        "        adapter_arg.write(out, value.arg());\n" +
                        "        out.endObject();\n" +
                        "    }\n" +
                        "\n" +
                        "    @Override\n" +
                        "    public Test read(JsonReader in) throws IOException {\n" +
                        READ_NULL_CHECK +
                        "        int _arg = 0;\n" +
                        "        in.beginObject();\n" +
                        "        while (in.hasNext()) {\n" +
                        "            switch (in.nextName()) {\n" +
                        "                case \"arg\":\n" +
                        "                    _arg = adapter_arg.read(in);\n" +
                        "                    break;\n" +
                        "                default:\n" +
                        "                    in.skipValue();\n" +
                        "            }\n" +
                        "        }\n" +
                        "        in.endObject();\n" +
                        "        return new Test(_arg);\n" +
                        "    }\n" +
                        "}"));
    }

    @Test
    public void oneArgMethod() {
        assertAbout(javaSource()).that(JavaFileObjects.forSourceString("test.Test",
                "package test;\n" +
                        "\n" +
                        "import me.tatarka.gsonvalue.annotations.GsonValue;\n" +
                        "\n" +
                        "@GsonValue\n" +
                        "public class Test {\n" +
                        "    private int arg;\n" +
                        "    public static Test create(int arg) {\n" +
                        "        Test test = new Test();\n" +
                        "        test.arg = arg;\n" +
                        "        return test;\n" +
                        "    }\n" +
                        "    public int arg() {\n" +
                        "        return arg;\n" +
                        "    }\n" +
                        "}"))
                .processedWith(new GsonValueProcessor())
                .compilesWithoutError()
                .and().generatesSources(JavaFileObjects.forSourceString("test.ValueTypeAdapter_Test",
                "package test;\n" +
                        "\n" +
                        VALUE_TYPE_ADAPTER_IMPORTS +
                        "\n" +
                        "public class ValueTypeAdapter_Test extends TypeAdapter<Test> {\n" +
                        "    private final TypeAdapter<Integer> adapter_arg;\n" +
                        "    public ValueTypeAdapter_Test(Gson gson, TypeToken<Test> typeToken) {\n" +
                        "        this.adapter_arg = gson.getAdapter(int.class);\n" +
                        "    }\n" +
                        "    @Override\n" +
                        "    public void write(JsonWriter out, Test value) throws IOException {\n" +
                        WRITE_NULL_CHECK +
                        "        out.beginObject();\n" +
                        "        out.name(\"arg\");\n" +
                        "        adapter_arg.write(out, value.arg());\n" +
                        "        out.endObject();\n" +
                        "    }\n" +
                        "\n" +
                        "    @Override\n" +
                        "    public Test read(JsonReader in) throws IOException {\n" +
                        READ_NULL_CHECK +
                        "        int _arg = 0;\n" +
                        "        in.beginObject();\n" +
                        "        while (in.hasNext()) {\n" +
                        "            switch (in.nextName()) {\n" +
                        "                case \"arg\":\n" +
                        "                    _arg = adapter_arg.read(in);\n" +
                        "                    break;\n" +
                        "                default:\n" +
                        "                    in.skipValue();\n" +
                        "            }\n" +
                        "        }\n" +
                        "        in.endObject();\n" +
                        "        return Test.create(_arg);\n" +
                        "    }\n" +
                        "}"));
    }

    @Test
    public void oneArgBuilderConstructor() {
        assertAbout(javaSource()).that(JavaFileObjects.forSourceString("test.Test",
                "package test;\n" +
                        "\n" +
                        "import me.tatarka.gsonvalue.annotations.GsonValue;\n" +
                        "\n" +
                        "@GsonValue\n" +
                        "public class Test {\n" +
                        "    private int arg;\n" +
                        "    public int arg() {\n" +
                        "        return arg;\n" +
                        "    }\n" +
                        "    \n" +
                        "    public static class Builder {\n" +
                        "        private int arg;\n" +
                        "        public Builder() {\n" +
                        "        }\n" +
                        "        \n" +
                        "        public Builder arg(int arg) {\n" +
                        "            this.arg = arg;\n" +
                        "            return this;\n" +
                        "        }\n" +
                        "        \n" +
                        "        public Test build() {\n" +
                        "            Test test = new Test();\n" +
                        "            test.arg = arg;\n" +
                        "            return test;\n" +
                        "        }\n" +
                        "    }\n" +
                        "}"))
                .processedWith(new GsonValueProcessor())
                .compilesWithoutError()
                .and().generatesSources(JavaFileObjects.forSourceString("test.ValueTypeAdapter_Test",
                "package test;\n" +
                        "\n" +
                        VALUE_TYPE_ADAPTER_IMPORTS +
                        "\n" +
                        "public class ValueTypeAdapter_Test extends TypeAdapter<Test> {\n" +
                        "    private final TypeAdapter<Integer> adapter_arg;\n" +
                        "    public ValueTypeAdapter_Test(Gson gson, TypeToken<Test> typeToken) {\n" +
                        "        this.adapter_arg = gson.getAdapter(int.class);\n" +
                        "    }\n" +
                        "    @Override\n" +
                        "    public void write(JsonWriter out, Test value) throws IOException {\n" +
                        WRITE_NULL_CHECK +
                        "        out.beginObject();\n" +
                        "        out.name(\"arg\");\n" +
                        "        adapter_arg.write(out, value.arg());\n" +
                        "        out.endObject();\n" +
                        "    }\n" +
                        "\n" +
                        "    @Override\n" +
                        "    public Test read(JsonReader in) throws IOException {\n" +
                        READ_NULL_CHECK +
                        "        int _arg = 0;\n" +
                        "        in.beginObject();\n" +
                        "        while (in.hasNext()) {\n" +
                        "            switch (in.nextName()) {\n" +
                        "                case \"arg\":\n" +
                        "                    _arg = adapter_arg.read(in);\n" +
                        "                    break;\n" +
                        "                default:\n" +
                        "                    in.skipValue();\n" +
                        "            }\n" +
                        "        }\n" +
                        "        in.endObject();\n" +
                        "        return new Test.Builder()\n" +
                        "                .arg(_arg)\n" +
                        "                .build();\n" +
                        "    }\n" +
                        "}"));
    }

    @Test
    public void oneArgBuilderMethod() {
        assertAbout(javaSource()).that(JavaFileObjects.forSourceString("test.Test",
                "package test;\n" +
                        "\n" +
                        "import me.tatarka.gsonvalue.annotations.GsonValue;\n" +
                        "\n" +
                        "@GsonValue\n" +
                        "public class Test {\n" +
                        "    private int arg;\n" +
                        "    public int arg() {\n" +
                        "        return arg;\n" +
                        "    }\n" +
                        "    public static Builder builder() {\n" +
                        "        return new Builder();\n" +
                        "    }\n" +
                        "    \n" +
                        "    public static class Builder {\n" +
                        "        private int arg;\n" +
                        "        \n" +
                        "        public Builder arg(int arg) {\n" +
                        "            this.arg = arg;\n" +
                        "            return this;\n" +
                        "        }\n" +
                        "        \n" +
                        "        public Test build() {\n" +
                        "            Test test = new Test();\n" +
                        "            test.arg = arg;\n" +
                        "            return test;\n" +
                        "        }\n" +
                        "    }\n" +
                        "}"))
                .processedWith(new GsonValueProcessor())
                .compilesWithoutError()
                .and().generatesSources(JavaFileObjects.forSourceString("test.ValueTypeAdapter_Test",
                "package test;\n" +
                        "\n" +
                        VALUE_TYPE_ADAPTER_IMPORTS +
                        "\n" +
                        "public class ValueTypeAdapter_Test extends TypeAdapter<Test> {\n" +
                        "    private final TypeAdapter<Integer> adapter_arg;\n" +
                        "    public ValueTypeAdapter_Test(Gson gson, TypeToken<Test> typeToken) {\n" +
                        "        this.adapter_arg = gson.getAdapter(int.class);\n" +
                        "    }\n" +
                        "    @Override\n" +
                        "    public void write(JsonWriter out, Test value) throws IOException {\n" +
                        WRITE_NULL_CHECK +
                        "        out.beginObject();\n" +
                        "        out.name(\"arg\");\n" +
                        "        adapter_arg.write(out, value.arg());\n" +
                        "        out.endObject();\n" +
                        "    }\n" +
                        "\n" +
                        "    @Override\n" +
                        "    public Test read(JsonReader in) throws IOException {\n" +
                        READ_NULL_CHECK +
                        "        int _arg = 0;\n" +
                        "        in.beginObject();\n" +
                        "        while (in.hasNext()) {\n" +
                        "            switch (in.nextName()) {\n" +
                        "                case \"arg\":\n" +
                        "                    _arg = adapter_arg.read(in);\n" +
                        "                    break;\n" +
                        "                default:\n" +
                        "                    in.skipValue();\n" +
                        "            }\n" +
                        "        }\n" +
                        "        in.endObject();\n" +
                        "        return Test.builder()\n" +
                        "                .arg(_arg)\n" +
                        "                .build();\n" +
                        "    }\n" +
                        "}"));
    }

    public void oneRequiredArgBuilderConstructor() {
        assertAbout(javaSource()).that(JavaFileObjects.forSourceString("test.Test",
                "package test;\n" +
                        "\n" +
                        "import me.tatarka.gsonvalue.annotations.GsonBuilder;\n" +
                        "\n" +
                        "@GsonValue\n" +
                        "public class Test {\n" +
                        "    private int arg;\n" +
                        "    public int arg() {\n" +
                        "        return arg;\n" +
                        "    }\n" +
                        "    \n" +
                        "    public static class Builder {\n" +
                        "        private int arg;\n" +
                        "        public Builder(int arg) {\n" +
                        "            this.arg = arg;\n" +
                        "        }\n" +
                        "        \n" +
                        "        public Test build() {\n" +
                        "            Test test = new Test();\n" +
                        "            test.arg = arg;\n" +
                        "            return test;\n" +
                        "        }\n" +
                        "    }\n" +
                        "}"))
                .processedWith(new GsonValueProcessor())
                .compilesWithoutError()
                .and().generatesSources(JavaFileObjects.forSourceString("test.ValueTypeAdapter_Test",
                "package test;\n" +
                        "\n" +
                        VALUE_TYPE_ADAPTER_IMPORTS +
                        "\n" +
                        "public class ValueTypeAdapter_Test extends TypeAdapter<Test> {\n" +
                        "    private final TypeAdapter<Integer> adapter_arg;\n" +
                        "    public ValueTypeAdapter_Test(Gson gson, TypeToken<Test> typeToken) {\n" +
                        "        this.adapter_arg = gson.getAdapter(int.class);\n" +
                        "    }\n" +
                        "    @Override\n" +
                        "    public void write(JsonWriter out, Test value) throws IOException {\n" +
                        WRITE_NULL_CHECK +
                        "        out.beginObject();\n" +
                        "        out.name(\"arg\");\n" +
                        "        adapter_arg.write(out, value.arg());\n" +
                        "        out.endObject();\n" +
                        "    }\n" +
                        "\n" +
                        "    @Override\n" +
                        "    public Test read(JsonReader in) throws IOException {\n" +
                        READ_NULL_CHECK +
                        "        int _arg = 0;\n" +
                        "        in.beginObject();\n" +
                        "        while (in.hasNext()) {\n" +
                        "            switch (in.nextName()) {\n" +
                        "                case \"arg\":\n" +
                        "                    _arg = adapter_arg.read(in);\n" +
                        "                    break;\n" +
                        "                default:\n" +
                        "                    in.skipValue();\n" +
                        "            }\n" +
                        "        }\n" +
                        "        in.endObject();\n" +
                        "        return new Test.Builder(_arg)\n" +
                        "                .build();\n" +
                        "    }\n" +
                        "}"));
    }

    @Test
    public void oneRequiredArgBuilderMethod() {
        assertAbout(javaSource()).that(JavaFileObjects.forSourceString("test.Test",
                "package test;\n" +
                        "\n" +
                        "import com.google.gson.annotations.SerializedName;\n" +
                        "import me.tatarka.gsonvalue.annotations.GsonValue;\n" +
                        "\n" +
                        "@GsonValue\n" +
                        "public class Test {\n" +
                        "    @SerializedName(\"named\")\n" +
                        "    private int arg;\n" +
                        "    public int arg() {\n" +
                        "        return arg;\n" +
                        "    }\n" +
                        "    public static Builder builder(int arg) {\n" +
                        "        return new Builder()\n" +
                        "                .arg(arg);\n" +
                        "    }\n" +
                        "    \n" +
                        "    public static class Builder {\n" +
                        "        private int arg;\n" +
                        "        \n" +
                        "        public Builder arg(int arg) {\n" +
                        "            this.arg = arg;\n" +
                        "            return this;\n" +
                        "        }\n" +
                        "        \n" +
                        "        public Test build() {\n" +
                        "            Test test = new Test();\n" +
                        "            test.arg = arg;\n" +
                        "            return test;\n" +
                        "        }\n" +
                        "    }\n" +
                        "}"))
                .processedWith(new GsonValueProcessor())
                .compilesWithoutError()
                .and().generatesSources(JavaFileObjects.forSourceString("test.ValueTypeAdapter_Test",
                "package test;\n" +
                        "\n" +
                        VALUE_TYPE_ADAPTER_IMPORTS +
                        "\n" +
                        "public class ValueTypeAdapter_Test extends TypeAdapter<Test> {\n" +
                        "    private final TypeAdapter<Integer> adapter_arg;\n" +
                        "    public ValueTypeAdapter_Test(Gson gson, TypeToken<Test> typeToken) {\n" +
                        "        this.adapter_arg = gson.getAdapter(int.class);\n" +
                        "    }\n" +
                        "    @Override\n" +
                        "    public void write(JsonWriter out, Test value) throws IOException {\n" +
                        WRITE_NULL_CHECK +
                        "        out.beginObject();\n" +
                        "        out.name(\"named\");\n" +
                        "        adapter_arg.write(out, value.arg());\n" +
                        "        out.endObject();\n" +
                        "    }\n" +
                        "\n" +
                        "    @Override\n" +
                        "    public Test read(JsonReader in) throws IOException {\n" +
                        READ_NULL_CHECK +
                        "        int _arg = 0;\n" +
                        "        in.beginObject();\n" +
                        "        while (in.hasNext()) {\n" +
                        "            switch (in.nextName()) {\n" +
                        "                case \"named\":\n" +
                        "                    _arg = adapter_arg.read(in);\n" +
                        "                    break;\n" +
                        "                default:\n" +
                        "                    in.skipValue();\n" +
                        "            }\n" +
                        "        }\n" +
                        "        in.endObject();\n" +
                        "        return Test.builder(_arg)\n" +
                        "                .build();\n" +
                        "    }\n" +
                        "}"));
    }

    @Test
    public void oneComplexArgConstructor() {
        assertAbout(javaSource()).that(JavaFileObjects.forSourceString("test.Test",
                "package test;\n" +
                        "\n" +
                        "import me.tatarka.gsonvalue.annotations.GsonValue;\n" +
                        "import java.util.List;\n" +
                        "\n" +
                        "@GsonValue\n" +
                        "public class Test {\n" +
                        "    private final List<String> arg;\n" +
                        "    public Test(List<String> arg) {\n" +
                        "        this.arg = arg;\n" +
                        "    }\n" +
                        "    \n" +
                        "    public List<String> arg() {\n" +
                        "        return arg;\n" +
                        "    }\n" +
                        "}"))
                .processedWith(new GsonValueProcessor())
                .compilesWithoutError()
                .and().generatesSources(JavaFileObjects.forSourceString("test.ValueTypeAdapter_Test",
                "package test;\n" +
                        "\n" +
                        VALUE_TYPE_ADAPTER_IMPORTS +
                        "import java.util.List;\n" +
                        "\n" +
                        "public class ValueTypeAdapter_Test extends TypeAdapter<Test> {\n" +
                        "    private final TypeAdapter<List<String>> adapter_arg;\n" +
                        "    public ValueTypeAdapter_Test(Gson gson, TypeToken<Test> typeToken) {\n" +
                        "        this.adapter_arg = gson.getAdapter(new TypeToken<List<String>>() {});\n" +
                        "    }\n" +
                        "    @Override\n" +
                        "    public void write(JsonWriter out, Test value) throws IOException {\n" +
                        WRITE_NULL_CHECK +
                        "        out.beginObject();\n" +
                        "        out.name(\"arg\");\n" +
                        "        adapter_arg.write(out, value.arg());\n" +
                        "        out.endObject();\n" +
                        "    }\n" +
                        "\n" +
                        "    @Override\n" +
                        "    public Test read(JsonReader in) throws IOException {\n" +
                        READ_NULL_CHECK +
                        "        List<String> _arg = null;\n" +
                        "        in.beginObject();\n" +
                        "        while (in.hasNext()) {\n" +
                        "            switch (in.nextName()) {\n" +
                        "                case \"arg\":\n" +
                        "                    _arg = adapter_arg.read(in);\n" +
                        "                    break;\n" +
                        "                default:\n" +
                        "                    in.skipValue();\n" +
                        "            }\n" +
                        "        }\n" +
                        "        in.endObject();\n" +
                        "        return new Test(_arg);\n" +
                        "    }\n" +
                        "}"));
    }

    @Test
    public void genericConstructor() {
        assertAbout(javaSource()).that(JavaFileObjects.forSourceString("test.Test",
                "package test;\n" +
                        "\n" +
                        "import me.tatarka.gsonvalue.annotations.GsonValue;\n" +
                        "\n" +
                        "@GsonValue\n" +
                        "public class Test<T> {\n" +
                        "    private final T arg;\n" +
                        "    public Test(T arg) {\n" +
                        "        this.arg = arg;\n" +
                        "    }\n" +
                        "    \n" +
                        "    public T arg() {\n" +
                        "        return arg;\n" +
                        "    }\n" +
                        "}"))
                .processedWith(new GsonValueProcessor())
                .compilesWithoutError()
                .and().generatesSources(JavaFileObjects.forSourceString("test.ValueTypeAdapter_Test",
                "package test;\n" +
                        "\n" +
                        VALUE_TYPE_ADAPTER_IMPORTS +
                        "import java.lang.reflect.ParameterizedType;\n" +
                        "\n" +
                        "public class ValueTypeAdapter_Test<T> extends TypeAdapter<Test<T>> {\n" +
                        "    private final TypeAdapter<T> adapter_arg;\n" +
                        "    public ValueTypeAdapter_Test(Gson gson, TypeToken<Test<T>> typeToken) {\n" +
                        "        this.adapter_arg = gson.getAdapter((TypeToken<T>) TypeToken.get(((ParameterizedType)typeToken.getType()).getActualTypeArguments()[0]));" +
                        "    }\n" +
                        "    @Override\n" +
                        "    public void write(JsonWriter out, Test<T> value) throws IOException {\n" +
                        WRITE_NULL_CHECK +
                        "        out.beginObject();\n" +
                        "        out.name(\"arg\");\n" +
                        "        adapter_arg.write(out, value.arg());\n" +
                        "        out.endObject();\n" +
                        "    }\n" +
                        "\n" +
                        "    @Override\n" +
                        "    public Test<T> read(JsonReader in) throws IOException {\n" +
                        READ_NULL_CHECK +
                        "        T _arg = null;\n" +
                        "        in.beginObject();\n" +
                        "        while (in.hasNext()) {\n" +
                        "            switch (in.nextName()) {\n" +
                        "                case \"arg\":\n" +
                        "                    _arg = adapter_arg.read(in);\n" +
                        "                    break;\n" +
                        "                default:\n" +
                        "                    in.skipValue();\n" +
                        "            }\n" +
                        "        }\n" +
                        "        in.endObject();\n" +
                        "        return new Test<T>(_arg);\n" +
                        "    }\n" +
                        "}"));
    }

    @Test
    public void complexGenericConstructor() {
        assertAbout(javaSource()).that(JavaFileObjects.forSourceString("test.Test",
                "package test;\n" +
                        "\n" +
                        "import me.tatarka.gsonvalue.annotations.GsonValue;\n" +
                        "import java.util.List;\n" +
                        "\n" +
                        "@GsonValue\n" +
                        "public class Test<T> {\n" +
                        "    private final List<T> arg;\n" +
                        "    public Test(List<T> arg) {\n" +
                        "        this.arg = arg;\n" +
                        "    }\n" +
                        "    \n" +
                        "    public List<T> arg() {\n" +
                        "        return arg;\n" +
                        "    }\n" +
                        "}"))
                .processedWith(new GsonValueProcessor())
                .compilesWithoutError()
                .and().generatesSources(JavaFileObjects.forSourceString("test.ValueTypeAdapter_Test",
                "package test;\n" +
                        "\n" +
                        VALUE_TYPE_ADAPTER_IMPORTS +
                        "import java.lang.reflect.ParameterizedType;\n" +
                        "import java.util.List;\n" +
                        "\n" +
                        "public class ValueTypeAdapter_Test<T> extends TypeAdapter<Test<T>> {\n" +
                        "    private final TypeAdapter<List<T>> adapter_arg;\n" +
                        "    public ValueTypeAdapter_Test(Gson gson, TypeToken<Test<T>> typeToken) {\n" +
                        "        this.adapter_arg = gson.getAdapter((TypeToken<List<T>>) TypeToken.getParameterized(List.class, ((ParameterizedType)typeToken.getType()).getActualTypeArguments()[0]));\n" +
                        "    }\n" +
                        "    @Override\n" +
                        "    public void write(JsonWriter out, Test<T> value) throws IOException {\n" +
                        WRITE_NULL_CHECK +
                        "        out.beginObject();\n" +
                        "        out.name(\"arg\");\n" +
                        "        adapter_arg.write(out, value.arg());\n" +
                        "        out.endObject();\n" +
                        "    }\n" +
                        "\n" +
                        "    @Override\n" +
                        "    public Test<T> read(JsonReader in) throws IOException {\n" +
                        READ_NULL_CHECK +
                        "        List<T> _arg = null;\n" +
                        "        in.beginObject();\n" +
                        "        while (in.hasNext()) {\n" +
                        "            switch (in.nextName()) {\n" +
                        "                case \"arg\":\n" +
                        "                    _arg = adapter_arg.read(in);\n" +
                        "                    break;\n" +
                        "                default:\n" +
                        "                    in.skipValue();\n" +
                        "            }\n" +
                        "        }\n" +
                        "        in.endObject();\n" +
                        "        return new Test<T>(_arg);\n" +
                        "    }\n" +
                        "}"));
    }

    @Test
    public void abstractNamedField() {
        assertAbout(javaSource()).that(JavaFileObjects.forSourceString("test.Test",
                "package test;\n" +
                        "\n" +
                        "import com.google.gson.annotations.SerializedName;\n" +
                        "import me.tatarka.gsonvalue.annotations.GsonValue;\n" +
                        "\n" +
                        "@GsonValue\n" +
                        "public abstract class Test {\n" +
                        "\n" +
                        "    public static Test create(int arg) {\n" +
                        "        return new TestImpl(arg);\n" +
                        "    }\n" +
                        "    \n" +
                        "    @SerializedName(\"named\")\n" +
                        "    public abstract int arg();\n" +
                        "    \n" +
                        "    static class TestImpl extends Test {\n" +
                        "        private final int arg;\n" +
                        "        \n" +
                        "        TestImpl(int arg) {\n" +
                        "            this.arg = arg;\n" +
                        "        }\n" +
                        "\n" +
                        "        @Override\n" +
                        "        public int arg() {\n" +
                        "            return arg;\n" +
                        "        }\n" +
                        "    }\n" +
                        "}"))
                .processedWith(new GsonValueProcessor())
                .compilesWithoutError()
                .and().generatesSources(JavaFileObjects.forSourceString("test.ValueTypeAdapter_Test",
                "package test;\n" +
                        "\n" +
                        VALUE_TYPE_ADAPTER_IMPORTS +
                        "\n" +
                        "public class ValueTypeAdapter_Test extends TypeAdapter<Test> {\n" +
                        "    private final TypeAdapter<Integer> adapter_arg;\n" +
                        "    public ValueTypeAdapter_Test(Gson gson, TypeToken<Test> typeToken) {\n" +
                        "        this.adapter_arg = gson.getAdapter(int.class);\n" +
                        "    }\n" +
                        "    @Override\n" +
                        "    public void write(JsonWriter out, Test value) throws IOException {\n" +
                        WRITE_NULL_CHECK +
                        "        out.beginObject();\n" +
                        "        out.name(\"named\");\n" +
                        "        adapter_arg.write(out, value.arg());\n" +
                        "        out.endObject();\n" +
                        "    }\n" +
                        "\n" +
                        "    @Override\n" +
                        "    public Test read(JsonReader in) throws IOException {\n" +
                        READ_NULL_CHECK +
                        "        int _arg = 0;\n" +
                        "        in.beginObject();\n" +
                        "        while (in.hasNext()) {\n" +
                        "            switch (in.nextName()) {\n" +
                        "                case \"named\":\n" +
                        "                    _arg = adapter_arg.read(in);\n" +
                        "                    break;\n" +
                        "                default:\n" +
                        "                    in.skipValue();\n" +
                        "            }\n" +
                        "        }\n" +
                        "        in.endObject();\n" +
                        "        return Test.create(_arg);\n" +
                        "    }\n" +
                        "}"));
    }

    @Test
    public void adapterAnnotation() {
        assertAbout(javaSource()).that(JavaFileObjects.forSourceString("test.Test",
                "package test;\n" +
                        "\n" +
                        "import com.google.gson.annotations.JsonAdapter;\n" +
                        "import me.tatarka.gsonvalue.annotations.GsonValue;\n" +
                        "import me.tatarka.gsonvalue.model.adapters.StringToIntTypeAdapter;\n" +
                        "\n" +
                        "@GsonValue\n" +
                        "public class Test {\n" +
                        "    @JsonAdapter(StringToIntTypeAdapter.class)\n" +
                        "    private final int arg;\n" +
                        "    public Test(int arg) {\n" +
                        "        this.arg = arg;\n" +
                        "    }\n" +
                        "    \n" +
                        "    public int arg() {\n" +
                        "        return arg;\n" +
                        "    }\n" +
                        "}"))
                .processedWith(new GsonValueProcessor())
                .compilesWithoutError()
                .and().generatesSources(JavaFileObjects.forSourceString("test.ValueTypeAdapter_Test",
                "package test;\n" +
                        "\n" +
                        VALUE_TYPE_ADAPTER_IMPORTS +
                        "import me.tatarka.gsonvalue.model.adapters.StringToIntTypeAdapter;\n" +
                        "\n" +
                        "public class ValueTypeAdapter_Test extends TypeAdapter<Test> {\n" +
                        "    private final TypeAdapter<Integer> adapter_arg;\n" +
                        "    public ValueTypeAdapter_Test(Gson gson, TypeToken<Test> typeToken) {\n" +
                        "        this.adapter_arg = new StringToIntTypeAdapter();\n" +
                        "    }\n" +
                        "    @Override\n" +
                        "    public void write(JsonWriter out, Test value) throws IOException {\n" +
                        WRITE_NULL_CHECK +
                        "        out.beginObject();\n" +
                        "        out.name(\"arg\");\n" +
                        "        adapter_arg.write(out, value.arg());\n" +
                        "        out.endObject();\n" +
                        "    }\n" +
                        "\n" +
                        "    @Override\n" +
                        "    public Test read(JsonReader in) throws IOException {\n" +
                        READ_NULL_CHECK +
                        "        int _arg = 0;\n" +
                        "        in.beginObject();\n" +
                        "        while (in.hasNext()) {\n" +
                        "            switch (in.nextName()) {\n" +
                        "                case \"arg\":\n" +
                        "                    _arg = adapter_arg.read(in);\n" +
                        "                    break;\n" +
                        "                default:\n" +
                        "                    in.skipValue();\n" +
                        "            }\n" +
                        "        }\n" +
                        "        in.endObject();\n" +
                        "        return new Test(_arg);\n" +
                        "    }\n" +
                        "}"));
    }

    @Test
    public void adapterFactoryAnnotation() {
        assertAbout(javaSource()).that(JavaFileObjects.forSourceString("test.Test",
                "package test;\n" +
                        "\n" +
                        "import com.google.gson.annotations.JsonAdapter;\n" +
                        "import me.tatarka.gsonvalue.annotations.GsonValue;\n" +
                        "import me.tatarka.gsonvalue.model.adapters.StringToIntTypeAdapterFactory;\n" +
                        "\n" +
                        "@GsonValue\n" +
                        "public class Test {\n" +
                        "    @JsonAdapter(StringToIntTypeAdapterFactory.class)\n" +
                        "    private final int arg;\n" +
                        "    public Test(int arg) {\n" +
                        "        this.arg = arg;\n" +
                        "    }\n" +
                        "    \n" +
                        "    public int arg() {\n" +
                        "        return arg;\n" +
                        "    }\n" +
                        "}"))
                .processedWith(new GsonValueProcessor())
                .compilesWithoutError()
                .and().generatesSources(JavaFileObjects.forSourceString("test.ValueTypeAdapter_Test",
                "package test;\n" +
                        "\n" +
                        VALUE_TYPE_ADAPTER_IMPORTS +
                        "import me.tatarka.gsonvalue.model.adapters.StringToIntTypeAdapterFactory;\n" +
                        "\n" +
                        "public class ValueTypeAdapter_Test extends TypeAdapter<Test> {\n" +
                        "    private final TypeAdapter<Integer> adapter_arg;\n" +
                        "    public ValueTypeAdapter_Test(Gson gson, TypeToken<Test> typeToken) {\n" +
                        "        this.adapter_arg = new StringToIntTypeAdapterFactory().create(gson, TypeToken.get(int.class));\n" +
                        "    }\n" +
                        "    @Override\n" +
                        "    public void write(JsonWriter out, Test value) throws IOException {\n" +
                        WRITE_NULL_CHECK +
                        "        out.beginObject();\n" +
                        "        out.name(\"arg\");\n" +
                        "        adapter_arg.write(out, value.arg());\n" +
                        "        out.endObject();\n" +
                        "    }\n" +
                        "\n" +
                        "    @Override\n" +
                        "    public Test read(JsonReader in) throws IOException {\n" +
                        READ_NULL_CHECK +
                        "        int _arg = 0;\n" +
                        "        in.beginObject();\n" +
                        "        while (in.hasNext()) {\n" +
                        "            switch (in.nextName()) {\n" +
                        "                case \"arg\":\n" +
                        "                    _arg = adapter_arg.read(in);\n" +
                        "                    break;\n" +
                        "                default:\n" +
                        "                    in.skipValue();\n" +
                        "            }\n" +
                        "        }\n" +
                        "        in.endObject();\n" +
                        "        return new Test(_arg);\n" +
                        "    }\n" +
                        "}"));
    }

    @Test
    public void multipleConstructorsFails() {
        assertAbout(javaSource()).that(JavaFileObjects.forSourceString("test.Test",
                "package test;\n" +
                        "\n" +
                        "import me.tatarka.gsonvalue.annotations.GsonValue;\n" +
                        "\n" +
                        "@GsonValue\n" +
                        "public class Test {\n" +
                        "    public Test() {\n" +
                        "    }\n" +
                        "    public Test(int arg) {\n" +
                        "    }\n" +
                        "}"))
                .processedWith(new GsonValueProcessor())
                .failsToCompile()
                .withErrorContaining("More than one creation methods found on test.Test")
                .and()
                .withErrorContaining("  Test()")
                .and()
                .withErrorContaining("  Test(int)");
    }

    @Test
    public void annotatedConstructor() {
        assertAbout(javaSource()).that(JavaFileObjects.forSourceString("test.Test",
                "package test;\n" +
                        "\n" +
                        "import me.tatarka.gsonvalue.annotations.GsonValue;\n" +
                        "import me.tatarka.gsonvalue.annotations.GsonValue.Creator;\n" +
                        "\n" +
                        "@GsonValue\n" +
                        "public class Test {\n" +
                        "    private int arg;\n" +
                        "    public Test() {" +
                        "    }\n" +
                        "    @GsonValue.Creator" +
                        "    public Test(int arg) {\n" +
                        "        this.arg = arg;\n" +
                        "    }\n" +
                        "    \n" +
                        "    public int arg() {\n" +
                        "        return arg;\n" +
                        "    }\n" +
                        "}"))
                .processedWith(new GsonValueProcessor())
                .compilesWithoutError()
                .and().generatesSources(JavaFileObjects.forSourceString("test.ValueTypeAdapter_Test",
                "package test;\n" +
                        "\n" +
                        VALUE_TYPE_ADAPTER_IMPORTS +
                        "\n" +
                        "public class ValueTypeAdapter_Test extends TypeAdapter<Test> {\n" +
                        "    private final TypeAdapter<Integer> adapter_arg;\n" +
                        "    public ValueTypeAdapter_Test(Gson gson, TypeToken<Test> typeToken) {\n" +
                        "        this.adapter_arg = gson.getAdapter(int.class);\n" +
                        "    }\n" +
                        "    @Override\n" +
                        "    public void write(JsonWriter out, Test value) throws IOException {\n" +
                        WRITE_NULL_CHECK +
                        "        out.beginObject();\n" +
                        "        out.name(\"arg\");\n" +
                        "        adapter_arg.write(out, value.arg());\n" +
                        "        out.endObject();\n" +
                        "    }\n" +
                        "\n" +
                        "    @Override\n" +
                        "    public Test read(JsonReader in) throws IOException {\n" +
                        READ_NULL_CHECK +
                        "        int _arg = 0;\n" +
                        "        in.beginObject();\n" +
                        "        while (in.hasNext()) {\n" +
                        "            switch (in.nextName()) {\n" +
                        "                case \"arg\":\n" +
                        "                    _arg = adapter_arg.read(in);\n" +
                        "                    break;\n" +
                        "                default:\n" +
                        "                    in.skipValue();\n" +
                        "            }\n" +
                        "        }\n" +
                        "        in.endObject();\n" +
                        "        return new Test(_arg);\n" +
                        "    }\n" +
                        "}"));
    }
}
