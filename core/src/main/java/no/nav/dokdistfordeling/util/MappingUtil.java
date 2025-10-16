package no.nav.dokdistfordeling.util;

import no.nav.dokdistfordeling.exception.functional.InvalidMappingToEnumFunctionalException;
import no.nav.dokdistfordeling.exception.functional.ValidationException;

import static java.lang.String.format;
import static no.nav.dokdistfordeling.constants.Constants.BEARER_PREFIX;
import static org.apache.commons.lang3.ObjectUtils.isEmpty;

public final class MappingUtil {

	private static final String NULL = "NULL";

	private MappingUtil() {
	}

	public static <E extends Enum<E>> E stringToEnum(Class<E> enumClass, String enumName) {
		try {
			return (NULL.equals(enumName) || isEmpty(enumName)) ? null : Enum.valueOf(enumClass, enumName);
		} catch (IllegalArgumentException e) {
			throw new InvalidMappingToEnumFunctionalException(format("Ulovlig verdi ble forsøkt mappet til enum: %s er ikke en gyldig kodeverdi for %s", enumName, enumClass));
		}
	}

	public static String splitBearerToken(String authorizationHeader) {
		if (authorizationHeader == null || !BEARER_PREFIX.equalsIgnoreCase(authorizationHeader.split(" ")[0])) {
			throw new ValidationException("Authorization header må være på formen Bearer {token}");
		}

		return authorizationHeader.split(" ")[1];
	}
}
