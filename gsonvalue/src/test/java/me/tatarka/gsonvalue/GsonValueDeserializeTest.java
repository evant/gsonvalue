package me.tatarka.gsonvalue;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;
import me.tatarka.gsonvalue.model.adapterfactory.MyTypeAdapterFactory;
import me.tatarka.gsonvalue.model.deserialize.*;
import me.tatarka.gsonvalue.model.roundtrip.Subclass;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;

import static org.junit.Assert.*;

@RunWith(JUnit4.class)
public class GsonValueDeserializeTest {

    TypeAdapterFactory factory;
    Gson gson;

    @Before
    public void setup() {
        factory = MyTypeAdapterFactory.create();
        gson = new GsonBuilder()
                .registerTypeAdapterFactory(factory)
                .create();
    }

    @Test
    public void deserializeWithNull() {
        NullableField result = gson.fromJson("{\"arg\":null}", NullableField.class);
        assertNull(result.arg);
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
    public void deserializeBuilderNamedField() {
        BuilderNamedField namedField = gson.fromJson("{\"named\":1}", BuilderNamedField.class);

        assertNotNull(namedField);
        assertEquals(1, namedField.arg());
    }

    @Test
    public void deserializeAbstractBuilderNamedField() {
        AbstractNamedField namedField = gson.fromJson("{\"named\":1}", AbstractNamedField.class);

        assertNotNull(namedField);
        assertEquals(1, namedField.arg());
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

    @Test
    public void deserializeIgnoreJsonField() {
        ConstructorArg constructorArg = gson.fromJson("{\"arg\":1,\"ignore\":2}", ConstructorArg.class);

        assertNotNull(constructorArg);
        assertEquals(1, constructorArg.arg);
        assertTrue(constructorArg.constructorCalled);
    }
}
