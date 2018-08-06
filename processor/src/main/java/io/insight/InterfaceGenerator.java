package io.insight;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;

import java.io.IOException;
import java.io.Writer;
import java.util.List;
import java.util.stream.Collectors;

import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.util.ElementFilter;

class InterfaceGenerator {

    private final String packageName;

    private final String originClassName;

    private final TypeElement element;

    InterfaceGenerator(final String packageName, final TypeElement element) {

        this.packageName = packageName;
        this.originClassName = element.getSimpleName().toString();
        this.element = element;
    }

    void writeTo(final Writer writer) throws IOException {
        JavaFile javaFile = null;
        try {
            javaFile = JavaFile.builder(packageName, build()).build();
            javaFile.writeTo(writer);
        } catch (final Exception e) {
            e.printStackTrace();
        }
    }

    private TypeSpec build() {
        final TypeSpec.Builder iface = TypeSpec.interfaceBuilder(className()).addModifiers(Modifier.PUBLIC);

        ElementFilter.methodsIn(element.getEnclosedElements()).forEach(me -> {
            final TypeName returnType = TypeName.get(me.getReturnType());
            final String methodName = me.getSimpleName().toString();
            final MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder(methodName).addModifiers(
                    Modifier.ABSTRACT, Modifier.PUBLIC).returns(returnType);
            me.getParameters().forEach(ve -> methodBuilder.addParameter(TypeName.get(ve.asType()),
                    ve.getSimpleName().toString()));

            final InvocationGenerator invocation = new InvocationGenerator(packageName, className(), me);
            methodBuilder.addParameter(ClassName.get(packageName, className(), invocation.className()),
                    "invocation");

            me.getThrownTypes().forEach(et -> methodBuilder.addException(TypeName.get(et)));

            iface.addMethod(methodBuilder.build());
            iface.addType(invocation.build());
        });
        iface.addMethod(
                MethodSpec.methodBuilder("wrap").addModifiers(Modifier.STATIC, Modifier.PUBLIC).addParameter(
                        ClassName.get(element.asType()), "service").returns(classType()).addStatement(
                                "return $L", buildWrapClass()).build());
        return iface.build();
    }

    private TypeSpec buildWrapClass() {
        final TypeSpec.Builder wrapClass = TypeSpec.anonymousClassBuilder("").addSuperinterface(classType());
        ElementFilter.methodsIn(element.getEnclosedElements()).forEach(me -> {
            final String methodName = me.getSimpleName().toString();
            final TypeName returnType = TypeName.get(me.getReturnType());

            final boolean isVoid = me.getReturnType().getKind() == TypeKind.VOID;

            final MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder(methodName).addModifiers(
                    Modifier.PUBLIC).returns(returnType);

            final List<String> paramNames = me.getParameters().stream().map(ve -> {
                methodBuilder.addParameter(TypeName.get(ve.asType()), ve.getSimpleName().toString());
                return ve.getSimpleName().toString();
            }).collect(Collectors.toList());

            final InvocationGenerator invocation = new InvocationGenerator(packageName, className(), me);
            methodBuilder.addParameter(ClassName.get(packageName, className(), invocation.className()),
                    "ignored");
            me.getThrownTypes().forEach(et -> methodBuilder.addException(TypeName.get(et)));

            wrapClass.addMethod(methodBuilder.addStatement((isVoid ? "" : "return ") + "service.$L($L)",
                    methodName, String.join(", ", paramNames)).build());
        });
        return wrapClass.build();
    }

    private ClassName classType() {
        return ClassName.get(packageName, className());
    }

    String className() {
        final Middleware anno = element.getAnnotation(Middleware.class);
        return anno.name().length() > 0 ? anno.name() : originClassName + "Middleware";
    }

    String getFullName() {
        return packageName + "." + className();
    }
}
