package no.nav.dokdistfordeling.itest.config;

import static no.nav.dokdistfordeling.config.cache.LokalCacheConfig.TKAT020_CACHE;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

/**
 * @author Joakim Bjørnstad, Jbit AS
 */
@Configuration
@Profile({"itest"})
public class LokalTestCacheConfig {

	@Bean
	CacheManager cacheManager() {
		SimpleCacheManager manager = new SimpleCacheManager();
		manager.setCaches(Arrays.asList(
				new CaffeineCache(TKAT020_CACHE, Caffeine.newBuilder()
						.expireAfterWrite(0, TimeUnit.MINUTES)
						.maximumSize(0)
						.build())
		));
		return manager;
	}
}
