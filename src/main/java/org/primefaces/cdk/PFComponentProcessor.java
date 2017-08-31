package org.primefaces.cdk;

import java.io.IOException;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;

import org.primefaces.cdk.annotations.PFComponent;

import com.aol.cyclops.trycatch.Try;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeSpec;

@SupportedAnnotationTypes("org.primefaces.cdk.annotations.PFComponent")
@SupportedSourceVersion(SourceVersion.RELEASE_8)
public class PFComponentProcessor extends AbstractProcessor {

	@Override
	public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
		Set<? extends Element> annotatedElements = roundEnv.getElementsAnnotatedWith(PFComponent.class);
		if (annotatedElements.isEmpty()) {
//			throw new IllegalArgumentException("Expecting component having annotation " + PFComponent.class.getSimpleName());
		}

		System.out.println("Starting processing...");
		for (Element component : annotatedElements) {
			System.out.println(component.getSimpleName());

			// Interface
			TypeSpec.Builder interfaze = TypeSpec.interfaceBuilder("I" + component.getSimpleName().toString().replaceAll("Core", ""))
					.addModifiers(Modifier.PUBLIC);

			// Implementation
			TypeSpec.Builder impl = TypeSpec.classBuilder(component.getSimpleName().toString().replace("Core", ""))
					.addModifiers(Modifier.PUBLIC)
					.superclass(ClassName.get(processingEnv.getElementUtils().getPackageOf(component).toString(), component.getSimpleName().toString()));

			FieldSpec componentType = FieldSpec.builder(ClassName.get(String.class), "COMPONENT_TYPE", Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
					.initializer("$S", getComponentType(component))
					.build();
			FieldSpec componentFamily = FieldSpec.builder(ClassName.get(String.class), "COMPONENT_FAMILY", Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
					.initializer("$S", getComponentFamily())
					.build();
			FieldSpec rendererType = FieldSpec.builder(ClassName.get(String.class), "DEFAULT_RENDERER", Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
					.initializer("$S", getRendererType(component))
					.build();

			impl.addField(componentType);
			impl.addField(componentFamily);
			impl.addField(rendererType);

			MethodSpec constructor = MethodSpec.constructorBuilder()
					.addModifiers(Modifier.PUBLIC)
					.addCode("$L", "setRendererType(DEFAULT_RENDERER);")
					.build();
			impl.addMethod(constructor);

			MethodSpec getFamily = MethodSpec.methodBuilder("getFamily")
					.addModifiers(Modifier.PUBLIC)
					.returns(ClassName.get(String.class))
					.addCode("return COMPONENT_FAMILY;")
					.addAnnotation(Override.class)
					.build();
			impl.addMethod(getFamily);

			PFComponent definition = component.getAnnotation(PFComponent.class);
			if (definition.widget()) {
				WidgetUtils.sync(roundEnv, interfaze, impl);
			}

			if (definition.rtl()) {
				RTLUtils.sync(roundEnv, processingEnv, component, interfaze, impl);
			}

			PFPropertyKeysUtils.sync(processingEnv, component, interfaze, impl);

			// Interface
			JavaFile interfaceFile = JavaFile
					.builder(processingEnv.getElementUtils().getPackageOf(component).toString(), interfaze.build())
					.build();

			Try.catchExceptions(IOException.class)
					.run(() -> interfaceFile.writeTo(processingEnv.getFiler()))
					.onFail(e-> processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, e.getMessage()));

			// Implementation
			JavaFile componentFile = JavaFile
					.builder(processingEnv.getElementUtils().getPackageOf(component).toString(), impl.build())
					.build();

			Try.catchExceptions(IOException.class)
					.run(() -> componentFile.writeTo(processingEnv.getFiler()))
					.onFail(e-> processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, e.getMessage()));
		}
		return true;
	}

	public String getComponentType(Element element) {
		return getComponentFamily() + "." + element.getSimpleName().toString();
	}

	public String getRendererType(Element element) {
		return getComponentFamily() + "." + element.getSimpleName().toString() + "Renderer";
	}

	public String getComponentFamily() {
		return "org.primefaces.component";
	}


}
