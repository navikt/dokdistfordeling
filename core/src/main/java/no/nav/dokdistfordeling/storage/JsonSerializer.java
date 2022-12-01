package no.nav.dokdistfordeling.storage;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import no.nav.dokdistfordeling.exception.technical.CouldNotSerializeObjectTechnicalException;

public class JsonSerializer {

	private static final ObjectMapper objectMapper = new ObjectMapper();

	static {
		objectMapper.configure(JsonParser.Feature.ALLOW_COMMENTS, true);
		objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
	}

	private static final ObjectWriter writer = objectMapper.writer();

	public static String serialize(Object object) {
		try {
			return writer.writeValueAsString(object);
		} catch (Exception e) {
			throw new CouldNotSerializeObjectTechnicalException(e.getMessage(), e);
		}
	}

	public static <T> T deserialize(String jsonPayload, Class<T> tClass) {
		try {
			return objectMapper.readValue(jsonPayload, tClass);
		} catch (JsonProcessingException e) {
			throw new IllegalStateException(e);
		}
	}
}
