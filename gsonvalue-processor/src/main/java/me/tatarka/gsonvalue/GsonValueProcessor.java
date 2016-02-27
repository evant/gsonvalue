package me.tatarka.gsonvalue;

import com.squareup.javapoet.*;
import me.tatarka.gsonvalue.annotations.GsonBuilder;
import me.tatarka.gsonvalue.annotations.GsonConstructor;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.*;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.util.*;

@SupportedSourceVersion(SourceVersion.RELEASE_7)
public class GsonValueProcessor extends AbstractProcessor {
    private static final String PREFIX = "ValueTypeAdapter_";
    private static final String ARG_PREFIX = "_";
    private static final String TYPE_ADAPTER_PREFIX = "adapter_";
    private static final ClassName GSON = ClassName.get("com.google.gson", "Gson");
    private static final ClassName TYPE_ADAPTER = ClassName.get("com.google.gson", "TypeAdapter");
    private static final ClassName JSON_WRITER = ClassName.get("com.google.gson.stream", "JsonWriter");
    private static final ClassName JSON_READER = ClassName.get("com.google.gson.stream", "JsonReader");
    private static final ClassName TYPE_TOKEN = ClassName.get("com.google.gson.reflect", "TypeToken");

    private Messager messager;
    private Filer filer;
    private Elements elementUtils;
    private Types typeUtils;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        messager = processingEnv.getMessager();
        filer = processingEnv.getFiler();
        elementUtils = processingEnv.getElementUtils();
        typeUtils = processingEnv.getTypeUtils();
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        for (TypeElement annotation : annotations) {
            Set<? extends Element> elements = roundEnv.getElementsAnnotatedWith(annotation);
            for (Element element : elements) {
                try {
                    process((ExecutableElement) element);
                } catch (IOException e) {
                    StringWriter stringWriter = new StringWriter();
                    e.printStackTrace(new PrintWriter(stringWriter));
                    messager.printMessage(Diagnostic.Kind.ERROR, "GsonValue threw an exception: " + stringWriter.toString(), element);
                }
            }
        }
        return false;
    }

    private void process(ExecutableElement element) throws IOException {
        TypeElement classElement = (TypeElement) element.getEnclosingElement();

        boolean isConstructor = element.getKind() == ElementKind.CONSTRUCTOR;
        boolean isBuilder = element.getAnnotation(GsonBuilder.class) != null;

        if (isConstructor && isBuilder) {
            // Annotation is on builder class, get real class
            classElement = (TypeElement) classElement.getEnclosingElement();
        }

        ClassName className = ClassName.get(classElement);
        ClassName typeAdapterClassName = ClassName.get(className.packageName(), PREFIX + join("_", className.simpleNames()));

        Names names = new Names();

        // constructor params
        for (VariableElement param : element.getParameters()) {
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
                builderType = element.getReturnType();
                builderClass = findInnerClass(classElement, builderType);
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

        Iterable<Name> params = names.params();

        TypeSpec.Builder spec = TypeSpec.classBuilder(typeAdapterClassName.simpleName())
                .addModifiers(Modifier.PUBLIC)
                .superclass(ParameterizedTypeName.get(TYPE_ADAPTER, className))
                .addField(GSON, "gson", Modifier.PRIVATE, Modifier.FINAL);

        // TypeAdapters
        for (Name param : params) {
            TypeName typeName = TypeName.get(param.getType());
            TypeName typeAdapterType = ParameterizedTypeName.get(TYPE_ADAPTER, typeName.box());
            spec.addField(FieldSpec.builder(typeAdapterType, TYPE_ADAPTER_PREFIX + param.getName())
                    .addModifiers(Modifier.PRIVATE, Modifier.FINAL)
                    .build());
        }

        // Test_TypeAdapter(Gson gson)
        {
            MethodSpec.Builder constructor = MethodSpec.constructorBuilder()
                    .addModifiers(Modifier.PUBLIC)
                    .addParameter(GSON, "gson")
                    .addStatement("this.gson = gson");
            for (Name param : params) {
                TypeName typeName = TypeName.get(param.getType());
                String typeAdapterName = TYPE_ADAPTER_PREFIX + param.getName();

                if (isComplexType(param.getType())) {
                    TypeName typeTokenType = ParameterizedTypeName.get(TYPE_TOKEN, typeName);
                    constructor.addStatement("this.$L = gson.getAdapter(new $T() {})", typeAdapterName, typeTokenType);
                } else {
                    constructor.addStatement("this.$L = gson.getAdapter($T.class)", typeAdapterName, typeName);
                }
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
                    .addParameter(className, "value")
                    .addException(IOException.class)
                    .addCode(code.build())
                    .build());
        }

        // @Override public T read(JsonReader in) throws IOException
        {
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
                code.endControlFlow()
                        .endControlFlow()
                        .addStatement("in.endObject()");
            }

            if (isBuilder) {
                String args = join(", ", names.constructorParams(), TO_ARGS);
                if (isConstructor) {
                    code.add("return new $T($L)", builderClass, args);
                } else {
                    code.add("return $T.$L($L)", className, element.getSimpleName(), args);
                }
                code.add("\n").indent();
                for (Name name : names.builderParams()) {
                    code.add(".$L($L)\n", name.getCallableName(), ARG_PREFIX + name.getName());
                }
                code.add(".$L();\n", buildMethod.getSimpleName()).unindent();
            } else {
                String args = join(", ", params, TO_ARGS);
                if (isConstructor) {
                    code.addStatement("return new $T($L)", className, args);
                } else {
                    code.addStatement("return $T.$L($L)", className, element.getSimpleName(), args);
                }
            }

            spec.addMethod(MethodSpec.methodBuilder("read")
                    .addAnnotation(Override.class)
                    .addModifiers(Modifier.PUBLIC)
                    .returns(className)
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

    private void addFieldsAndGetters(Names names, TypeElement classElement) {
        // getters
        for (ExecutableElement method : ElementFilter.methodsIn(classElement.getEnclosedElements())) {
            names.addGetter(method);
        }

        // fields
        for (VariableElement field : ElementFilter.fieldsIn(classElement.getEnclosedElements())) {
            names.addField(field);
        }

        for (TypeMirror superInterface : classElement.getInterfaces()) {
            addFieldsAndGetters(names, (TypeElement) typeUtils.asElement(superInterface));
        }

        if (classElement.getSuperclass().getKind() != TypeKind.NONE) {
            addFieldsAndGetters(names, (TypeElement) typeUtils.asElement(classElement.getSuperclass()));
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

    private static TypeElement findInnerClass(TypeElement classElement, TypeMirror type) {
        for (TypeElement element : ElementFilter.typesIn(classElement.getEnclosedElements())) {
            if (element.asType().equals(type)) {
                return element;
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
