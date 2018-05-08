package io.insight;

import com.squareup.javapoet.*;

import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.ElementFilter;
import java.io.IOException;
import java.io.Writer;

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
      MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder(me.getSimpleName().toString())
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
    return iface.build();
  }

  String className() {
    Middleware anno = element.getAnnotation(Middleware.class);
    return anno.name().length() > 0 ? anno.name() : originClassName + "Middleware";
  }

  String getFullName() {
    return packageName + "." + className();
  }
}
