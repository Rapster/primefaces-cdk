package org.primefaces.cdk;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.Name;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.MirroredTypesException;

import org.primefaces.cdk.annotations.PFProperty;
import org.primefaces.cdk.annotations.PFPropertyKeys;

import com.aol.cyclops.trycatch.Try;
import com.squareup.javapoet.TypeSpec;

public class PFPropertyKeysUtils {

	public static void sync(ProcessingEnvironment processingEnv, Element component, TypeSpec.Builder interfaze, TypeSpec.Builder impl) {
		Optional<? extends Element> optPropertyKeys = findPFPropertyKeys(component);

		if (!optPropertyKeys.isPresent()) {
			// throw new
		}

		// Write PropertyKeys enum defined in component
		Element propertyKeys = optPropertyKeys.get();
		List<Element> pfproperties = PFPropertyUtils.listPFProperties(propertyKeys);
		PFPropertyUtils.sync(propertyKeys, interfaze, impl);

		// Write PFComponent.properties
		PFPropertyKeys pfcomponent = propertyKeys.getAnnotation(PFPropertyKeys.class);
		List<TypeElement> pfPropertyKeysSet = retrievePropertyKeys(processingEnv, pfcomponent);

		List<Name> propertyNames = pfproperties.stream().map(Element::getSimpleName).collect(Collectors.toList());
		for (TypeElement element : pfPropertyKeysSet) {
			List<? extends Element> properties = element.getEnclosedElements().stream()
					.filter(elt -> elt.getAnnotation(PFProperty.class) != null
					&& !propertyNames.contains(elt.getSimpleName())).collect(Collectors.toList());
			properties.stream().forEach(elt -> PFPropertyUtils.sync(element, elt, interfaze, impl));
		}
	}

	@SuppressWarnings("unchecked")
	public static List<TypeElement> retrievePropertyKeys(ProcessingEnvironment processingEnv, PFPropertyKeys propertyKeys ) {
		List<TypeElement> propertiesKeysSet = Try.catchExceptions(MirroredTypesException.class)
				.tryThis(() -> List.class.cast(propertyKeys.base()))
				.recover(e -> e.getTypeMirrors().stream()
						.map(elt -> processingEnv.getElementUtils().getTypeElement(elt.toString()))
						.collect(Collectors.toList()))
				.get();
		List<TypeElement> keys = new ArrayList<>();
		retrievePropertyKeys(processingEnv, propertiesKeysSet, keys);
		return keys;
	}

	public static Optional<? extends Element> findPFPropertyKeys(Element component) {
		return component.getEnclosedElements().stream()
				.filter(e -> e.getAnnotation(PFPropertyKeys.class) != null).findFirst();
	}

	@SuppressWarnings("unchecked")
	private static void retrievePropertyKeys(ProcessingEnvironment processingEnv, List<TypeElement> properties, List<TypeElement> keys) {
		for(TypeElement property : properties) {
			keys.add(property);
			PFPropertyKeys k = property.getAnnotation(PFPropertyKeys.class);
			List<TypeElement> t = Try.catchExceptions(MirroredTypesException.class)
					.tryThis(() -> List.class.cast(k.base()))
					.recover(e -> e.getTypeMirrors().stream().map(elt -> processingEnv.getElementUtils().getTypeElement(elt.toString())).collect(Collectors.toList()))
					.get();
			retrievePropertyKeys(processingEnv, t, keys);
		}
	}
}
