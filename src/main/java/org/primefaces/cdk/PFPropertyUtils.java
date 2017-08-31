package org.primefaces.cdk;

import java.util.AbstractMap.SimpleEntry;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
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
import com.squareup.javapoet.MethodSpec.Builder;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import com.squareup.javapoet.TypeVariableName;

public class PFPropertyUtils {

	public static final Map<TypeName, TypeName> PRIMTIVES = Collections.unmodifiableMap(Stream.of(
				new SimpleEntry<TypeName, TypeName>(ClassName.get(Boolean.class), TypeName.BOOLEAN),
				new SimpleEntry<TypeName, TypeName>(ClassName.get(Integer.class), TypeName.INT),
				new SimpleEntry<TypeName, TypeName>(ClassName.get(Double.class), TypeName.DOUBLE))
			.collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue())));

	public static void sync(Element propertyKeys, Element pfProperty, TypeSpec.Builder interfaze, TypeSpec.Builder impl) {
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

		MethodSpec igetMethod = MethodSpec.methodBuilder(prefix + suffix)
				.addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
				.returns(primitive)
				.build();

		interfaze.addMethod(igetMethod);

		MethodSpec isetMethod = MethodSpec.methodBuilder("set" + suffix)
				.addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
				.addParameter(primitive, pfProperty.getSimpleName().toString())
				.build();

		interfaze.addMethod(isetMethod);

		// Implementation
		MethodSpec getMethod = overriding(igetMethod)
				.addCode("return ($T) getStateHelper().eval($T.$L, $L);",
					returnType,
					propertyKeys,
					pfProperty.getSimpleName().toString(),
					returnType.equals(ClassName.get(String.class)) && !property.defaultValue().equals("")
							? "\"" + property.defaultValue() + "\""
							: StringUtils.defaultIfEmpty(property.defaultValue(), null))
				.build();
		impl.addMethod(getMethod);

		MethodSpec setMethod = overriding(isetMethod)
				.addCode("getStateHelper().put($T.$L, $L);",
				propertyKeys,
				pfProperty.getSimpleName().toString(),
				pfProperty.getSimpleName().toString()).build();
		impl.addMethod(setMethod);
	}

	public static void sync(Element propertyKeys, TypeSpec.Builder interf, TypeSpec.Builder impl) {
		listPFProperties(propertyKeys).stream().forEach(elt -> sync(propertyKeys, elt, interf, impl));
	}

	public static final List<Element> listPFProperties(Element element) {
		return element.getEnclosedElements().stream().filter(elt -> elt.getAnnotation(PFProperty.class) != null)
				.collect(Collectors.toList());
	}

	/**
	 * Returns a new method spec builder that overrides {@code method}.
	 *
	 * <p>
	 * This will copy its visibility modifiers, type parameters, return type, name,
	 * parameters, and throws declarations. An {@link Override} annotation will be
	 * added.
	 *
	 * <p>
	 * Note that in JavaPoet 1.2 through 1.7 this method retained annotations from
	 * the method and parameters of the overridden method. Since JavaPoet 1.8
	 * annotations must be added separately.
	 */
	public static Builder overriding(MethodSpec method) {
		Objects.requireNonNull(method, "method == null");

		Set<Modifier> modifiers = method.modifiers;
		if (modifiers.contains(Modifier.PRIVATE) || modifiers.contains(Modifier.FINAL)
				|| modifiers.contains(Modifier.STATIC)) {
			throw new IllegalArgumentException("cannot override method with modifiers: " + modifiers);
		}

		String methodName = method.name;
		MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder(methodName);

		methodBuilder.addAnnotation(Override.class);

		modifiers = new LinkedHashSet<>(modifiers);
		modifiers.remove(Modifier.ABSTRACT);
		modifiers.remove(Modifier.valueOf("DEFAULT")); // LinkedHashSet permits null as element for Java 7
		methodBuilder.addModifiers(modifiers);

		for (TypeVariableName typeParameterElement : method.typeVariables) {
			methodBuilder.addTypeVariable(typeParameterElement);
		}

		methodBuilder.returns(method.returnType);
		methodBuilder.addParameters(method.parameters);
		methodBuilder.varargs(method.varargs);

		for (TypeName thrownType : method.exceptions) {
			methodBuilder.addException(thrownType);
		}

		return methodBuilder;
	}
}
