package no.nav.dokdistfordeling.config;

import no.nav.dokdistfordeling.config.azure.AzureProperties;
import no.nav.dokdistfordeling.web.MDCHandlerInterceptor;
import no.nav.security.token.support.core.context.TokenValidationContextHolder;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

	private final TokenValidationContextHolder tokenValidationContextHolder;
	private final AzureProperties azureProperties;

	public WebMvcConfig(TokenValidationContextHolder tokenValidationContextHolder,
						AzureProperties azureProperties) {
		this.tokenValidationContextHolder = tokenValidationContextHolder;
		this.azureProperties = azureProperties;
	}

	@Override
	public void addInterceptors(InterceptorRegistry registry) {
		registry.addInterceptor(new MDCHandlerInterceptor(tokenValidationContextHolder, azureProperties))
				.addPathPatterns("/rest/**");
	}
}