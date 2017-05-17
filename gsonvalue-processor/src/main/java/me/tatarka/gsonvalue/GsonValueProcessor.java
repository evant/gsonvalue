package me.tatarka.gsonvalue;

import com.squareup.javapoet.*;
import me.tatarka.gsonvalue.annotations.GsonBuilder;
import me.tatarka.gsonvalue.annotations.GsonConstructor;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.*;
import javax.lang.model.element.Modifier;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import javax.lang.model.util.Elements;
import javax.lang.model.util.SimpleTypeVisitor6;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.reflect.*;
import java.util.*;

@SupportedSourceVersion(SourceVersion.RELEASE_7)
public class GsonValueProcessor extends AbstractProcessor {
    private static final String PREFIX = "ValueTypeAdapter_";
    private static final String ARG_PREFIX = "_";
    private static final String TYPE_ADAPTER_PREFIX = "adapter_";
    private static final ClassName GSON = ClassName.get("com.google.gson", "Gson");
    private static final ClassName TYPE_ADAPTER = ClassName.get("com.google.gson", "TypeAdapter");
    private static final ClassName TYPE_ADAPTER_FACTORY = ClassName.get("com.google.gson", "TypeAdapterFactory");
    private static final ClassName JSON_WRITER = ClassName.get("com.google.gson.stream", "JsonWriter");
    private static final ClassName JSON_READER = ClassName.get("com.google.gson.stream", "JsonReader");
    private static final ClassName TYPE_TOKEN = ClassName.get("com.google.gson.reflect", "TypeToken");
    private static final ClassName JSON_ADAPTER = ClassName.get("com.google.gson.annotations", "JsonAdapter");
    private static final ClassName JSON_ADAPTER_METHOD = ClassName.get("me.tatarka.gsonvalue.annotations", "JsonAdapter");

    private Messager messager;
    private Filer filer;
    private Elements elementUtils;
    private Types typeUtils;
    private List<ClassName> seen;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        messager = processingEnv.getMessager();
        filer = processingEnv.getFiler();
        elementUtils = processingEnv.getElementUtils();
        typeUtils = processingEnv.getTypeUtils();
        seen = new ArrayList<>();
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        for (TypeElement annotation : annotations) {
            Set<? extends Element> elements = roundEnv.getElementsAnnotatedWith(annotation);
            for (Element element : elements) {
                if (element.getAnnotation(GsonConstructor.class) != null || element.getAnnotation(GsonBuilder.class) != null) {
                    try {
                        process(element);
                    } catch (IOException e) {
                        StringWriter stringWriter = new StringWriter();
                        e.printStackTrace(new PrintWriter(stringWriter));
                        messager.printMessage(Diagnostic.Kind.ERROR, "GsonValue threw an exception: " + stringWriter.toString(), element);
                    }
                }
            }
        }
        return false;
    }

    private void process(Element element) throws IOException {
        ExecutableElement executableElement = findConstructorOrFactory(element);
        if (executableElement == null) {
            return;
        }
        boolean isConstructor = executableElement.getKind() == ElementKind.CONSTRUCTOR;
        boolean isBuilder = element.getAnnotation(GsonBuilder.class) != null;

        TypeElement classElement;
        if (isBuilder) {
            TypeElement builderClass;
            if (isConstructor) {
                builderClass = (TypeElement) element.getEnclosingElement();
            } else {
                builderClass = (TypeElement) typeUtils.asElement(executableElement.getReturnType());
            }
            classElement = discoverBuiltClass(element, builderClass);
            if (classElement == null) {
                messager.printMessage(Diagnostic.Kind.ERROR, "Could not find class that builder " + builderClass + " builds. Consider providing it in the @GsonBuilder annotation.", builderClass);
                return;
            }
        } else {
            if (isConstructor) {
                classElement = (TypeElement) executableElement.getEnclosingElement();
            } else {
                classElement = (TypeElement) typeUtils.asElement(executableElement.getReturnType());
            }
        }

        ClassName className = ClassName.get(classElement);
        if (seen.contains(className)) {
            // Don't process the same class more than once.
            return;
        } else {
            seen.add(className);
        }
        ClassName creatorName = ClassName.get((TypeElement) executableElement.getEnclosingElement());
        ClassName typeAdapterClassName = ClassName.get(className.packageName(), PREFIX + join("_", className.simpleNames()));

        Names names = new Names();

        // constructor params
        for (VariableElement param : executableElement.getParameters()) {
            names.addConstructorParam(param);
        }

        // builder params
        TypeElement builderClass = null;
        ExecutableElement buildMethod = null;
        if (isBuilder) {
            TypeMirror builderType;
            if (isConstructor) {
                builderClass = (TypeElement) element.getEnclosingElement();
                builderType = builderClass.asType();
            } else {
                builderType = executableElement.getReturnType();
                builderClass = (TypeElement) typeUtils.asElement(builderType);
            }

            if (builderClass == null) {
                messager.printMessage(Diagnostic.Kind.ERROR, "Cannot find builder " + builderType + " in class " + classElement, element);
                return;
            }
            for (ExecutableElement method : ElementFilter.methodsIn(builderClass.getEnclosedElements())) {
                names.addBuilderParam(builderType, method);
                if (method.getReturnType().equals(classElement.asType()) && method.getParameters().isEmpty()) {
                    buildMethod = method;
                }
            }
            if (buildMethod == null) {
                messager.printMessage(Diagnostic.Kind.ERROR, "Missing build method on " + builderType + " in class " + classElement, builderClass);
                return;
            }
        }

        addFieldsAndGetters(names, classElement);

        try {
            names.finish();
        } catch (ElementException e) {
            e.printMessage(messager);
            return;
        }

        TypeName classType = TypeName.get(classElement.asType());
        List<TypeVariableName> typeVariables = new ArrayList<>();
        if (classType instanceof ParameterizedTypeName) {
            ParameterizedTypeName type = (ParameterizedTypeName) classType;
            for (TypeName typeArgument : type.typeArguments) {
                typeVariables.add(TypeVariableName.get(typeArgument.toString()));
            }
        }

        TypeSpec.Builder spec = TypeSpec.classBuilder(typeAdapterClassName.simpleName())
                .addTypeVariables(typeVariables)
                .addModifiers(Modifier.PUBLIC)
                .superclass(ParameterizedTypeName.get(TYPE_ADAPTER, classType));

        // TypeAdapters
        for (Name name : names.names()) {
            TypeName typeName = TypeName.get(name.getType());
            TypeName typeAdapterType = ParameterizedTypeName.get(TYPE_ADAPTER, typeName.box());
            spec.addField(FieldSpec.builder(typeAdapterType, TYPE_ADAPTER_PREFIX + name.getName())
                    .addModifiers(Modifier.PRIVATE, Modifier.FINAL)
                    .build());
        }

        // Test_TypeAdapter(Gson gson, TypeToken<Test> typeToken)
        {
            MethodSpec.Builder constructor = MethodSpec.constructorBuilder()
                    .addModifiers(Modifier.PUBLIC)
                    .addParameter(GSON, "gson")
                    .addParameter(ParameterizedTypeName.get(TYPE_TOKEN, classType), "typeToken");
            for (Name<?> name : names.names()) {
                String typeAdapterName = TYPE_ADAPTER_PREFIX + name.getName();
                DeclaredType typeAdapterClass = findTypeAdapterClass(name.annotations);
                CodeBlock.Builder block = CodeBlock.builder()
                        .add("this.$L = ", typeAdapterName);
                if (typeAdapterClass != null) {
                    if (isInstance(typeAdapterClass, TYPE_ADAPTER.toString())) {
                        block.add("new $T(", typeAdapterClass);
                    } else if (isInstance(typeAdapterClass, TYPE_ADAPTER_FACTORY.toString())) {
                        block.add("new $T().create(gson, ", typeAdapterClass);
                        appendFieldTypeToken(block, name, typeVariables, /*allowClassType=*/false);
                    } else {
                        messager.printMessage(Diagnostic.Kind.ERROR, "@JsonAdapter value must by TypeAdapter or TypeAdapterFactory reference.", name.element);
                    }
                } else {
                    block.add("gson.getAdapter(");
                    appendFieldTypeToken(block, name, typeVariables, /*allowClassType=*/true);
                }
                block.add(");\n");
                constructor.addCode(block.build());
            }
            spec.addMethod(constructor.build());
        }

        // @Override public void write(JsonWriter out, T value) throws IOException
        {
            CodeBlock.Builder code = CodeBlock.builder();
            code.addStatement("out.beginObject()");
            for (Name name : names.fields()) {
                code.addStatement("out.name($S)", name.getSerializeName())
                        .addStatement("$L.write(out, value.$L)", TYPE_ADAPTER_PREFIX + name.getName(), name.getCallableName());
            }
            for (Name name : names.getters()) {
                code.addStatement("out.name($S)", name.getSerializeName())
                        .addStatement("$L.write(out, value.$L())", TYPE_ADAPTER_PREFIX + name.getName(), name.getCallableName());
            }
            code.addStatement("out.endObject()");

            spec.addMethod(MethodSpec.methodBuilder("write")
                    .addAnnotation(Override.class)
                    .addModifiers(Modifier.PUBLIC)
                    .addParameter(JSON_WRITER, "out")
                    .addParameter(classType, "value")
                    .addException(IOException.class)
                    .addCode(code.build())
                    .build());
        }

        // @Override public T read(JsonReader in) throws IOException
        {
            Iterable<Name> params = names.params();
            CodeBlock.Builder code = CodeBlock.builder();
            boolean isEmpty = true;
            for (Name name : params) {
                isEmpty = false;
                code.addStatement("$T $L = $L", name.getType(), ARG_PREFIX + name.getName(), getDefaultValue(name.getType()));
            }
            if (isEmpty) {
                code.addStatement("in.skipValue()");
            } else {
                code.addStatement("in.beginObject()")
                        .beginControlFlow("while (in.hasNext())")
                        .beginControlFlow("switch (in.nextName())");
                for (Name name : params) {
                    code.add("case $S:\n", name.getSerializeName()).indent();
                    code.addStatement("$L = $L.read(in)", ARG_PREFIX + name.getName(), TYPE_ADAPTER_PREFIX + name.getName())
                            .addStatement("break").unindent();
                }
                code.add("default:\n").indent()
                        .addStatement("in.skipValue()")
                        .unindent();

                code.endControlFlow()
                        .endControlFlow()
                        .addStatement("in.endObject()");
            }

            if (isBuilder) {
                String args = join(", ", names.constructorParams(), TO_ARGS);
                if (isConstructor) {
                    code.add("return new $T($L)", builderClass, args);
                } else {
                    code.add("return $T.$L($L)", creatorName, element.getSimpleName(), args);
                }
                code.add("\n").indent();
                for (Name name : names.builderParams()) {
                    code.add(".$L($L)\n", name.getCallableName(), ARG_PREFIX + name.getName());
                }
                code.add(".$L();\n", buildMethod.getSimpleName()).unindent();
            } else {
                String args = join(", ", params, TO_ARGS);
                if (isConstructor) {
                    code.addStatement("return new $T($L)", classType, args);
                } else {
                    code.addStatement("return $T.$L($L)", creatorName, executableElement.getSimpleName(), args);
                }
            }

            spec.addMethod(MethodSpec.methodBuilder("read")
                    .addAnnotation(Override.class)
                    .addModifiers(Modifier.PUBLIC)
                    .returns(classType)
                    .addParameter(JSON_READER, "in")
                    .addException(IOException.class)
                    .addCode(code.build())
                    .build());
        }

        Writer writer = null;
        boolean threw = true;
        try {
            JavaFileObject jfo = filer.createSourceFile(typeAdapterClassName.toString());
            writer = jfo.openWriter();
            JavaFile javaFile = JavaFile.builder(className.packageName(), spec.build())
                    .skipJavaLangImports(true)
                    .build();
            javaFile.writeTo(writer);
            threw = false;
        } finally {
            try {
                if (writer != null) {
                    writer.close();
                }
            } catch (IOException e) {
                if (!threw) {
                    throw e;
                }
            }
        }
    }

    private ExecutableElement findConstructorOrFactory(Element element) {
        if (element.getKind() == ElementKind.CONSTRUCTOR || element.getKind() == ElementKind.METHOD) {
            return (ExecutableElement) element;
        }
        ExecutableElement noArgConstructor = null;
        List<ExecutableElement> constructors = ElementFilter.constructorsIn(element.getEnclosedElements());
        if (constructors.size() == 1) {
            ExecutableElement constructor = constructors.get(0);
            if (constructor.getParameters().isEmpty()) {
                noArgConstructor = constructor;
                constructors.remove(0);
            }
        }
        for (ExecutableElement method : ElementFilter.methodsIn(element.getEnclosedElements())) {
            Set<Modifier> modifiers = method.getModifiers();
            if (modifiers.contains(Modifier.STATIC)
                    && !modifiers.contains(Modifier.PRIVATE)
                    && method.getReturnType().equals(element.asType())) {
                constructors.add(method);
            }
        }
        if (constructors.isEmpty() && noArgConstructor != null) {
            constructors.add(noArgConstructor);
        }
        if (constructors.size() == 1) {
            return constructors.get(0);
        } else {
            StringBuilder message = new StringBuilder("More than one constructor or factory method found. You should annotate the specific constructor of factory method instead of the class.\n");
            for (ExecutableElement constructor : constructors) {
                message.append(constructor).append("\n");
            }
            messager.printMessage(Diagnostic.Kind.ERROR, message.toString(), element);
            return null;
        }
    }

    private void addFieldsAndGetters(Names names, TypeElement classElement) {
        // getters
        for (ExecutableElement method : ElementFilter.methodsIn(classElement.getEnclosedElements())) {
            names.addGetter(classElement, method);
        }

        // fields
        for (VariableElement field : ElementFilter.fieldsIn(classElement.getEnclosedElements())) {
            names.addField(field);
        }

        for (TypeMirror superInterface : classElement.getInterfaces()) {
            addFieldsAndGetters(names, (TypeElement) typeUtils.asElement(superInterface));
        }

        TypeMirror superclass = classElement.getSuperclass();
        if (superclass.getKind() != TypeKind.NONE && !superclass.toString().equals("java.lang.Object")) {
            addFieldsAndGetters(names, (TypeElement) typeUtils.asElement(classElement.getSuperclass()));
        }
    }

    private void appendFieldTypeToken(CodeBlock.Builder block, Name<?> name, List<TypeVariableName> typeVariables, boolean allowClassType) {
        TypeMirror type = name.getType();
        TypeName typeName = TypeName.get(type);

        if (isComplexType(type)) {
            TypeName typeTokenType = ParameterizedTypeName.get(TYPE_TOKEN, typeName);
            List<? extends TypeMirror> typeParams = getGenericTypes(type);
            if (typeParams.isEmpty()) {
                block.add("new $T() {}", typeTokenType);
            } else {
                block.add("($T) $T.getParameterized($T.class, ", typeTokenType, TYPE_TOKEN, typeUtils.erasure(type));
                for (Iterator<? extends TypeMirror> iterator = typeParams.iterator(); iterator.hasNext(); ) {
                    TypeMirror typeParam = iterator.next();
                    int typeIndex = typeVariables.indexOf(TypeVariableName.get(typeParam.toString()));
                    block.add("(($T)typeToken.getType()).getActualTypeArguments()[$L]", ParameterizedType.class, typeIndex);
                    if (iterator.hasNext()) {
                        block.add(", ");
                    }
                }
                block.add(")");
            }
        } else if (isGenericType(type)) {
            TypeName typeTokenType = ParameterizedTypeName.get(TYPE_TOKEN, typeName);
            int typeIndex = typeVariables.indexOf(TypeVariableName.get(name.getType().toString()));
            block.add("($T) $T.get((($T)typeToken.getType()).getActualTypeArguments()[$L])",
                    typeTokenType, TYPE_TOKEN, ParameterizedType.class, typeIndex);
        } else {
            if (allowClassType) {
                block.add("$T.class", typeName);
            } else {
                block.add("TypeToken.get($T.class)", typeName);
            }
        }
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        return new LinkedHashSet<String>(Arrays.asList(
                GsonConstructor.class.getCanonicalName(),
                GsonBuilder.class.getCanonicalName()
        ));
    }

    private static String getDefaultValue(TypeMirror type) {
        switch (type.getKind()) {
            case BYTE:
            case SHORT:
            case INT:
            case LONG:
            case FLOAT:
            case CHAR:
            case DOUBLE:
                return "0";
            case BOOLEAN:
                return "false";
            default:
                return "null";
        }
    }

    private TypeElement discoverBuiltClass(Element annotaded, TypeElement builderClass) {
        // First check to see if the annotation tells us.
        {
            String builtClass = null;
            for (AnnotationMirror annotationMirror : annotaded.getAnnotationMirrors()) {
                if (annotationMirror.getAnnotationType().toString().equals(GsonBuilder.class.getName())) {
                    if (!annotationMirror.getElementValues().isEmpty()) {
                        AnnotationValue value = annotationMirror.getElementValues().values().iterator().next();
                        builtClass = value.getValue().toString();
                    }
                }
            }
            if (builtClass != null) {
                for (ExecutableElement method : ElementFilter.methodsIn(builderClass.getEnclosedElements())) {
                    if (method.getReturnType().toString().equals(builtClass) && method.getParameters().isEmpty()) {
                        return (TypeElement) typeUtils.asElement(method.getReturnType());
                    }
                }
            }
        }
        // Ok, maybe there is just one possible builder method.
        {
            ExecutableElement candidate = null;
            boolean foundMultipleCandidates = false;
            boolean isCandidateReasonableBuilderMethodName = false;
            for (ExecutableElement method : ElementFilter.methodsIn(builderClass.getEnclosedElements())) {
                if (isPossibleBuilderMethod(method, builderClass)) {
                    if (candidate == null) {
                        candidate = method;
                    } else {
                        // Multiple possible methods, keep the one with a reasonable builder name if
                        // possible.
                        foundMultipleCandidates = true;
                        isCandidateReasonableBuilderMethodName =
                                isCandidateReasonableBuilderMethodName || isReasonableBuilderMethodName(candidate);
                        if (isCandidateReasonableBuilderMethodName) {
                            if (isReasonableBuilderMethodName(method)) {
                                // both reasonable, too ambiguous.
                                candidate = null;
                                break;
                            }
                        } else {
                            candidate = method;
                        }
                    }
                }
            }
            if (candidate != null && (!foundMultipleCandidates || isCandidateReasonableBuilderMethodName)) {
                return (TypeElement) typeUtils.asElement(candidate.getReturnType());
            }
        }
        // Last try, check to see if the immediate parent class makes sense.
        {
            Element candidate = builderClass.getEnclosingElement();
            if (candidate.getKind() == ElementKind.CLASS) {
                for (ExecutableElement method : ElementFilter.methodsIn(builderClass.getEnclosedElements())) {
                    if (method.getReturnType().equals(candidate.asType()) && method.getParameters().isEmpty()) {
                        return (TypeElement) candidate;
                    }
                }
            }
        }
        // Well, I give up.
        return null;
    }

    /**
     * A possible builder method has no parameters and a return type of the class we want to
     * construct. Therefore, the return type is not going to be void, primitive, or a platform
     * class.
     */
    private boolean isPossibleBuilderMethod(ExecutableElement method, TypeElement builderClass) {
        if (!method.getParameters().isEmpty()) {
            return false;
        }
        TypeMirror returnType = method.getReturnType();
        if (returnType.getKind() == TypeKind.VOID) {
            return false;
        }
        if (returnType.getKind().isPrimitive()) {
            return false;
        }
        if (returnType.equals(builderClass.asType())) {
            return false;
        }
        String returnTypeName = returnType.toString();
        if (returnTypeName.startsWith("java.")
                || returnTypeName.startsWith("javax.")
                || returnTypeName.startsWith("android.")) {
            return false;
        }
        return true;
    }

    private boolean isReasonableBuilderMethodName(ExecutableElement method) {
        String methodName = method.getSimpleName().toString().toLowerCase(Locale.US);
        return methodName.startsWith("build") || methodName.startsWith("create");
    }

    private DeclaredType findTypeAdapterClass(List<? extends AnnotationMirror> annotations) {
        for (AnnotationMirror annotation : annotations) {
            String typeName = annotation.getAnnotationType().toString();
            if (typeName.equals(JSON_ADAPTER.toString()) || typeName.equals(JSON_ADAPTER_METHOD.toString())) {
                Map<? extends ExecutableElement, ? extends AnnotationValue> elements = annotation.getElementValues();
                if (!elements.isEmpty()) {
                    AnnotationValue value = elements.values().iterator().next();
                    return (DeclaredType) value.getValue();
                }
            }
        }
        return null;
    }

    private boolean isComplexType(TypeMirror type) {
        Element element = typeUtils.asElement(type);
        if (!(element instanceof TypeElement)) {
            return false;
        }
        TypeElement typeElement = (TypeElement) element;
        return !typeElement.getTypeParameters().isEmpty();
    }

    private boolean isGenericType(TypeMirror type) {
        return type.getKind() == TypeKind.TYPEVAR;
    }

    private List<? extends TypeMirror> getGenericTypes(TypeMirror type) {
        DeclaredType declaredType = asDeclaredType(type);
        if (declaredType == null) {
            return Collections.emptyList();
        }
        ArrayList<TypeMirror> result = new ArrayList<>();
        for (TypeMirror argType : declaredType.getTypeArguments()) {
            if (argType.getKind() == TypeKind.TYPEVAR) {
                result.add(argType);
            }
        }
        return result;
    }

    private static DeclaredType asDeclaredType(TypeMirror type) {
        return type.accept(new SimpleTypeVisitor6<DeclaredType, Object>() {
            @Override
            public DeclaredType visitDeclared(DeclaredType t, Object o) {
                return t;
            }
        }, null);
    }

    private boolean isInstance(DeclaredType type, String parentClassName) {
        if (type == null) {
            return false;
        }
        TypeElement element = (TypeElement) type.asElement();
        for (TypeMirror interfaceType : element.getInterfaces()) {
            if (typeUtils.erasure(interfaceType).toString().equals(parentClassName)) {
                return true;
            }
        }
        TypeMirror superclassType = element.getSuperclass();
        if (superclassType != null) {
            if (typeUtils.erasure(superclassType).toString().equals(parentClassName)) {
                return true;
            } else {
                return isInstance(asDeclaredType(superclassType), parentClassName);
            }
        }
        return false;
    }

    private static <T> String join(String sep, Iterable<T> collection) {
        return join(sep, collection, new ToString<T>() {
            @Override
            public String toString(T value) {
                return value.toString();
            }
        });
    }

    private static <T> String join(String sep, Iterable<T> collection, ToString<? super T> toString) {
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

    private interface ToString<T> {
        String toString(T value);
    }

    private static final ToString<Name> TO_ARGS = new ToString<Name>() {
        @Override
        public String toString(Name value) {
            return ARG_PREFIX + value.getName();
        }
    };
}
