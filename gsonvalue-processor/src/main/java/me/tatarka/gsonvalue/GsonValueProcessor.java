package me.tatarka.gsonvalue;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import com.squareup.javapoet.TypeVariableName;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.SimpleTypeVisitor6;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;

import me.tatarka.gsonvalue.annotations.GsonBuilder;
import me.tatarka.gsonvalue.annotations.GsonConstructor;
import me.tatarka.valueprocessor.ConstructionSource;
import me.tatarka.valueprocessor.ElementException;
import me.tatarka.valueprocessor.Properties;
import me.tatarka.valueprocessor.Property;
import me.tatarka.valueprocessor.Value;
import me.tatarka.valueprocessor.ValueCreator;

@SupportedSourceVersion(SourceVersion.RELEASE_7)
public class GsonValueProcessor extends AbstractProcessor {

    private static final String SERIALIZED_NAME = "com.google.gson.annotations.SerializedName";

    private Messager messager;
    private Filer filer;
    private Types typeUtils;
    private List<ClassName> seen;
    private ValueCreator valueCreator;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        messager = processingEnv.getMessager();
        filer = processingEnv.getFiler();
        typeUtils = processingEnv.getTypeUtils();
        seen = new ArrayList<>();
        valueCreator = new ValueCreator(processingEnv);
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        for (TypeElement annotation : annotations) {
            Set<? extends Element> elements = roundEnv.getElementsAnnotatedWith(annotation);
            for (Element element : elements) {
                if (element.getAnnotation(GsonConstructor.class) != null || element.getAnnotation(GsonBuilder.class) != null) {
                    try {
                        process(element, element.getAnnotation(GsonBuilder.class) != null);
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

    private void process(Element element, boolean isBuilder) throws IOException {
        try {
            Value value = valueCreator.from(element, isBuilder);
            ConstructionSource constructionSource = value.getConstructionSource();
            Properties properties = value.getProperties();

            TypeElement classElement = value.getElement();
            ClassName className = ClassName.get(classElement);
            if (seen.contains(className)) {
                // Don't process the same class more than once.
                return;
            } else {
                seen.add(className);
            }

            ClassName creatorName = ClassName.get((TypeElement) constructionSource.getConstructionElement().getEnclosingElement());
            ClassName typeAdapterClassName = ClassName.get(className.packageName(), Prefix.PREFIX + StringUtils.join("_", className.simpleNames()));
            TypeName classType = TypeName.get(classElement.asType());
            List<TypeVariableName> typeVariables = new ArrayList<>();
            if (classType instanceof ParameterizedTypeName) {
                ParameterizedTypeName type = (ParameterizedTypeName) classType;
                for (TypeName typeArgument : type.typeArguments) {
                    typeVariables.add(TypeVariableName.get(typeArgument.toString()));
                }
            }

            TypeSpec.Builder spec = TypeSpec.classBuilder(typeAdapterClassName.simpleName())
                    .addOriginatingElement(value.getElement())
                    .addTypeVariables(typeVariables)
                    .addModifiers(Modifier.PUBLIC)
                    .superclass(ParameterizedTypeName.get(GsonClassNames.TYPE_ADAPTER, classType));

            // TypeAdapters
            for (Property<?> property : properties) {
                TypeName typeName = TypeName.get(property.getType());
                TypeName typeAdapterType = ParameterizedTypeName.get(GsonClassNames.TYPE_ADAPTER, typeName.box());
                spec.addField(FieldSpec.builder(typeAdapterType, Prefix.TYPE_ADAPTER_PREFIX + property.getName())
                        .addModifiers(Modifier.PRIVATE, Modifier.FINAL)
                        .build());
            }

            // Test_TypeAdapter(Gson gson, TypeToken<Test> typeToken)
            {
                MethodSpec.Builder constructor = MethodSpec.constructorBuilder()
                        .addModifiers(Modifier.PUBLIC)
                        .addParameter(GsonClassNames.GSON, "gson")
                        .addParameter(ParameterizedTypeName.get(GsonClassNames.TYPE_TOKEN, classType), "typeToken");
                for (Property<?> property : properties) {
                    String typeAdapterName = Prefix.TYPE_ADAPTER_PREFIX + property.getName();
                    DeclaredType typeAdapterClass = findTypeAdapterClass(property.getAnnotations());
                    CodeBlock.Builder block = CodeBlock.builder()
                            .add("this.$L = ", typeAdapterName);
                    if (typeAdapterClass != null) {
                        if (isInstance(typeAdapterClass, GsonClassNames.TYPE_ADAPTER.toString())) {
                            block.add("new $T(", typeAdapterClass);
                        } else if (isInstance(typeAdapterClass, GsonClassNames.TYPE_ADAPTER_FACTORY.toString())) {
                            block.add("new $T().create(gson, ", typeAdapterClass);
                            appendFieldTypeToken(block, property, typeVariables, /*allowClassType=*/false);
                        } else {
                            messager.printMessage(Diagnostic.Kind.ERROR, "@JsonAdapter value must by TypeAdapter or TypeAdapterFactory reference.", property.getElement());
                        }
                    } else {
                        block.add("gson.getAdapter(");
                        appendFieldTypeToken(block, property, typeVariables, /*allowClassType=*/true);
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
                for (Property.Accessor<?> accessor: properties.getAccessors()) {
                    code.addStatement("out.name($S)", getSerializedName(accessor));
                    String writeStatement;
                    if (accessor instanceof Property.Field) {
                        writeStatement = "$L.write(out, value.$L)";
                    } else if (accessor instanceof Property.Getter) {
                        writeStatement = "$L.write(out, value.$L())";
                    } else {
                        throw new AssertionError("unknown accessor: " + accessor);
                    }
                    code.addStatement(writeStatement, Prefix.TYPE_ADAPTER_PREFIX + accessor.getName(), accessor.getCallableName());
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

                Iterable<Property.Param> params = properties.getParams();
                boolean isEmpty = true;
                for (Property.Param param : params) {
                    isEmpty = false;
                    code.addStatement("$T $L = $L", param.getType(), Prefix.ARG_PREFIX + param.getName(), getDefaultValue(param.getType()));
                }
                if (isEmpty) {
                    code.addStatement("in.skipValue()");
                } else {
                    code.addStatement("in.beginObject()")
                            .beginControlFlow("while (in.hasNext())")
                            .beginControlFlow("switch (in.nextName())");
                    for (Property.Param param : params) {
                        code.add("case $S:\n", getSerializedName(param)).indent();
                        code.addStatement("$L = $L.read(in)", Prefix.ARG_PREFIX + param.getName(), Prefix.TYPE_ADAPTER_PREFIX + param.getName())
                                .addStatement("break").unindent();
                    }
                    code.add("default:\n").indent()
                            .addStatement("in.skipValue()")
                            .unindent();

                    code.endControlFlow()
                            .endControlFlow()
                            .addStatement("in.endObject()");
                }

                if (constructionSource instanceof ConstructionSource.Builder) {
                    String args = StringUtils.join(", ", properties.getConstructorParams(), TO_ARGS);
                    if (constructionSource.isConstructor()) {
                        code.add("return new $T($L)", ((ConstructionSource.Builder) constructionSource).getBuilderClass(), args);
                    } else {
                        code.add("return $T.$L($L)", creatorName, element.getSimpleName(), args);
                    }
                    code.add("\n").indent();
                    for (Property.BuilderParam param : properties.getBuilderParams()) {
                        code.add(".$L($L)\n", param.getCallableName(), Prefix.ARG_PREFIX + param.getName());
                    }
                    code.add(".$L();\n", ((ConstructionSource.Builder) constructionSource).getBuildMethod().getSimpleName()).unindent();
                } else {
                    String args = StringUtils.join(", ", params, TO_ARGS);
                    if (constructionSource.isConstructor()) {
                        code.addStatement("return new $T($L)", classType, args);
                    } else {
                        code.addStatement("return $T.$L($L)", creatorName, constructionSource.getConstructionElement().getSimpleName(), args);
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

            JavaFile javaFile = JavaFile.builder(className.packageName(), spec.build())
                    .skipJavaLangImports(true)
                    .build();
            javaFile.writeTo(filer);
        } catch (ElementException e) {
            e.printMessage(messager);
        }
    }

    private void appendFieldTypeToken(CodeBlock.Builder block, Property<?> property, List<TypeVariableName> typeVariables, boolean allowClassType) {
        TypeMirror type = property.getType();
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
            int typeIndex = typeVariables.indexOf(TypeVariableName.get(property.getType().toString()));
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

    private static String getSerializedName(Property<?> property) {
        for (AnnotationMirror annotationMirror : property.getAnnotations()) {
            if (annotationMirror.getAnnotationType().asElement().toString().equals(SERIALIZED_NAME)) {
                return (String) annotationMirror.getElementValues().values().iterator().next().getValue();
            }
        }
        return property.getName();
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

    private static final StringUtils.ToString<Property<?>> TO_ARGS = new StringUtils.ToString<Property<?>>() {
        @Override
        public String toString(Property<?> value) {
            return Prefix.ARG_PREFIX + value.getName();
        }
    };
}
