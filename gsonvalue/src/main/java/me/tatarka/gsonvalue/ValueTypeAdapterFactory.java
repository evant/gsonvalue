package me.tatarka.gsonvalue;

import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;
import me.tatarka.gsonvalue.annotations.GsonBuilder;
import me.tatarka.gsonvalue.annotations.GsonConstructor;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class ValueTypeAdapterFactory implements TypeAdapterFactory {
    private static final ConcurrentMap<TypeToken<?>, TypeAdapter<?>> TYPE_MAP = new ConcurrentHashMap<TypeToken<?>, TypeAdapter<?>>();

    @Override
    public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type) {
        Class<? super T> rawType = type.getRawType();
        if (!hasValueTypeAdapter(rawType)) {
            return null;
        }

        TypeAdapter<T> adapter = (TypeAdapter<T>) TYPE_MAP.get(type);
        if (adapter != null) {
            return adapter;
        }

        String packageName = rawType.getPackage().getName();
        String className = rawType.getName().substring(packageName.length() + 1).replace('$', '_');
        String typeAdapterClassName = packageName + ".ValueTypeAdapter_" + className;

        try {
            Class<TypeAdapter<T>> typeAdapterClass = (Class<TypeAdapter<T>>) Class.forName(typeAdapterClassName);
            Constructor<TypeAdapter<T>> constructor = typeAdapterClass.getConstructor(Gson.class, TypeToken.class);
            TypeAdapter<T> typeAdapter = constructor.newInstance(gson, type);
            TYPE_MAP.put(type, typeAdapter);
            return typeAdapter;
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Could not load ValueTypeAdapter " + typeAdapterClassName, e);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException("Could not load ValueTypeAdapter " + typeAdapterClassName, e);
        } catch (InstantiationException e) {
            throw new RuntimeException("Could not load ValueTypeAdapter " + typeAdapterClassName, e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Could not load ValueTypeAdapter " + typeAdapterClassName, e);
        } catch (InvocationTargetException e) {
            throw new RuntimeException("Could not load ValueTypeAdapter " + typeAdapterClassName, e);
        }
    }

    private boolean hasValueTypeAdapter(Class<?> type) {
        for (Constructor<?> constructor : type.getConstructors()) {
            if (constructor.isAnnotationPresent(GsonConstructor.class)) {
                return true;
            }
        }
        for (Method method : type.getMethods()) {
            if (method.isAnnotationPresent(GsonConstructor.class) || method.isAnnotationPresent(GsonBuilder.class)) {
                return true;
            }
        }
        for (Class<?> classes : type.getClasses()) {
            for (Constructor<?> constructor : classes.getConstructors()) {
                if (constructor.isAnnotationPresent(GsonBuilder.class)) {
                    return true;
                }
            }
        }
        return false;
    }
}
