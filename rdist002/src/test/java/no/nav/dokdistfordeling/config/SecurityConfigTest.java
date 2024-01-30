package no.nav.dokdistfordeling.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Profile("itest")
@Configuration
public class SecurityConfigTest {
	@Bean
	public SecurityFilterChain filterChain(HttpSecurity httpSecurity) throws Exception {
		httpSecurity.csrf(csrfConfigurer -> csrfConfigurer.disable());

		return httpSecurity.build();
	}
}
