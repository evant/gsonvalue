package me.tatarka.gsonvalue;

import java.util.Iterator;

final class StringUtils {
    static <T> String join(String sep, Iterable<T> collection) {
        return join(sep, collection, new ToString<T>() {
            @Override
            public String toString(T value) {
                return value.toString();
            }
        });
    }

    static <T> String join(String sep, Iterable<T> collection, ToString<? super T> toString) {
        StringBuilder result = new StringBuilder();
        Iterator<T> itr = collection.iterator();
        while (itr.hasNext()) {
            T next = itr.next();
            result.append(toString.toString(next));
            if (itr.hasNext()) {
                result.append(sep);
            }
        }
        return result.toString();
    }

    interface ToString<T> {
        String toString(T value);
    }
}
