package me.tatarka.gsonvalue;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

class Names {
    private List<Name.GetterName> getters = new ArrayList<Name.GetterName>();
    private List<Name.FieldName> fields = new ArrayList<Name.FieldName>();
    private List<Name.ConstructorParamName> constructorParams = new ArrayList<Name.ConstructorParamName>();
    private List<Name.BuilderParamName> builderParams = new ArrayList<Name.BuilderParamName>();
    private List<Name> params = new ArrayList<Name>();

    public void addGetter(ExecutableElement method) {
        Set<Modifier> modifiers = method.getModifiers();
        if (modifiers.contains(Modifier.PRIVATE) ||
                modifiers.contains(Modifier.STATIC) ||
                method.getReturnType().getKind() == TypeKind.VOID ||
                !method.getParameters().isEmpty()) {
            return;
        }
        getters.add(new Name.GetterName(method));
    }

    public void addField(VariableElement field) {
        Set<Modifier> modifiers = field.getModifiers();
        if (modifiers.contains(Modifier.STATIC) ||
                modifiers.contains(Modifier.TRANSIENT)) {
            return;
        }
        fields.add(new Name.FieldName(field));
    }

    public void addConstructorParam(VariableElement param) {
        Name.ConstructorParamName name = new Name.ConstructorParamName(param);
        constructorParams.add(name);
        params.add(name);
    }

    public void addBuilderParam(TypeMirror builderType, ExecutableElement method) {
        if (method.getReturnType().equals(builderType) && method.getParameters().size() == 1) {
            Name.BuilderParamName name = new Name.BuilderParamName(method);
            builderParams.add(name);
            params.add(name);
        }
    }

    public void finish() throws ElementException {
        removeExtraMethods(getters, params, /*includeBeans=*/false);
        stripBeans(getters);
        removeExtraMethods(getters, params, /*includeBeans=*/true);
        removeExtraBuilders();
        mergeSerializeNames(params, fields, getters);
        removeExtraFields();
    }

    private static void merge(Name... names) throws ElementException {
        if (names.length == 0) {
            return;
        }

        String serializeName = null;
        for (Name name : names) {
            if (name == null) {
                continue;
            }
            if (name.serializeName != null) {
                if (serializeName == null) {
                    serializeName = name.serializeName;
                } else {
                    throw new ElementException("Duplicate @SerializeName() found on " + name, name.element);
                }
            }
        }
        if (serializeName != null) {
            for (Name name : names) {
                if (name != null) {
                    name.serializeName = serializeName;
                }
            }
        }
    }

    private static void removeExtraMethods(List<Name.GetterName> getters, List<Name> params, boolean includeBeans) {
        for (int i = getters.size() - 1; i >= 0; i--) {
            Name.GetterName getter = getters.get(i);
            if (includeBeans || !getter.isBean()) {
                if (!containsName(params, getter)) {
                    getters.remove(i);
                }
            }
        }
    }

    private static void stripBeans(List<Name.GetterName> getters) {
        boolean allBeans = true;
        for (Name.GetterName getter : getters) {
            if (!getter.isBean()) {
                allBeans = false;
            }
        }
        if (allBeans) {
            for (Name.GetterName getter : getters) {
                getter.stripBean();

            }
        }
    }

    private void removeExtraBuilders() {
        for (int i = builderParams.size() - 1; i >= 0; i--) {
            Name.BuilderParamName builderParam = builderParams.get(i);
            if (containsName(constructorParams, builderParam)) {
                builderParams.remove(i);
                params.remove(builderParam);
            }
        }
    }

    private void removeExtraFields() {
        for (int i = fields.size() - 1; i >= 0; i--) {
            Name.FieldName field = fields.get(i);
            if (field.element.getModifiers().contains(Modifier.PRIVATE) || containsName(getters, field)) {
                fields.remove(i);
            }
        }
    }

    @SafeVarargs
    private static void mergeSerializeNames(List<? extends Name>... nameLists) throws ElementException {
        if (nameLists.length == 0) {
            return;
        }
        for (Name name : nameLists[0]) {
            Name[] names = new Name[nameLists.length];
            names[0] = name;
            for (int i = 1; i < nameLists.length; i++) {
                names[i] = findName(nameLists[i], name);
            }
            merge(names);
        }
    }

    private static Name findName(List<? extends Name> names, Name name) {
        for (Name n : names) {
            if (n.getName().equals(name.getName())) {
                return n;
            }
        }
        return null;
    }

    private static boolean containsName(List<? extends Name> names, Name name) {
        return findName(names, name) != null;
    }

    public Iterable<Name> params() {
        return params;
    }

    public Iterable<Name.FieldName> fields() {
        return fields;
    }

    public Iterable<Name.GetterName> getters() {
        return getters;
    }

    public Iterable<Name.ConstructorParamName> constructorParams() {
        return constructorParams;
    }

    public Iterable<Name.BuilderParamName> builderParams() {
        return builderParams;
    }
}
