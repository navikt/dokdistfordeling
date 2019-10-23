package no.nav.dokdistfordeling.util;

import static java.lang.String.format;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import no.nav.dokdistfordeling.exception.functional.InvalidMappingToEnumFunctionalException;

import java.io.IOException;

public final class MappingUtil {

	private MappingUtil() {
	}

	public static <E extends Enum<E>> E stringToEnum(Class<E> enumClass, String enumName) {
		try {
			return enumName == null ? null : Enum.valueOf(enumClass, enumName);
		} catch (IllegalArgumentException e) {
			throw new InvalidMappingToEnumFunctionalException(format("%s er ikke en gyldig kodeverdi for %s", enumName, enumClass));
		}
	}

	public static <T> T jsonStringToObject(String jsonString, Class<T> tClass) {
		ObjectMapper mapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

		try {
			return mapper.readValue(jsonString, tClass);
		} catch (IOException e) {
			return null;
		}

	}
}
