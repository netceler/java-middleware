package io.insight;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
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

public class ChainGenerator {
    private final String packageName;

    private final String originClassName;

    private final TypeElement element;

    private final String itfName;

    ChainGenerator(final String packageName, final TypeElement element, final String itfName) {

        this.packageName = packageName;
        this.originClassName = element.getSimpleName().toString();
        this.element = element;
        this.itfName = itfName;
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
        final TypeSpec.Builder classBuilder = TypeSpec.classBuilder(className()).superclass(
                ParameterizedTypeName.get(ClassName.get(ServiceChain.class),
                        ClassName.get(packageName, itfName))).addSuperinterface(
                                ClassName.get(element.asType())).addModifiers(Modifier.PUBLIC);

        ElementFilter.methodsIn(element.getEnclosedElements()).forEach(me -> {
            final TypeName returnType = TypeName.get(me.getReturnType());
            final MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder(
                    me.getSimpleName().toString()).addModifiers(Modifier.PUBLIC).returns(returnType);
            me.getParameters().forEach(ve -> methodBuilder.addParameter(TypeName.get(ve.asType()),
                    ve.getSimpleName().toString()));

            final List<String> paramNames = me.getParameters().stream().map(
                    ve -> ve.getSimpleName().toString()).collect(Collectors.toList());
            me.getThrownTypes().forEach(et -> methodBuilder.addException(TypeName.get(et)));
            final ClassName invocationType = ClassName.get(packageName, itfName,
                    JavaPoetUtils.upperCaseFirst(me.getSimpleName().toString()) + "Invocation");
            if (me.getReturnType().getKind() == TypeKind.VOID) {
                methodBuilder.addStatement("new $T(root.get() $L).next()", invocationType,
                        (paramNames.isEmpty() ? "" : ", ") + String.join(", ", paramNames));
            } else {
                methodBuilder.addStatement("return new $T(root.get() $L).next()", invocationType,
                        (paramNames.isEmpty() ? "" : ", ") + String.join(", ", paramNames));
            }
            classBuilder.addMethod(methodBuilder.build());

        });
        return classBuilder.build();
    }

    String className() {
        return originClassName + "Chain";
    }

    String getFullName() {
        return packageName + "." + className();
    }
}
