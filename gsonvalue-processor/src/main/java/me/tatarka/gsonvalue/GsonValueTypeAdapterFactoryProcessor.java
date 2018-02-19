package me.tatarka.gsonvalue;

import com.squareup.javapoet.*;

import org.omg.CORBA.portable.ValueFactory;

import me.tatarka.gsonvalue.annotations.GsonBuilder;
import me.tatarka.gsonvalue.annotations.GsonConstructor;
import me.tatarka.gsonvalue.annotations.GsonValueTypeAdapterFactory;
import me.tatarka.valueprocessor.ElementException;
import me.tatarka.valueprocessor.ValueCreator;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;

import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

@SupportedSourceVersion(SourceVersion.RELEASE_7)
public class GsonValueTypeAdapterFactoryProcessor extends AbstractProcessor {

    private Messager messager;
    private Types typeUtils;
    private Elements elementUtils;
    private ValueCreator valueCreator;

    @Override
    public synchronized void init(ProcessingEnvironment env) {
        super.init(env);
        messager = env.getMessager();
        typeUtils = env.getTypeUtils();
        elementUtils = env.getElementUtils();
        valueCreator = new ValueCreator(env);
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        Set<TypeElement> elements = new LinkedHashSet<>();
        for (Element element : roundEnv.getElementsAnnotatedWith(GsonConstructor.class)) {
            try {
                elements.add(valueCreator.from(element, false).getElement());
            } catch (ElementException e) {
                // Ignore, should be reported by other processor
            }
        }
        for (Element element : roundEnv.getElementsAnnotatedWith(GsonBuilder.class)) {
            try {
                elements.add(valueCreator.from(element, true).getElement());
            } catch (ElementException e) {
                // Ignore, should be reported by other processor
            }
        }
        Set<? extends Element> adaptorFactories = roundEnv.getElementsAnnotatedWith(GsonValueTypeAdapterFactory.class);
        for (Element adapter : adaptorFactories) {
            if (!adapter.getModifiers().contains(Modifier.ABSTRACT)) {
                messager.printMessage(Diagnostic.Kind.ERROR, "Must be abstract", adapter);
                continue;
            }
            TypeElement type = (TypeElement) adapter; // Safe to cast because this is only applicable on types anyway
            if (!implementsTypeAdapterFactory(type)) {
                messager.printMessage(Diagnostic.Kind.ERROR, "Must implement TypeAdapterFactory", adapter);
                continue;
            }
            ClassName className = ClassName.get(type);
            TypeSpec typeAdapterFactory = createTypeAdapterFactory(adapter, className, elements);
            JavaFile file = JavaFile.builder(className.packageName(), typeAdapterFactory).build();
            try {
                file.writeTo(processingEnv.getFiler());
            } catch (IOException e) {
                messager.printMessage(Diagnostic.Kind.ERROR, "Failed to write TypeAdapterFactory: " + e.getLocalizedMessage());
            }
        }
        return false;
    }

    private TypeSpec createTypeAdapterFactory(Element adapter, ClassName className, Set<TypeElement> elements) {
        TypeSpec.Builder factory = TypeSpec.classBuilder(Prefix.FACTORY_PREFIX + className.simpleName());
        factory.addModifiers(Modifier.FINAL);
        factory.superclass(className);

        ParameterSpec gson = ParameterSpec.builder(GsonClassNames.GSON, "gson").build();
        TypeVariableName t = TypeVariableName.get("T");
        ParameterSpec type = ParameterSpec
                .builder(ParameterizedTypeName.get(GsonClassNames.TYPE_TOKEN, t), "type")
                .build();
        ParameterizedTypeName result = ParameterizedTypeName.get(GsonClassNames.TYPE_ADAPTER, t);
        MethodSpec.Builder create = MethodSpec.methodBuilder("create")
                .addModifiers(Modifier.PUBLIC)
                .addTypeVariable(t)
                .addAnnotation(Override.class)
                .addAnnotation(AnnotationSpec.builder(SuppressWarnings.class)
                        .addMember("value", "\"unchecked\"")
                        .build())
                .addParameters(Arrays.asList(gson, type))
                .returns(result);

        if (elements.isEmpty()) {
            create.addStatement("return null");
        } else {
            MethodSpec.Builder builder = create.beginControlFlow("switch (type.getRawType().getName())");
            for (TypeElement element : elements) {
                factory.addOriginatingElement(element);
                ClassName elementClassName = ClassName.get(element);
                ClassName typeAdapterClassName = ClassName.get(elementClassName.packageName(), Prefix.PREFIX + StringUtils.join("_", elementClassName.simpleNames()));
                builder.addStatement("case $S:\n$>return ($T) new $T(gson, ($T) type)$<", classLiteralName(elementClassName), ParameterizedTypeName.get(GsonClassNames.TYPE_ADAPTER, TypeVariableName.get("T")), typeAdapterClassName, ParameterizedTypeName.get(GsonClassNames.TYPE_TOKEN, elementClassName));
            }
            builder.addStatement("default:\n$>return null$<");
            builder.endControlFlow();
        }

        factory.addMethod(create.build());
        return factory.build();
    }

    private String classLiteralName(ClassName className) {
        return className.packageName() + "." + StringUtils.join("$", className.simpleNames());
    }

    private boolean implementsTypeAdapterFactory(TypeElement type) {
        TypeMirror typeAdapterFactoryType
                = elementUtils.getTypeElement(GsonClassNames.TYPE_ADAPTER_FACTORY.toString()).asType();
        for (TypeMirror iface : type.getInterfaces()) {
            if (typeUtils.isSameType(iface, typeAdapterFactoryType)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        return new LinkedHashSet<>(Arrays.asList(
                GsonConstructor.class.getCanonicalName(),
                GsonBuilder.class.getCanonicalName(),
                GsonValueTypeAdapterFactory.class.getCanonicalName()
        ));
    }
}
