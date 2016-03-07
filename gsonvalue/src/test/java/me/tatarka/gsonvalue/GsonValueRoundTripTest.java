package me.tatarka.gsonvalue;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import me.tatarka.gsonvalue.model.roundtrip.Empty;
import me.tatarka.gsonvalue.model.roundtrip.PublicField;
import me.tatarka.gsonvalue.model.roundtrip.WithJsonAdapterField;
import me.tatarka.gsonvalue.model.roundtrip.WithJsonAdapterMethod;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import static org.junit.Assert.assertEquals;

@RunWith(JUnit4.class)
public class GsonValueRoundTripTest {

    Gson gson;

    @Before
    public void setup() {
        gson = new GsonBuilder()
                .registerTypeAdapterFactory(new ValueTypeAdapterFactory())
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
