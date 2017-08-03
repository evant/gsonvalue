package me.tatarka.gsonvalue;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapterFactory;
import me.tatarka.gsonvalue.model.adapterfactory.MyTypeAdapterFactory;
import me.tatarka.gsonvalue.model.roundtrip.Empty;
import me.tatarka.gsonvalue.model.roundtrip.PublicField;
import me.tatarka.gsonvalue.model.roundtrip.WithJsonAdapterField;
import me.tatarka.gsonvalue.model.roundtrip.WithJsonAdapterMethod;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;

import static org.junit.Assert.assertEquals;

@RunWith(Parameterized.class)
public class GsonValueRoundTripTest {

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> params() {
        return Arrays.asList(new Object[]{new ValueTypeAdapterFactory()}, new Object[]{MyTypeAdapterFactory.create()});
    }

    final TypeAdapterFactory factory;
    Gson gson;

    public GsonValueRoundTripTest(TypeAdapterFactory factory) {
        this.factory = factory;
    }

    @Before
    public void setup() {
        gson = new GsonBuilder()
                .registerTypeAdapterFactory(factory)
                .create();
    }

    @Test
    public void roundTripEmpty() {
        String json = "{}";
        String newJson = gson.toJson(gson.fromJson(json, Empty.class));

        assertEquals(json, newJson);
    }

    @Test
    public void roundTripPublicField() {
        String json = "{\"arg\":1}";
        String newJson = gson.toJson(gson.fromJson(json, PublicField.class));

        assertEquals(json, newJson);
    }

    @Test
    public void roundTripJsonAdapterField() {
        String json = "{\"arg\":\"1\"}";
        String newJson = gson.toJson(gson.fromJson(json, WithJsonAdapterField.class));

        assertEquals(json, newJson);
    }

    @Test
    public void roundTripJsonAdapterMethod() {
        String json = "{\"arg\":\"1\"}";
        String newJson = gson.toJson(gson.fromJson(json, WithJsonAdapterMethod.class));

        assertEquals(json, newJson);
    }
}
