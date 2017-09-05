package org.primefaces.cdk;

import java.util.Optional;

import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;

import org.primefaces.cdk.annotations.PFPropertyKeys;
import org.primefaces.cdk.annotations.PFWidget;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeSpec;

public class WidgetUtils {

	public static void sync(RoundEnvironment roundEnv, TypeSpec.Builder impl) {
		Optional<? extends Element> optWidgetPropertyKeys = roundEnv.getElementsAnnotatedWith(PFPropertyKeys.class).stream()
				.filter(elt -> elt.getEnclosingElement().getAnnotation(PFWidget.class) != null).findFirst();

		if (!optWidgetPropertyKeys.isPresent()) {
			throw new UnsupportedOperationException("No property keys found for " + PFWidget.class);
		}

		Element widgetPropertyKeys = optWidgetPropertyKeys.get();

		impl.addSuperinterface(ClassName.get((TypeElement)widgetPropertyKeys.getEnclosingElement()));
		MethodSpec resolveWidgetVar = MethodSpec.methodBuilder("resolveWidgetVar")
				.addModifiers(Modifier.PUBLIC)
				.returns(ClassName.get(String.class))
				.addCode("return $T.$L", ClassName.get("org.primefaces.util", "ComponentUtils"), "resolveWidgetVar(getFacesContext(), this);")
				.build();
		impl.addMethod(resolveWidgetVar);

		PFPropertyUtils.sync(widgetPropertyKeys, impl);
	}
}
