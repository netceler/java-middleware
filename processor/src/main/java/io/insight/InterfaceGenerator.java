package io.insight;

import com.squareup.javapoet.*;

import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.util.ElementFilter;
import java.io.IOException;
import java.io.Writer;
import java.util.List;
import java.util.stream.Collectors;

class InterfaceGenerator {

  private final String packageName;
  private final String originClassName;
  private final TypeElement element;

  InterfaceGenerator(String packageName, TypeElement element) {

    this.packageName = packageName;
    this.originClassName = element.getSimpleName().toString();
    this.element = element;
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
    TypeSpec.Builder iface = TypeSpec.interfaceBuilder(className())
        .addModifiers(Modifier.PUBLIC);



    ElementFilter.methodsIn(element.getEnclosedElements()).forEach(me -> {
      TypeName returnType = TypeName.get(me.getReturnType());
      String methodName = me.getSimpleName().toString();
      MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder(methodName)
          .addModifiers(Modifier.ABSTRACT, Modifier.PUBLIC)
          .returns(returnType);
      me.getParameters().forEach(ve ->
          methodBuilder.addParameter(TypeName.get(ve.asType()), ve.getSimpleName().toString()));

      InvocationGenerator invocation = new InvocationGenerator(packageName, className(), me);
      methodBuilder.addParameter(ClassName.get(packageName, className(), invocation.className()), "invocation");

      me.getThrownTypes().forEach(et -> methodBuilder.addException(TypeName.get(et)));

      iface.addMethod(methodBuilder.build());
      iface.addType(invocation.build());
    });
    iface.addMethod(MethodSpec.methodBuilder("wrap")
        .addModifiers(Modifier.STATIC,Modifier.PUBLIC)
        .addParameter(ClassName.get(element.asType()), "service")
        .returns(classType())
        .addStatement("return $L", buildWrapClass())
        .build());
    return iface.build();
  }

  private TypeSpec buildWrapClass(){
    TypeSpec.Builder wrapClass = TypeSpec.anonymousClassBuilder("")
        .addSuperinterface(classType());
    ElementFilter.methodsIn(element.getEnclosedElements()).forEach(me -> {
      String methodName = me.getSimpleName().toString();
      TypeName returnType = TypeName.get(me.getReturnType());

      boolean isVoid = me.getReturnType().getKind() == TypeKind.VOID;

      MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder(methodName)
          .addModifiers(Modifier.PUBLIC)
          .returns(returnType);

      List<String> paramNames = me.getParameters().stream().map(ve -> {
        methodBuilder.addParameter(TypeName.get(ve.asType()), ve.getSimpleName().toString());
        return ve.getSimpleName().toString();
      }).collect(Collectors.toList());

      InvocationGenerator invocation = new InvocationGenerator(packageName, className(), me);
      methodBuilder.addParameter(ClassName.get(packageName, className(), invocation.className()), "ignored");
      me.getThrownTypes().forEach(et -> methodBuilder.addException(TypeName.get(et)));

      wrapClass.addMethod(
          methodBuilder
              .addStatement((isVoid ? "" : "return ") + "service.$L($L)", methodName, String.join(", ", paramNames))
              .build());
    });
    return wrapClass.build();
  }

  private ClassName classType() {
    return ClassName.get(packageName, className());
  }

  String className() {
    Middleware anno = element.getAnnotation(Middleware.class);
    return anno.name().length() > 0 ? anno.name() : originClassName + "Middleware";
  }

  String getFullName() {
    return packageName + "." + className();
  }
}
