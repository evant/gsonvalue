package me.tatarka.gsonvalue;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import me.tatarka.gsonvalue.model.deserialize.*;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.Arrays;

import static org.junit.Assert.*;

@RunWith(JUnit4.class)
public class GsonValueDeserializeTest {

    Gson gson;

    @Before
    public void setup() {
        gson = new GsonBuilder()
                .registerTypeAdapterFactory(new ValueTypeAdapterFactory())
                .create();
    }

    @Test
    public void deserializeEmpty() {
        Empty empty = gson.fromJson("{}", Empty.class);

        assertNotNull(empty);
    }

    @Test
    public void deserializeConstructorArg() {
        ConstructorArg constructorArg = gson.fromJson("{\"arg\":1}", ConstructorArg.class);

        assertNotNull(constructorArg);
        assertEquals(1, constructorArg.arg);
        assertTrue(constructorArg.constructorCalled);
    }

    @Test
    public void deserializeMethodArg() {
        MethodArg methodArg = gson.fromJson("{\"arg\":1}", MethodArg.class);

        assertNotNull(methodArg);
        assertEquals(1, methodArg.arg);
        assertTrue(methodArg.factoryMethodCalled);
    }

    @Test
    public void deserializeBuilderConstructorArg() {
        BuilderConstructorArg builderArg = gson.fromJson("{\"arg\":1}", BuilderConstructorArg.class);

        assertNotNull(builderArg);
        assertEquals(1, builderArg.arg);
        assertTrue(builderArg.builderCalled);
    }

    @Test
    public void deserializeBuilderMethodArg() {
        BuilderMethodArg builderArg = gson.fromJson("{\"arg\":1}", BuilderMethodArg.class);

        assertNotNull(builderArg);
        assertEquals(1, builderArg.arg);
        assertTrue(builderArg.builderCalled);
    }

    @Test
    public void deserializeBuilderConstructorRequiredArg() {
        BuilderConstructorRequiredArg builderArg = gson.fromJson("{\"arg\":1}", BuilderConstructorRequiredArg.class);

        assertNotNull(builderArg);
        assertEquals(1, builderArg.arg);
        assertTrue(builderArg.builderCalled);
    }

    @Test
    public void deserializeBuilderMethodRequiredArg() {
        BuilderMethodRequiredArg builderArg = gson.fromJson("{\"arg\":1}", BuilderMethodRequiredArg.class);

        assertNotNull(builderArg);
        assertEquals(1, builderArg.arg);
        assertTrue(builderArg.builderCalled);
    }

    @Test
    public void deserializeNamedField() {
        NamedField namedField = gson.fromJson("{\"named\":1}", NamedField.class);

        assertNotNull(namedField);
        assertEquals(1, namedField.arg);
    }

    @Test
    public void deserializeComplexArg() {
        ComplexArg complexArg = gson.fromJson("{\"args\":[\"one\",\"two\"]}", ComplexArg.class);

        assertNotNull(complexArg);
        assertEquals(Arrays.asList("one", "two"), complexArg.args);
    }

    @Test
    public void deserializeGenericConstructorArg() {
        GenericConstructorArg<Integer> constructorArg = gson.fromJson("{\"arg\":1}", new TypeToken<GenericConstructorArg<Integer>>() {
        }.getType());

        assertNotNull(constructorArg);
        assertEquals(Integer.valueOf(1), constructorArg.arg);
        assertTrue(constructorArg.constructorCalled);
    }

    @Test
    public void deserializeComplexGenericConstructorArg() {
        ComplexGenericConstructorArg<Integer> constructorArg = gson.fromJson("{\"arg\":[1,2]}", new TypeToken<ComplexGenericConstructorArg<Integer>>() {
        }.getType());

        assertNotNull(constructorArg);
        assertEquals(Arrays.asList(1, 2), constructorArg.arg);
        assertTrue(constructorArg.constructorCalled);
    }

    @Test
    public void deserializeGenericMethodArg() {
        GenericMethodArg<Integer> constructorArg = gson.fromJson("{\"arg\":1}", new TypeToken<GenericMethodArg<Integer>>() {
        }.getType());

        assertNotNull(constructorArg);
        assertEquals(Integer.valueOf(1), constructorArg.arg);
        assertTrue(constructorArg.factoryMethodCalled);
    }
}
