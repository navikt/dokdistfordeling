package no.nav.dokdistfordeling.consumer.pdl;

import lombok.extern.slf4j.Slf4j;
import no.nav.dokdistfordeling.config.props.PdlProperties;
import no.nav.dokdistfordeling.consumer.sts.StsRestConsumer;
import no.nav.dokdistfordeling.exception.functional.PdlHentFolkeregisteridentForAktoerIdFunctionalException;
import no.nav.dokdistfordeling.exception.functional.PdlPersonIkkeFunnetFunctionalException;
import no.nav.dokdistfordeling.exception.technical.PdlHentFolkeregisteridentForAktoerIdTechnicalException;
import org.slf4j.MDC;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.Duration;
import java.util.HashMap;
import java.util.Optional;

import static java.util.Objects.requireNonNull;
import static no.nav.dokdistfordeling.consumer.NavHeaders.NAV_CALL_ID;
import static no.nav.dokdistfordeling.consumer.pdl.IdentType.FOLKEREGISTERIDENT;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.MediaType.APPLICATION_JSON;

@Slf4j
@Component
public class PdlGraphQLConsumer {

    private static final String PERSON_IKKE_FUNNET_CODE = "not_found";
    private static final String HEADER_PDL_NAV_CONSUMER_TOKEN = "Nav-Consumer-Token";
    private final RestTemplate restTemplate;
    private final StsRestConsumer stsConsumer;
    private final String pdlUrl;

    public PdlGraphQLConsumer(RestTemplateBuilder restTemplateBuilder,
                              StsRestConsumer stsConsumer,
                              PdlProperties pdlProperties) {
        this.restTemplate = restTemplateBuilder
                .setConnectTimeout(Duration.ofSeconds(3))
                .setReadTimeout(Duration.ofSeconds(20))
                .build();
        this.stsConsumer = stsConsumer;
        this.pdlUrl = pdlProperties.getUrl();
    }

    @Retryable(retryFor = HttpServerErrorException.class)
    public String hentFolkeregisteridentForAktoerId(final String aktorId) {
        try {
            final UriComponents uri = UriComponentsBuilder.fromHttpUrl(pdlUrl).build();
            final String serviceuserToken = "Bearer " + stsConsumer.getOidcToken();
            final RequestEntity<PdlRequest> requestEntity = RequestEntity.post(uri.toUri())
                    .accept(APPLICATION_JSON)
                    .header(CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .header(AUTHORIZATION, serviceuserToken)
                    .header(HEADER_PDL_NAV_CONSUMER_TOKEN, serviceuserToken)
                    .header(NAV_CALL_ID, MDC.get(NAV_CALL_ID))
                    .body(mapRequest(aktorId));
            final PdlHentIdenterResponse pdlHentIdenterResponse = requireNonNull(restTemplate.exchange(requestEntity, PdlHentIdenterResponse.class).getBody());
            if (pdlHentIdenterResponse.getErrors() == null || pdlHentIdenterResponse.getErrors().isEmpty()) {
                return getFolkeregisteridentFromResponse(pdlHentIdenterResponse);
            } else {
                if (PERSON_IKKE_FUNNET_CODE.equals(pdlHentIdenterResponse.getErrors().get(0).getExtensions().getCode())) {
                    throw new PdlPersonIkkeFunnetFunctionalException("Fant ikke folkeregisterident for person i PDL.");
                }
                throw new PdlHentFolkeregisteridentForAktoerIdFunctionalException("Kunne ikke hente folkeregisterident fra PDL." + pdlHentIdenterResponse.getErrors());
            }
        } catch (HttpClientErrorException e) {
            throw new PdlHentFolkeregisteridentForAktoerIdFunctionalException("Funksjonell feil ved kall mot PDL.", e);
        } catch (HttpServerErrorException e) {
            throw new PdlHentFolkeregisteridentForAktoerIdTechnicalException("Teknisk feil ved kall mot PDL.", e);
        }
    }

    private String getFolkeregisteridentFromResponse(PdlHentIdenterResponse pdlHentIdenterResponse){
        return Optional.ofNullable(pdlHentIdenterResponse.getData())
                .map(PdlHentIdenterResponse.PdlHentIdenterData::getHentIdenter)
                .map(PdlHentIdenterResponse.PdlIdenter::getIdenter)
                .flatMap(identer -> identer.stream()
                        .filter(it -> it.getGruppe() == FOLKEREGISTERIDENT)
                        .filter(it -> !it.isHistorisk())
                        .map(PdlHentIdenterResponse.PdlIdentTo::getIdent)
                        .findFirst())
                .orElseThrow(()-> new PdlHentFolkeregisteridentForAktoerIdFunctionalException("Kunne ikke hente folkeregisterident fra PDL. Respons fra PDL inneholdt ikke gjeldende folkeregisterident"));
    }

    private PdlRequest mapRequest(final String aktoerId) {
        final HashMap<String, Object> variables = new HashMap<>();
        variables.put("ident", aktoerId);
        return PdlRequest.builder()
                .query("""
                        query($ident: ID!) {
                          hentIdenter(ident: $ident, historikk: false, grupper: FOLKEREGISTERIDENT) {
                            identer {
                              ident
                              historisk
                              gruppe
                            }
                          }
                        }
                        """)
                .variables(variables)
                .build();
    }
}
