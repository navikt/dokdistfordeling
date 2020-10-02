package no.nav.dokdistfordeling.swagger;

import static springfox.documentation.builders.PathSelectors.regex;

import com.google.common.collect.Lists;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.service.ApiKey;
import springfox.documentation.service.Contact;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger.web.DocExpansion;
import springfox.documentation.swagger.web.ModelRendering;
import springfox.documentation.swagger.web.OperationsSorter;
import springfox.documentation.swagger.web.TagsSorter;
import springfox.documentation.swagger.web.UiConfiguration;
import springfox.documentation.swagger.web.UiConfigurationBuilder;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

import java.util.Collections;

@Configuration
@EnableSwagger2
public class SwaggerConfig implements WebMvcConfigurer {

	@Value("${APP_VERSION:0.0.0}")
	private String version;

	@Bean
	public Docket api() {
		return new Docket(DocumentationType.SWAGGER_2)
				.select()
				.apis(RequestHandlerSelectors.any())
				.paths(regex("/rest/v1.*"))
				.build()
				.useDefaultResponseMessages(false)
				.apiInfo(apiInfo())
				.securitySchemes(Lists.newArrayList(apiKey()));
	}

	@Bean
	UiConfiguration uiConfig() {
		return UiConfigurationBuilder.builder()
				.deepLinking(true)
				.displayOperationId(false)
				.defaultModelsExpandDepth(1)
				.defaultModelExpandDepth(1)
				.defaultModelRendering(ModelRendering.EXAMPLE)
				.displayRequestDuration(false)
				.docExpansion(DocExpansion.NONE)
				.filter(false)
				.maxDisplayedTags(null)
				.operationsSorter(OperationsSorter.ALPHA)
				.showExtensions(false)
				.tagsSorter(TagsSorter.ALPHA)
				.validatorUrl(null)
				.build();
	}

	private ApiInfo apiInfo() {
		return new ApiInfo(
				"DistribuerJournalpost API",
				"Her dokumenteres tjenestegrensesnittet til distribuerJournalpost. Til autentisering brukes OIDC-token (JWT via OAuth2.0). " +
						"Følgende format må brukes i Authorize sitt input-felt \"Value\": <strong> Bearer {token} </strong>. " +
						"Eksempel på verdi i input-feltet: <strong> Bearer eYdmifml0ejugm </strong>. Et gyldig token kommer til å ha mange flere karakterer enn i eksempelet.",
				version,
				"",
				new Contact("Team Dokument", "", ""),
				"", "", Collections.EMPTY_LIST);
	}

	private ApiKey apiKey() {

		return new ApiKey("apiKey", HttpHeaders.AUTHORIZATION, "header");
	}

	@Override
	public void addResourceHandlers(ResourceHandlerRegistry registry) {
		registry.addResourceHandler("/swagger-ui/**")
				.addResourceLocations("classpath:/META-INF/resources/webjars/springfox-swagger-ui/")
				.resourceChain(false);
	}

	@Override
	public void addViewControllers(ViewControllerRegistry registry) {
		registry.addViewController("/swagger-ui/")
				.setViewName("forward:" + "/swagger-ui/index.html");
	}
}
