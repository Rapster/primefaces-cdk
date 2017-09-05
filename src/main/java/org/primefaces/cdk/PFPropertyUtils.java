package org.primefaces.cdk;

import java.util.AbstractMap.SimpleEntry;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;
import javax.lang.model.type.MirroredTypeException;

import org.apache.commons.lang3.StringUtils;
import org.primefaces.cdk.annotations.PFProperty;

import com.aol.cyclops.trycatch.Try;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;

public class PFPropertyUtils {

	public static final Map<TypeName, TypeName> PRIMTIVES = Collections.unmodifiableMap(Stream.of(
				new SimpleEntry<TypeName, TypeName>(ClassName.get(Boolean.class), TypeName.BOOLEAN),
				new SimpleEntry<TypeName, TypeName>(ClassName.get(Integer.class), TypeName.INT),
				new SimpleEntry<TypeName, TypeName>(ClassName.get(Double.class), TypeName.DOUBLE))
			.collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue())));

	public static void sync(Element propertyKeys, Element pfProperty, TypeSpec.Builder impl) {
		PFProperty property = pfProperty.getAnnotation(PFProperty.class);
		Objects.requireNonNull(property, "Expecting element to have a " + PFProperty.class.getSimpleName());

		if (property.ignore()) {
			return;
		}

		TypeName returnType = Try.catchExceptions(MirroredTypeException.class)
				.tryThis(() -> TypeName.class.cast(property.type()))
				.recover(e -> ClassName.get(e.getTypeMirror()))
				.get();

		TypeName primitive = PRIMTIVES.getOrDefault(returnType, returnType);

		// Interface
		String prefix = TypeName.BOOLEAN.equals(primitive) ? "is" : "get";
		String suffix = StringUtils.capitalize("forValue".equals(pfProperty.getSimpleName().toString())
												? "for"
												: pfProperty.getSimpleName().toString());

		// Implementation
		MethodSpec getMethod = MethodSpec.methodBuilder(prefix + suffix)
			.addModifiers(Modifier.PUBLIC)
			.returns(primitive)
			.addCode("return ($T) getStateHelper().eval($T.$L, $L);",
				returnType,
				propertyKeys,
				pfProperty.getSimpleName().toString(),
				returnType.equals(ClassName.get(String.class)) && !property.defaultValue().equals("")
						? "\"" + property.defaultValue() + "\""
						: StringUtils.defaultIfEmpty(property.defaultValue(), null))
			.build();
		impl.addMethod(getMethod);

		MethodSpec setMethod = MethodSpec.methodBuilder("set" + suffix)
			.addModifiers(Modifier.PUBLIC)
			.addParameter(primitive, pfProperty.getSimpleName().toString())
			.addCode("getStateHelper().put($T.$L, $L);",
				propertyKeys,
				pfProperty.getSimpleName().toString(),
				pfProperty.getSimpleName().toString())
			.build();
		impl.addMethod(setMethod);
	}

	public static void sync(Element propertyKeys, TypeSpec.Builder impl) {
		listPFProperties(propertyKeys).stream().forEach(elt -> sync(propertyKeys, elt, impl));
	}

	public static final List<Element> listPFProperties(Element element) {
		return element.getEnclosedElements().stream().filter(elt -> elt.getAnnotation(PFProperty.class) != null)
				.collect(Collectors.toList());
	}
}
