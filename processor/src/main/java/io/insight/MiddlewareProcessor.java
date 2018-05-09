package io.insight;

import com.google.auto.service.AutoService;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.*;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@SupportedAnnotationTypes("io.insight.Middleware")
@SupportedSourceVersion(SourceVersion.RELEASE_8)
@AutoService(Processor.class)
public class MiddlewareProcessor extends AbstractProcessor {
  public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
    for (TypeElement annotation : annotations) {

      Set<? extends Element> annotatedElements = roundEnv.getElementsAnnotatedWith(annotation);

      Map<Boolean, List<Element>> annotatedMethods =
          annotatedElements.stream().collect(Collectors.partitioningBy(this::isValidElement));

      List<Element> interfaces = annotatedMethods.get(true);
      List<Element> otherClasses = annotatedMethods.get(false);

      otherClasses.forEach(
          element -> processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR,
              "@Middleware must be applied to interface", element));

      if (interfaces.isEmpty()) {
        continue;
      }
      for (Element e : interfaces) {
        TypeElement classElement = (TypeElement) e;
        try {
          writeInterfaceFile(classElement);
        } catch (IOException err) {
          StringWriter writer = new StringWriter();
          err.printStackTrace(new PrintWriter(writer));
          processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR,
              writer.toString());
        }
      }
    }

    return true;
  }

  private void writeInterfaceFile(TypeElement classElement) throws IOException {
    PackageElement packageElement =
        processingEnv.getElementUtils().getPackageOf(classElement);
    String pkg = packageElement.getQualifiedName().toString();
    InterfaceGenerator generator = new InterfaceGenerator(pkg, classElement);
    JavaFileObject jfo = processingEnv.getFiler().createSourceFile(generator.getFullName());
    try (Writer writer = jfo.openWriter()) {
      generator.writeTo(writer);
    }
    writeChainFile(pkg, classElement, generator.className());
  }

  private void writeChainFile(String pkg, TypeElement classElement, String itfName) throws IOException {
    ChainGenerator generator = new ChainGenerator(pkg, classElement, itfName);
    JavaFileObject jfo = processingEnv.getFiler().createSourceFile(generator.getFullName());
    try (Writer writer = jfo.openWriter()) {
      generator.writeTo(writer);
    }
  }


  private boolean isValidElement(Element element) {
    return element.getKind() == ElementKind.INTERFACE;
  }
}
