package no.nav.dokdistfordeling.config.json;


import com.fasterxml.jackson.databind.MapperFeature;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JsonConfig {

	@Bean
	public Jackson2ObjectMapperBuilderCustomizer initJacksonObjectMapper() {
		return builder -> builder.featuresToEnable(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES);
	}

}


