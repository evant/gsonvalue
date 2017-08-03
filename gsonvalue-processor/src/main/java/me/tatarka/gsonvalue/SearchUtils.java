package me.tatarka.gsonvalue;

import me.tatarka.gsonvalue.annotations.GsonBuilder;

import javax.annotation.processing.Messager;
import javax.lang.model.element.*;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;
import java.util.List;
import java.util.Locale;
import java.util.Set;

final class SearchUtils {

    private final Messager messager;
    private final Types typeUtils;

    SearchUtils(Messager messager, Types typeUtils) {
        this.messager = messager;
        this.typeUtils = typeUtils;
    }

    Search forElement(Element element) {
        return new Search(element);
    }

    class Search {
        private final Element element;
        private ExecutableElement cachedExecutableElement;

        private Search(Element element) {
            this.element = element;
        }

        boolean isConstructor() {
            return findConstructorOrFactory().getKind() == ElementKind.CONSTRUCTOR;
        }

        boolean isBuilder() {
            return element.getAnnotation(GsonBuilder.class) != null;
        }

        TypeElement findClass() {
            ExecutableElement executableElement = findConstructorOrFactory();
            if (executableElement == null) {
                return null;
            }

            boolean isConstructor = isConstructor();
            boolean isBuilder = isBuilder();

            TypeElement classElement;
            if (isBuilder) {
                TypeElement builderClass;
                if (isConstructor) {
                    builderClass = (TypeElement) element.getEnclosingElement();
                } else {
                    builderClass = (TypeElement) typeUtils.asElement(cachedExecutableElement.getReturnType());
                }
                classElement = discoverBuiltClass(element, builderClass);
                if (classElement == null) {
                    messager.printMessage(Diagnostic.Kind.ERROR, "Could not find class that builder " + builderClass + " builds. Consider providing it in the @GsonBuilder annotation.", builderClass);
                    return null;
                }
            } else {
                if (isConstructor) {
                    classElement = (TypeElement) executableElement.getEnclosingElement();
                } else {
                    classElement = (TypeElement) typeUtils.asElement(executableElement.getReturnType());
                }
            }
            return classElement;
        }

        ExecutableElement findConstructorOrFactory() {
            if (cachedExecutableElement != null) {
                return cachedExecutableElement;
            }

            if (element.getKind() == ElementKind.CONSTRUCTOR || element.getKind() == ElementKind.METHOD) {
                return (cachedExecutableElement = (ExecutableElement) element);
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
                return (cachedExecutableElement = constructors.get(0));
            } else {
                StringBuilder message = new StringBuilder("More than one constructor or factory method found. You should annotate the specific constructor of factory method instead of the class.\n");
                for (ExecutableElement constructor : constructors) {
                    message.append(constructor).append("\n");
                }
                messager.printMessage(Diagnostic.Kind.ERROR, message.toString(), element);
                return null;
            }
        }

        private TypeElement discoverBuiltClass(Element annotated, TypeElement builderClass) {
            // First check to see if the annotation tells us.
            {
                String builtClass = null;
                for (AnnotationMirror annotationMirror : annotated.getAnnotationMirrors()) {
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
    }

}
