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

    private Messager messager;
    private Filer filer;
    private Types typeUtils;
    private List<ClassName> seen;
    private SearchUtils searchUtils;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        messager = processingEnv.getMessager();
        filer = processingEnv.getFiler();
        typeUtils = processingEnv.getTypeUtils();
        seen = new ArrayList<>();
        searchUtils = new SearchUtils(messager, typeUtils);
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
        SearchUtils.Search search = searchUtils.forElement(element);

        ExecutableElement executableElement = search.findConstructorOrFactory();
        if (executableElement == null) {
            return;
        }

        boolean isConstructor = search.isConstructor();
        boolean isBuilder = search.isBuilder();
        TypeElement classElement = search.findClass();

        ClassName className = ClassName.get(classElement);
        if (seen.contains(className)) {
            // Don't process the same class more than once.
            return;
        } else {
            seen.add(className);
        }
        ClassName creatorName = ClassName.get((TypeElement) executableElement.getEnclosingElement());
        ClassName typeAdapterClassName = ClassName.get(className.packageName(), Prefix.PREFIX + StringUtils.join("_", className.simpleNames()));

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
                .superclass(ParameterizedTypeName.get(GsonClassNames.TYPE_ADAPTER, classType));

        // TypeAdapters
        for (Name name : names.names()) {
            TypeName typeName = TypeName.get(name.getType());
            TypeName typeAdapterType = ParameterizedTypeName.get(GsonClassNames.TYPE_ADAPTER, typeName.box());
            spec.addField(FieldSpec.builder(typeAdapterType, Prefix.TYPE_ADAPTER_PREFIX + name.getName())
                    .addModifiers(Modifier.PRIVATE, Modifier.FINAL)
                    .build());
        }

        // Test_TypeAdapter(Gson gson, TypeToken<Test> typeToken)
        {
            MethodSpec.Builder constructor = MethodSpec.constructorBuilder()
                    .addModifiers(Modifier.PUBLIC)
                    .addParameter(GsonClassNames.GSON, "gson")
                    .addParameter(ParameterizedTypeName.get(GsonClassNames.TYPE_TOKEN, classType), "typeToken");
            for (Name<?> name : names.names()) {
                String typeAdapterName = Prefix.TYPE_ADAPTER_PREFIX + name.getName();
                DeclaredType typeAdapterClass = findTypeAdapterClass(name.annotations);
                CodeBlock.Builder block = CodeBlock.builder()
                        .add("this.$L = ", typeAdapterName);
                if (typeAdapterClass != null) {
                    if (isInstance(typeAdapterClass, GsonClassNames.TYPE_ADAPTER.toString())) {
                        block.add("new $T(", typeAdapterClass);
                    } else if (isInstance(typeAdapterClass, GsonClassNames.TYPE_ADAPTER_FACTORY.toString())) {
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
            code.beginControlFlow("if (value == null)")
                    .addStatement("out.nullValue()")
                    .addStatement("return")
                    .endControlFlow();

            code.addStatement("out.beginObject()");
            for (Name name : names.fields()) {
                code.addStatement("out.name($S)", name.getSerializeName())
                        .addStatement("$L.write(out, value.$L)", Prefix.TYPE_ADAPTER_PREFIX + name.getName(), name.getCallableName());
            }
            for (Name name : names.getters()) {
                code.addStatement("out.name($S)", name.getSerializeName())
                        .addStatement("$L.write(out, value.$L())", Prefix.TYPE_ADAPTER_PREFIX + name.getName(), name.getCallableName());
            }
            code.addStatement("out.endObject()");

            spec.addMethod(MethodSpec.methodBuilder("write")
                    .addAnnotation(Override.class)
                    .addModifiers(Modifier.PUBLIC)
                    .addParameter(GsonClassNames.JSON_WRITER, "out")
                    .addParameter(classType, "value")
                    .addException(IOException.class)
                    .addCode(code.build())
                    .build());
        }

        // @Override public T read(JsonReader in) throws IOException
        {
            CodeBlock.Builder code = CodeBlock.builder();
            code.beginControlFlow("if (in.peek() == $T.NULL)", GsonClassNames.JSON_TOKEN)
                    .addStatement("in.nextNull()")
                    .addStatement("return null")
                    .endControlFlow();

            Iterable<Name> params = names.params();
            boolean isEmpty = true;
            for (Name name : params) {
                isEmpty = false;
                code.addStatement("$T $L = $L", name.getType(), Prefix.ARG_PREFIX + name.getName(), getDefaultValue(name.getType()));
            }
            if (isEmpty) {
                code.addStatement("in.skipValue()");
            } else {
                code.addStatement("in.beginObject()")
                        .beginControlFlow("while (in.hasNext())")
                        .beginControlFlow("switch (in.nextName())");
                for (Name name : params) {
                    code.add("case $S:\n", name.getSerializeName()).indent();
                    code.addStatement("$L = $L.read(in)", Prefix.ARG_PREFIX + name.getName(), Prefix.TYPE_ADAPTER_PREFIX + name.getName())
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
                String args = StringUtils.join(", ", names.constructorParams(), TO_ARGS);
                if (isConstructor) {
                    code.add("return new $T($L)", builderClass, args);
                } else {
                    code.add("return $T.$L($L)", creatorName, element.getSimpleName(), args);
                }
                code.add("\n").indent();
                for (Name name : names.builderParams()) {
                    code.add(".$L($L)\n", name.getCallableName(), Prefix.ARG_PREFIX + name.getName());
                }
                code.add(".$L();\n", buildMethod.getSimpleName()).unindent();
            } else {
                String args = StringUtils.join(", ", params, TO_ARGS);
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
                    .addParameter(GsonClassNames.JSON_READER, "in")
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
            TypeName typeTokenType = ParameterizedTypeName.get(GsonClassNames.TYPE_TOKEN, typeName);
            List<? extends TypeMirror> typeParams = getGenericTypes(type);
            if (typeParams.isEmpty()) {
                block.add("new $T() {}", typeTokenType);
            } else {
                block.add("($T) $T.getParameterized($T.class, ", typeTokenType, GsonClassNames.TYPE_TOKEN, typeUtils.erasure(type));
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
            TypeName typeTokenType = ParameterizedTypeName.get(GsonClassNames.TYPE_TOKEN, typeName);
            int typeIndex = typeVariables.indexOf(TypeVariableName.get(name.getType().toString()));
            block.add("($T) $T.get((($T)typeToken.getType()).getActualTypeArguments()[$L])",
                    typeTokenType, GsonClassNames.TYPE_TOKEN, ParameterizedType.class, typeIndex);
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
        return new LinkedHashSet<>(Arrays.asList(
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

    private DeclaredType findTypeAdapterClass(List<? extends AnnotationMirror> annotations) {
        for (AnnotationMirror annotation : annotations) {
            String typeName = annotation.getAnnotationType().toString();
            if (typeName.equals(GsonClassNames.JSON_ADAPTER.toString()) || typeName.equals(GsonClassNames.JSON_ADAPTER_METHOD.toString())) {
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

    private static final StringUtils.ToString<Name> TO_ARGS = new StringUtils.ToString<Name>() {
        @Override
        public String toString(Name value) {
            return Prefix.ARG_PREFIX + value.getName();
        }
    };
}
