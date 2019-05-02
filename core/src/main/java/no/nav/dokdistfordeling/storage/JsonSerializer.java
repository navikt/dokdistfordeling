package no.nav.dokdistfordeling.storage;

import com.amazonaws.util.json.Jackson;
import no.nav.dokdistfordeling.exception.technical.CouldNotSerializeObjectTechnicalException;

/**
 * @author Ugur Alpay Cenar, Visma Consulting.
 */
public class JsonSerializer {

	public static String serialize(Object object) {
		try {
			return Jackson.toJsonString(object);
		} catch (IllegalStateException e) {
			throw new CouldNotSerializeObjectTechnicalException(e.getMessage(), e);
		}
	}

	public static <T> T deserialize(String jsonPayload, Class<T> tClass) {
		return Jackson.fromJsonString(jsonPayload, tClass);
	}
}
