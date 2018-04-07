package me.tatarka.gsonvalue;

import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeSpec;
import com.squareup.javapoet.TypeVariableName;

import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;

import me.tatarka.gsonvalue.annotations.GsonValue;
import me.tatarka.gsonvalue.annotations.GsonValueTypeAdapterFactory;

@SupportedSourceVersion(SourceVersion.RELEASE_7)
public class GsonValueTypeAdapterFactoryProcessor extends AbstractProcessor {

    private Messager messager;
    private Types typeUtils;
    private Elements elementUtils;

    @Override
    public synchronized void init(ProcessingEnvironment env) {
        super.init(env);
        messager = env.getMessager();
        typeUtils = env.getTypeUtils();
        elementUtils = env.getElementUtils();
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        Set<TypeElement> elements = new LinkedHashSet<>();
        for (Element element : roundEnv.getElementsAnnotatedWith(GsonValue.class)) {
            if (element instanceof TypeElement) {
                elements.add((TypeElement) element);
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
            MethodSpec.Builder builder = create.addStatement("Class<?> clazz = type.getRawType()");
            {
                TypeElement first = elements.iterator().next();
                factory.addOriginatingElement(first);
                ClassName elementClassName = ClassName.get(first);
                ClassName typeAdapterClassName = ClassName.get(elementClassName.packageName(),
                        Prefix.PREFIX + StringUtils.join("_", elementClassName.simpleNames()));

                builder.beginControlFlow("if (clazz == $N)", classLiteralName(elementClassName)).
                        addStatement("return ($T) new $T(gson, ($T) type)",
                                ParameterizedTypeName.get(GsonClassNames.TYPE_ADAPTER, TypeVariableName.get("T")),
                                typeAdapterClassName,
                                ParameterizedTypeName.get(GsonClassNames.TYPE_TOKEN, elementClassName));
                elements.remove(first);
            }
            for (TypeElement element : elements) {
                factory.addOriginatingElement(element);
                ClassName elementClassName = ClassName.get(element);
                ClassName typeAdapterClassName = ClassName.get(elementClassName.packageName(), Prefix.PREFIX + StringUtils.join("_", elementClassName.simpleNames()));
                builder.nextControlFlow("else if (clazz == $N)", classLiteralName(elementClassName)).
                        addStatement("return ($T) new $T(gson, ($T) type)",
                                ParameterizedTypeName.get(GsonClassNames.TYPE_ADAPTER, TypeVariableName.get("T")),
                                typeAdapterClassName, ParameterizedTypeName.get(GsonClassNames.TYPE_TOKEN, elementClassName));
            }
            builder.nextControlFlow("else")
                    .addStatement("return null");
            builder.endControlFlow();
        }

        factory.addMethod(create.build());
        return factory.build();
    }

    private String classLiteralName(ClassName className) {
        return className.packageName() + "." + StringUtils.join(".", className.simpleNames()) + ".class";
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
                GsonValue.class.getCanonicalName(),
                GsonValueTypeAdapterFactory.class.getCanonicalName()
        ));
    }
}
