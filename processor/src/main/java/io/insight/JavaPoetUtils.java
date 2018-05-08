package io.insight;

import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;

import javax.lang.model.element.Modifier;


public class JavaPoetUtils {
  public static void addPrivateField(TypeSpec.Builder classBuilder,
                                     MethodSpec.Builder constructorBuilder,
                                     TypeName type,
                                     String name,
                                     boolean addSetter, boolean addGetter) {
    classBuilder.addField(FieldSpec.builder(type, name, Modifier.PRIVATE).build());
    constructorBuilder
        .addParameter(type,name)
        .addStatement("this.$L = $L", name, name);
    if (addGetter) {
      classBuilder.addMethod(MethodSpec.methodBuilder("get" + upperCaseFirst(name))
          .addModifiers(Modifier.PUBLIC)
          .returns(type)
          .addStatement("return this.$L", name)
          .build());
    }
    if (addSetter) {
      classBuilder.addMethod(MethodSpec.methodBuilder("set" + upperCaseFirst(name))
          .addModifiers(Modifier.PUBLIC)
          .addParameter(type,name)
          .addStatement("this.$L = $L", name, name)
          .build());
    }
  }

  public static String upperCaseFirst(String s){
    if (s.length() > 0) {
      return s.substring(0, 1).toUpperCase() +
          s.substring(1);
    }
    return s;
  }
}
