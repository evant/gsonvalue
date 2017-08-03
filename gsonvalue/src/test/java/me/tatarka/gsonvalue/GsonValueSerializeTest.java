package me.tatarka.gsonvalue;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapterFactory;
import me.tatarka.gsonvalue.model.adapterfactory.MyTypeAdapterFactory;
import me.tatarka.gsonvalue.model.serialize.*;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;

import static org.junit.Assert.assertEquals;

@RunWith(Parameterized.class)
public class GsonValueSerializeTest {

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> params() {
        return Arrays.asList(new Object[]{new ValueTypeAdapterFactory()}, new Object[]{MyTypeAdapterFactory.create()});
    }

    final TypeAdapterFactory factory;
    Gson gson;

    public GsonValueSerializeTest(TypeAdapterFactory factory) {
        this.factory = factory;
    }

    @Before
    public void setup() {
        gson = new GsonBuilder()
                .registerTypeAdapterFactory(factory)
                .create();
    }

    @Test
    public void serializeEmpty() {
        Empty empty = new Empty();
        String json = gson.toJson(empty);

        assertEquals("{}", json);
    }

    @Test
    public void serializePublicField() {
        PublicField publicField = new PublicField(1);
        String json = gson.toJson(publicField);

        assertEquals("{\"arg\":1}", json);
    }

    @Test
    public void serializeGetter() {
        Getter getter = new Getter(1);
        String json = gson.toJson(getter);

        assertEquals("{\"arg\":1}", json);
    }

    @Test
    public void serializeBeanGetter() {
        BeanGetter getter = new BeanGetter(1);
        String json = gson.toJson(getter);

        assertEquals("{\"arg\":1}", json);
    }

    @Test
    public void serializeNamedField() {
        NamedField namedField = new NamedField(1);
        String json = gson.toJson(namedField);

        assertEquals("{\"named\":1}", json);
    }

    @Test
    public void serializeNamedMethod() {
        NamedMethod namedMethod = new NamedMethod(1);
        String json = gson.toJson(namedMethod);

        assertEquals("{\"named\":1}", json);
    }

    @Test
    public void serializeNamedFieldForMethod() {
        NamedFieldForMethod namedFieldForMethod = new NamedFieldForMethod(1);
        String json = gson.toJson(namedFieldForMethod);

        assertEquals("{\"named\":1}", json);
    }

    @Test
    public void serializeComplexArg() {
        ComplexArg complexArg = new ComplexArg(Arrays.asList("one", "two"));
        String json = gson.toJson(complexArg);

        assertEquals("{\"args\":[\"one\",\"two\"]}", json);
    }

    @Test
    public void serializeIgnoreObjectMethods() {
        IgnoreObjectMethods ignoreObjectMethods = new IgnoreObjectMethods(1);
        String json = gson.toJson(ignoreObjectMethods);

        assertEquals("{\"arg\":1}", json);
    }

    @Test
    public void serializeSubclass() {
        Subclass.Child subclass = new Subclass.Child(1, 2, 3);
        String json = gson.toJson(subclass);

        assertEquals("{\"arg1\":1,\"arg2\":2,\"arg3\":3}", json);
    }

    @Test
    public void serializeTransient() {
        TransientField field = new TransientField(1);
        String json = gson.toJson(field);

        assertEquals("{\"arg\":1}", json);
    }
}
