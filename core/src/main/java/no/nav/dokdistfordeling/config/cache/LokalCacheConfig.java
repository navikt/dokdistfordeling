package no.nav.dokdistfordeling.config.cache;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

/**
 * @author Joakim Bjørnstad, Jbit AS
 */
@Configuration
@Profile({"nais", "local"})
public class LokalCacheConfig {

    public static final String TKAT020_CACHE = "tkat020Cache";
    public static final String OIDC_TOKEN_CACHE = "OidcTokenCache";
    public static final String SAML_TOKEN_CACHE = "SamlTokenCache";
    public static final String AZURE_TOKEN_CACHE = "AzureToken";

    @Bean
    @Primary
    CacheManager cacheManager() {
        SimpleCacheManager manager = new SimpleCacheManager();
        manager.setCaches(Arrays.asList(
                new CaffeineCache(TKAT020_CACHE, Caffeine.newBuilder()
                        .expireAfterWrite(1, TimeUnit.DAYS)
                        .build()),
                new CaffeineCache(OIDC_TOKEN_CACHE, Caffeine.newBuilder()
                        .expireAfterWrite(55, TimeUnit.MINUTES)
                        .build()),
                new CaffeineCache(SAML_TOKEN_CACHE, Caffeine.newBuilder()
                        .expireAfterWrite(55, TimeUnit.MINUTES)
                        .build()),
                new CaffeineCache(AZURE_TOKEN_CACHE, Caffeine.newBuilder()
                        .expireAfterWrite(55, TimeUnit.MINUTES)
                        .build())));
        return manager;
    }
}
