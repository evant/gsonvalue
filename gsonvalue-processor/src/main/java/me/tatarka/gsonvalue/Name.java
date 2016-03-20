package me.tatarka.gsonvalue;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import java.util.List;

abstract class Name<E extends Element> {
    private static final String SERIALIZED_NAME = "com.google.gson.annotations.SerializedName";

    protected final E element;
    protected String serializeName;
    protected List<? extends AnnotationMirror> annotations;
    private final Kind kind;

    public Name(E element, Kind kind) {
        this.element = element;
        serializeName = serializeName(element);
        annotations = element.getAnnotationMirrors();
        this.kind = kind;
    }

    public String getName() {
        return element.getSimpleName().toString();
    }

    public String getCallableName() {
        return getName();
    }

    public TypeMirror getType() {
        return element.asType();
    }

    public String getSerializeName() {
        return serializeName != null ? serializeName : getName();
    }

    public Kind getKind() {
        return kind;
    }

    public Class getAdapterType() {
        return null;
    }

    @Override
    public String toString() {
        return getName();
    }

    enum Kind {
        FIELD, GETTER, CONSTRUCTOR_PARAM, BUILDER_PARAM
    }

    private static String serializeName(Element element) {
        for (AnnotationMirror annotationMirror : element.getAnnotationMirrors()) {
            if (annotationMirror.getAnnotationType().asElement().toString().equals(SERIALIZED_NAME)) {
                return (String) annotationMirror.getElementValues().values().iterator().next().getValue();
            }
        }
        return null;
    }

    public static class FieldName extends Name<VariableElement> {
        public FieldName(VariableElement field) {
            super(field, Kind.FIELD);
        }
    }

    public static class GetterName extends Name<ExecutableElement> {
        private static final String BEAN_PREFIX_BOOL = "is";
        private static final String BEAN_PREFIX = "get";

        private boolean stripBean;

        public GetterName(ExecutableElement method) {
            super(method, Kind.GETTER);
        }

        void stripBean() {
            stripBean = true;
        }

        boolean isBean() {
            return beanPrefix() != null;
        }

        String beanPrefix() {
            if (serializeName != null) {
                return null;
            }
            if (element.getReturnType().getKind() == TypeKind.BOOLEAN) {
                String name = super.getName();
                if (name.length() > BEAN_PREFIX_BOOL.length() && name.startsWith(BEAN_PREFIX_BOOL)) {
                    return BEAN_PREFIX_BOOL;
                }
            }
            String name = super.getName();
            return name.length() > BEAN_PREFIX.length() && name.startsWith(BEAN_PREFIX) ? BEAN_PREFIX : null;
        }

        @Override
        public String getName() {
            String name = super.getName();
            if (stripBean) {
                String prefix = beanPrefix();
                if (prefix != null) {
                    return Character.toLowerCase(name.charAt(prefix.length())) + name.substring(prefix.length() + 1);
                }
            }
            return name;
        }

        @Override
        public String getCallableName() {
            return super.getName();
        }

        @Override
        public TypeMirror getType() {
            return element.getReturnType();
        }
    }

    public static class ConstructorParamName extends Name<VariableElement> {
        public ConstructorParamName(VariableElement param) {
            super(param, Kind.CONSTRUCTOR_PARAM);
        }
    }

    public static class BuilderParamName extends Name<VariableElement> {
        private final ExecutableElement method;

        public BuilderParamName(ExecutableElement method) {
            super(method.getParameters().get(0), Kind.BUILDER_PARAM);
            this.method = method;
        }

        @Override
        public String getCallableName() {
            return method.getSimpleName().toString();
        }
    }
}
