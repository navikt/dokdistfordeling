package no.nav.dokdistfordeling.util;

import no.nav.dokdistfordeling.exception.functional.ValidationException;

import java.util.stream.Stream;

import static java.lang.String.format;
import static java.util.stream.Collectors.joining;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNumeric;

public final class ValidationUtil {

	private ValidationUtil() {
	}

	public static void assertNotNullOrEmpty(String field, String value) {
		if (isBlank(value)) {
			throw new ValidationException(format("Feltet %s kan ikke være null eller tomt. Fikk %s=%s", field, field, value));
		}
	}

	public static void assertStringIsNumberOfExactLength(String field, String value, int expectedLength) {
		if (!isNumeric(value) || value.length() != expectedLength) {
			throw new ValidationException(format("Feltet %s må være et gyldig tall med %s siffer. Fikk %s=%s", field, expectedLength, field, value));
		}
	}

	public static void assertNotNull(Class inputClass, Object value) {
		if (value == null) {
			throw new ValidationException(format("%s kan ikke være null. Fikk %s=null", inputClass.getCanonicalName(), inputClass
					.getCanonicalName()));
		}
	}

	public static void assertParameterIsAsExpected(String parameterName, String value, String expected) {
		if (!expected.equals(value)) {
			throw new ValidationException(String.format("%s er ikke som forventet, fikk: %s, men forventet %s", parameterName, value, expected));
		}
	}

	public static void assertJournalpostFieldNotNull(Class inputClass, Object value) {
		if (value == null) {
			throw new ValidationException(format("For journalposter kan feltet %s ikke være null eller tomt. Fikk %s=null", inputClass.getCanonicalName(), inputClass.getCanonicalName()));
		}
	}

	public static void assertJournalpostFieldNotNullOrEmpty(String field, String value) {
		if (isBlank(value)) {
			throw new ValidationException(format("For journalposter kan feltet %s ikke være null eller tomt. Fikk %s=%s", field, field, value));
		}
	}

	public static void assertDokumentFieldNotNullOrEmpty(String field, String value) {
		if (isBlank(value)) {
			throw new ValidationException(format("For dokumenter kan feltet %s ikke være null eller tomt. Fikk %s=%s", field, field, value));
		}
	}

	public static void assertHovedokumentFieldNotNullOrEmpty(String field, String value) {
		if (isBlank(value)) {
			throw new ValidationException(format("For hoveddokumentet kan feltet %s ikke være null eller tomt. Fikk %s=%s", field, field, value));
		}
	}

	public static <T extends Enum<?>> void assertNotNullAndValidValueIgnoreCase(String field, String value, T... validValues) {
		assertNotNullOrEmpty(field, value);
		if (Stream.of(validValues).map(Enum::name).noneMatch(value::equalsIgnoreCase)) {
			throw new ValidationException(format("Feltet %s hadde en ugyldig verdi. Fikk %s=%s. Gyldige verdier er [%s]", field, field, value, Stream.of(validValues).map(Enum::name).collect(joining(", "))));
		}

	}
}
