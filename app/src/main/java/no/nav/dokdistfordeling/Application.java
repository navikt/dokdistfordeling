package no.nav.dokdistfordeling;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.annotation.EnableRetry;

@Configuration
@EnableAutoConfiguration
@ComponentScan
@EnableRetry
public class Application {

	public static void main(String[] args) {
		SpringApplication.run(Application.class, args);
	}
}
