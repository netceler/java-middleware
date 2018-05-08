package io.insight;

import com.squareup.javapoet.*;

import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.util.ElementFilter;
import java.io.IOException;
import java.io.Writer;
import java.util.List;
import java.util.stream.Collectors;

public class ChainGenerator {
  private final String packageName;
  private final String originClassName;
  private final TypeElement element;
  private String itfName;

  ChainGenerator(String packageName, TypeElement element, String itfName) {

    this.packageName = packageName;
    this.originClassName = element.getSimpleName().toString();
    this.element = element;
    this.itfName = itfName;
  }

  void writeTo(Writer writer) throws IOException {
    JavaFile javaFile = null;
    try {
      javaFile = JavaFile.builder(packageName, build()).build();
    } catch (Exception e) {
      e.printStackTrace();
    }
    javaFile.writeTo(writer);
  }

  private TypeSpec build() {
    TypeSpec.Builder classBuilder = TypeSpec.classBuilder(className())
        .superclass(ParameterizedTypeName.get(ClassName.get(ServiceChain.class),
            ClassName.get(packageName, itfName)))
        .addSuperinterface(ClassName.get(element.asType()))
        .addModifiers(Modifier.PUBLIC);

    ElementFilter.methodsIn(element.getEnclosedElements()).forEach(me -> {
      TypeName returnType = TypeName.get(me.getReturnType());
      MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder(me.getSimpleName().toString())
          .addModifiers(Modifier.PUBLIC)
          .returns(returnType);
      me.getParameters().forEach(ve ->
          methodBuilder.addParameter(TypeName.get(ve.asType()), ve.getSimpleName().toString()));
       
      List<String> paramNames = me.getParameters().stream().map(ve -> ve.getSimpleName().toString())
          .collect(Collectors.toList());
      me.getThrownTypes().forEach(et -> methodBuilder.addException(TypeName.get(et)));
      ClassName invocationType = ClassName.get(packageName, itfName, JavaPoetUtils.upperCaseFirst(me.getSimpleName().toString()) + "Invocation");
      if (me.getReturnType().getKind() == TypeKind.VOID) {
        methodBuilder.addStatement("new $T(root.get(), $L).next()", invocationType, String.join(", ", paramNames));
      } else {
        methodBuilder.addStatement("return new $T(root.get(), $L).next()", invocationType, String.join(", ", paramNames));
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
