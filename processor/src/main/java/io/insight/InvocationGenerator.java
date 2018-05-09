package io.insight;

import com.squareup.javapoet.*;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.type.TypeKind;
import java.util.ArrayList;
import java.util.stream.Collectors;

public class InvocationGenerator {


  private String packageName;
  private String ifaceClassName;
  private ExecutableElement method;

  public InvocationGenerator(String packageName,String ifaceClassName, ExecutableElement method) {
    this.packageName = packageName;
    this.ifaceClassName = ifaceClassName;
    this.method = method;
  }


  public TypeSpec build() {

    TypeSpec.Builder classBuilder = TypeSpec.classBuilder(className())
        .addModifiers(Modifier.PUBLIC, Modifier.STATIC);
    boolean isVoid = method.getReturnType().getKind() == TypeKind.VOID;

    MethodSpec.Builder constructorBuilder = MethodSpec.constructorBuilder();
    JavaPoetUtils.addPrivateField(classBuilder, constructorBuilder,
        TypeName.get(LinkedService.class), "service", false, false);
    ArrayList<String> paramNames=new ArrayList<>();
    method.getParameters().forEach(ve -> {
      paramNames.add(ve.getSimpleName().toString());
      JavaPoetUtils.addPrivateField(classBuilder, constructorBuilder,
          TypeName.get(ve.asType()),
          ve.getSimpleName().toString(),
          true,
          false
      );
    });
    classBuilder.addMethod(constructorBuilder.build());


    MethodSpec.Builder nextMethod = MethodSpec.methodBuilder("next")
        .addExceptions(method.getThrownTypes().stream().map(TypeName::get).collect(Collectors.toList()))
        .addModifiers(Modifier.PUBLIC);

    if(isVoid) {
        nextMethod.addStatement("(($L) this.service.getService()).$L($L nextInvocation())",
          ifaceClassName,
          method.getSimpleName().toString(),
          String.join(", ", paramNames) + (paramNames.isEmpty()? "" : ", "));
    } else {
      nextMethod.addStatement("return (($L) this.service.getService()).$L($L nextInvocation())",
          ifaceClassName,
          method.getSimpleName().toString(),
          String.join(", ", paramNames) + (paramNames.isEmpty()? "" : ", "))
      .returns(TypeName.get(method.getReturnType()));
    }
    classBuilder.addMethod(nextMethod
        .build());
    classBuilder.addMethod(MethodSpec.methodBuilder("nextInvocation")
        .addModifiers(Modifier.PRIVATE)
        .returns(ClassName.get(packageName,ifaceClassName,className()))
        .addCode(CodeBlock.builder()
            .beginControlFlow("if (this.service.hasNext())")
            .addStatement("return new $L(this.service.next()$L)",
                className(),
                paramNames.isEmpty()? "" : ", " +  String.join(", ", paramNames))
            .nextControlFlow("else")
            .addStatement("return null")
            .endControlFlow()
            .build())
        .build());

    return classBuilder.build();
  }

  String className() {
    return JavaPoetUtils.upperCaseFirst(method.getSimpleName().toString()) + "Invocation";
  }


}
