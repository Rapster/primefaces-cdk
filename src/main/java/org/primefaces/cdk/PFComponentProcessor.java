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
import javax.lang.model.type.MirroredTypeException;
import javax.tools.Diagnostic;

import org.primefaces.cdk.annotations.PFComponent;

import com.aol.cyclops.trycatch.Try;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;
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
			System.out.println("Creating " + component.getSimpleName() + "...");
			PFComponent definition = component.getAnnotation(PFComponent.class);

			TypeName parent = Try.catchExceptions(MirroredTypeException.class)
					.tryThis(() -> TypeName.class.cast(definition.parent()))
					.recover(e -> ClassName.get(e.getTypeMirror()))
					.get();

			// Implementation
			TypeSpec.Builder impl = TypeSpec.classBuilder("Abstract" + component.getSimpleName().toString())
					.addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
					.superclass(parent);

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

			if (definition.widget()) {
				WidgetUtils.sync(roundEnv, impl);
			}

			if (definition.rtl()) {
				RTLUtils.sync(roundEnv, processingEnv, component, impl);
			}

			PFPropertyKeysUtils.sync(processingEnv, component, impl);

			// Implementation
			JavaFile componentFile = JavaFile
					.builder(processingEnv.getElementUtils().getPackageOf(component).toString(), impl.build())
					.build();

			Try.catchExceptions(IOException.class)
					.run(() -> componentFile.writeTo(processingEnv.getFiler()))
					.onFail(e-> processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, e.getMessage()));

			System.out.println("Creating " + component.getSimpleName() + " is done");
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
