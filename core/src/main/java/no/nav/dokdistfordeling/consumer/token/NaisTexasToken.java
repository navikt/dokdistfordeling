package no.nav.dokdistfordeling.consumer.token;

import com.fasterxml.jackson.annotation.JsonProperty;

record NaisTexasToken(@JsonProperty("access_token") String accessToken) {
}
