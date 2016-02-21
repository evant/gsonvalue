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
        mergeSerializeNames(fields, getters);
        mergeSerializeNames(params, fields);
        removeExtraFields();
    }

    private static void merge(Name newName, Name existingName) throws ElementException {
        if (newName == null || existingName == null) {
            return;
        }
        if (newName.serializeName != null && existingName.serializeName != null) {
            throw new ElementException("Duplicate @SerializeName() found on " + newName + " and " + existingName, newName.element);
        }
        if (newName.serializeName == null && existingName.serializeName != null) {
            newName.serializeName = existingName.serializeName;
        } else if (newName.serializeName != null) {
            existingName.serializeName = newName.serializeName;
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

    private static void mergeSerializeNames(List<? extends Name> one, List<? extends Name> two) throws ElementException {
        for (Name o : one) {
            for (Name t : two) {
                if (o.getName().equals(t.getName())) {
                    merge(o, t);
                }
            }
        }
    }

    private static boolean containsName(List<? extends Name> names, Name name) {
        for (Name n : names) {
            if (n.getName().equals(name.getName())) {
                return true;
            }
        }
        return false;
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
