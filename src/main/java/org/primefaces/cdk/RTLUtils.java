package org.primefaces.cdk;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.Name;
import javax.lang.model.element.TypeElement;

import org.primefaces.cdk.annotations.PFProperty;
import org.primefaces.cdk.annotations.PFPropertyKeys;
import org.primefaces.cdk.annotations.PFRTL;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;

public class RTLUtils {

	public static void sync(RoundEnvironment roundEnv, ProcessingEnvironment processingEnv, Element component, TypeSpec.Builder impl) {
		Optional<? extends Element> optRTLPropertyKeys = roundEnv.getElementsAnnotatedWith(PFPropertyKeys.class).stream()
				.filter(elt -> elt.getEnclosingElement().getAnnotation(PFRTL.class) != null).findFirst();

		if (!optRTLPropertyKeys.isPresent()) {
			throw new UnsupportedOperationException("No property keys found for " + PFRTL.class);
		}

		Element rtlPropertyKeys = optRTLPropertyKeys.get();
		impl.addSuperinterface(ClassName.get((TypeElement)rtlPropertyKeys.getEnclosingElement()));

		MethodSpec isRTL = MethodSpec.methodBuilder("isRTL")
				.addModifiers(Modifier.PUBLIC)
				.returns(TypeName.BOOLEAN)
				.addCode("return $L", "getDir().equalsIgnoreCase(\"rtl\");")
				.build();
		impl.addMethod(isRTL);

		PFPropertyKeys propertyKeys = PFPropertyKeysUtils.findPFPropertyKeys(component).get().getAnnotation(PFPropertyKeys.class);

		List<Element> rtlProperties = PFPropertyUtils.listPFProperties(rtlPropertyKeys);
		List<Name> pfproperties = PFPropertyKeysUtils.retrievePropertyKeys(processingEnv, propertyKeys)
				.stream()
				.map(TypeElement::getEnclosedElements)
				.flatMap(i -> i.stream())
				.filter(elt -> elt.getAnnotation(PFProperty.class) != null)
				.map(Element::getSimpleName)
				.collect(Collectors.toList());

		for (Element element : rtlProperties) {
			if(!pfproperties.contains(element.getSimpleName())) {
				PFPropertyUtils.sync(rtlPropertyKeys, element, impl);
			}
		}
	}
}
