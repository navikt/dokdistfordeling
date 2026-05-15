package no.nav.dokdistfordeling.storage;

import no.nav.dokdistfordeling.exception.technical.CouldNotSerializeObjectTechnicalException;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.MapperFeature;
import tools.jackson.databind.ObjectWriter;
import tools.jackson.databind.json.JsonMapper;

public class JsonSerializer {

	private static final JsonMapper objectMapper = JsonMapper.builder()
			.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
			.build();

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
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}
	}
}
