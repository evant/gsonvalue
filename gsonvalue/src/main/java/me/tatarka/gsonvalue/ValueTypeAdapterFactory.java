package me.tatarka.gsonvalue;

import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class ValueTypeAdapterFactory implements TypeAdapterFactory {
    private static final ConcurrentMap<TypeToken<?>, TypeAdapter<?>> TYPE_MAP = new ConcurrentHashMap<TypeToken<?>, TypeAdapter<?>>();

    @Override
    @SuppressWarnings("unchecked")
    public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type) {
        Class<? super T> rawType = type.getRawType();
        if (!shouldLookForValueTypeAdapter(rawType)) {
            return null;
        }

        TypeAdapter<T> adapter = (TypeAdapter<T>) TYPE_MAP.get(type);
        if (adapter != null) {
            return adapter;
        }

        Package p = rawType.getPackage();
        String packageName = p != null ? p.getName() + "." : "";
        String className = rawType.getName().substring(packageName.length()).replace('$', '_');
        String typeAdapterClassName = packageName + "ValueTypeAdapter_" + className;

        try {
            Class<TypeAdapter<T>> typeAdapterClass = (Class<TypeAdapter<T>>) Class.forName(typeAdapterClassName);
            Constructor<TypeAdapter<T>> constructor = typeAdapterClass.getConstructor(Gson.class, TypeToken.class);
            TypeAdapter<T> typeAdapter = constructor.newInstance(gson, type);
            TYPE_MAP.put(type, typeAdapter);
            return typeAdapter;
        } catch (ClassNotFoundException e) {
            return null;
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

    private boolean shouldLookForValueTypeAdapter(Class<?> type) {
        if (type.isPrimitive()) {
            return false;
        }
        Package p = type.getPackage();
        if (p != null) {
            String packageName = p.getName();
            if (packageName.startsWith("java.")
                    || packageName.startsWith("javax.")
                    || packageName.startsWith("android.")) {
                return false;
            }
        }
        return true;
    }
}
