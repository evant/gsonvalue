package me.tatarka.gsonvalue.processor;

import com.google.testing.compile.JavaFileObjects;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.Arrays;

import me.tatarka.gsonvalue.GsonValueProcessor;
import me.tatarka.gsonvalue.GsonValueTypeAdapterFactoryProcessor;

import static com.google.common.truth.Truth.assertAbout;
import static com.google.testing.compile.JavaSourceSubjectFactory.javaSource;
import static com.google.testing.compile.JavaSourcesSubjectFactory.javaSources;

@RunWith(JUnit4.class)
public class GsonValueTypeAdapterFactoryProcessorTest {

    @Test
    public void emptyFactory() {
        assertAbout(javaSource()).that(JavaFileObjects.forSourceString("test.TestFactory",
                "package test;\n" +
                        "\n" +
                        "import com.google.gson.TypeAdapterFactory;\n" +
                        "import me.tatarka.gsonvalue.annotations.GsonValueTypeAdapterFactory;\n" +
                        "\n" +
                        "@GsonValueTypeAdapterFactory\n" +
                        "public abstract class TestFactory implements TypeAdapterFactory {\n" +
                        "    public static TestFactory create() {\n" +
                        "        return new GsonValue_TestFactory();\n" +
                        "    }\n" +
                        "}"))
                .processedWith(new GsonValueTypeAdapterFactoryProcessor())
                .compilesWithoutError()
                .and().generatesSources(JavaFileObjects.forSourceString("test.GsonValue_TestFactory",
                "package test;\n" +
                        "\n" +
                        "import com.google.gson.Gson;\n" +
                        "import com.google.gson.TypeAdapter;\n" +
                        "import com.google.gson.reflect.TypeToken;\n" +
                        "import java.lang.Override;\n" +
                        "import java.lang.SuppressWarnings;\n" +
                        "\n" +
                        "final class GsonValue_TestFactory extends TestFactory {\n" +
                        "    @Override\n" +
                        "    @SuppressWarnings(\"unchecked\")\n" +
                        "    public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type) {\n" +
                        "        return null;\n" +
                        "    }\n" +
                        "}"));
    }

    @Test
    public void factoryWithOneClass() {
        assertAbout(javaSources()).that(Arrays.asList(JavaFileObjects.forSourceString("test.TestFactory",
                "package test;\n" +
                        "\n" +
                        "import com.google.gson.TypeAdapterFactory;\n" +
                        "import me.tatarka.gsonvalue.annotations.GsonValueTypeAdapterFactory;\n" +
                        "\n" +
                        "@GsonValueTypeAdapterFactory\n" +
                        "public abstract class TestFactory implements TypeAdapterFactory {\n" +
                        "    public static TestFactory create() {\n" +
                        "        return new GsonValue_TestFactory();\n" +
                        "    }\n" +
                        "}"), JavaFileObjects.forSourceString("test.Test",
                "package test;\n" +
                        "\n" +
                        "import me.tatarka.gsonvalue.annotations.GsonValue;\n" +
                        "\n" +
                        "@GsonValue\n" +
                        "public class Test {\n" +
                        "    public Test() {\n" +
                        "    }\n" +
                        "}")))
                .processedWith(new GsonValueProcessor(), new GsonValueTypeAdapterFactoryProcessor())
                .compilesWithoutError()
                .and().generatesSources(JavaFileObjects.forSourceString("test.GsonValue_TestFactory",
                "package test;\n" +
                        "\n" +
                        "import com.google.gson.Gson;\n" +
                        "import com.google.gson.TypeAdapter;\n" +
                        "import com.google.gson.reflect.TypeToken;\n" +
                        "import java.lang.Override;\n" +
                        "import java.lang.SuppressWarnings;\n" +
                        "\n" +
                        "final class GsonValue_TestFactory extends TestFactory {\n" +
                        "    @Override\n" +
                        "    @SuppressWarnings(\"unchecked\")\n" +
                        "    public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type) {\n" +
                        "        Class<?> clazz = type.getRawType();\n" +
                        "        if (clazz == test.Test.class) {\n" +
                        "           return (TypeAdapter<T>) new ValueTypeAdapter_Test(gson, (TypeToken<Test>) type);\n" +
                        "        } else {" +
                        "           return null;\n" +
                        "        }"+
                        "    }\n" +
                        "}"));
    }
}
