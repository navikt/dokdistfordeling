package no.nav.dokdistfordeling;


import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import no.nav.dokdistfordeling.consumer.saf.SafJournalpostQueryService;
import no.nav.dokdistfordeling.consumer.saf.journalpost.Journalpost;
import no.nav.dokdistfordeling.exception.functional.SafJournalpostQueryUnauthorizedException;
import no.nav.dokdistfordeling.exception.functional.ValidationException;
import no.nav.dokdistfordeling.metrics.Monitor;
import no.nav.dokdistfordeling.springdoc.SwaggerRestDistribuerJournalpost;
import org.slf4j.MDC;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static java.util.Objects.isNull;
import static java.util.UUID.randomUUID;
import static no.nav.dokdistfordeling.Rdist002ValidationUtil.validateDistribuerJournalpostRequest;
import static no.nav.dokdistfordeling.constants.Constants.CALL_ID;
import static no.nav.dokdistfordeling.constants.Constants.CONSUMER_ID;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.HttpStatus.CONFLICT;

@Slf4j
@RestController
@RequestMapping("rest/v1/")
@Tag(name = "distribuerJournalpost API", description = "Tilbyr distribusjon av journalposter")
public class DistribuerJournalpostController {

	private final SafJournalpostQueryService safJournalpostQueryService;
	private final DistribuerJournalpostService distribuerJournalpostService;

	private static final String RDIST002_PREFIX = "rdist002 - Distribusjon feilet for journalpostId= ";
	private static final String FEILMELDING_SUFFIX = ". Feilmelding: ";


	public DistribuerJournalpostController(SafJournalpostQueryService safJournalpostQueryService,
										   DistribuerJournalpostService distribuerJournalpostService) {
		this.safJournalpostQueryService = safJournalpostQueryService;
		this.distribuerJournalpostService = distribuerJournalpostService;
	}

	@SwaggerRestDistribuerJournalpost
	@PostMapping(value = "/distribuerjournalpost")
	@Monitor(value = "dok_metric", process = "rdist002", extraTags = {"process", "rdist002"}, histogram = true)
	public ResponseEntity<DistribuerJournalpostResponseTo> distribuerJournalpost(
			@RequestBody DistribuerJournalpostRequestTo distribuerJournalpostRequestTo,
			@Parameter(hidden = true) @RequestHeader(value = AUTHORIZATION) String authorizationHeader,
			@Parameter(description = "Nav-CallId - teknisk sporingsid") @RequestHeader(value = "Nav-CallId", required = false) String navCallId,
			@Parameter(description = "Nav-Consumer-Id - teknisk sporingsinfo om konsument") @RequestHeader(value = "Nav-Consumer-Id", required = false) String navConsumerId) throws Exception {

		addCallIdToMDC(navCallId);
		addConsumerIdToMDC(navConsumerId);
		log.info("rdist002 har mottatt kall for journalpostId={}", distribuerJournalpostRequestTo.getJournalpostId());

		try {
			validateDistribuerJournalpostRequest(distribuerJournalpostRequestTo);
			Journalpost journalpost = safJournalpostQueryService.hentJournalpost(distribuerJournalpostRequestTo.getJournalpostId(), authorizationHeader);

			if (!isTilleggsopplysningerNull(journalpost.getTilleggsopplysninger())) {
				final var bestillingsId = journalpost.getTilleggsopplysninger().getVerdi();
				log.info("Journalpost med journalpostId={} og bestillingsId={} er allerede distribuert", distribuerJournalpostRequestTo.getJournalpostId(), bestillingsId);
				return ResponseEntity.status(CONFLICT)
						.body(new DistribuerJournalpostResponseTo(bestillingsId));
			}

			DistribuerJournalpostResponseTo response = new DistribuerJournalpostResponseTo(
					distribuerJournalpostService.distribuerForsendelse(distribuerJournalpostRequestTo, journalpost));
			return ResponseEntity.ok().body(response);
		} catch (SafJournalpostQueryUnauthorizedException e) {
			log.warn(RDIST002_PREFIX + distribuerJournalpostRequestTo.getJournalpostId() + FEILMELDING_SUFFIX + e.getMessage());
			throw new SafJournalpostQueryUnauthorizedException("Saksbehandler har ikke tilgang til journalpost= " + distribuerJournalpostRequestTo.getJournalpostId() + " og kan derfor ikke bestille distribusjon. Feilmelding: " + e.getMessage(), e);
		} catch (ValidationException e) {
			log.warn(RDIST002_PREFIX + distribuerJournalpostRequestTo.getJournalpostId() + FEILMELDING_SUFFIX + e.getMessage());
			throw new ValidationException("Validering av distribusjonsforespørsel feilet med feilmelding: " + e.getMessage(), e);
		} catch (Exception e) {
			log.warn(RDIST002_PREFIX + distribuerJournalpostRequestTo.getJournalpostId() + FEILMELDING_SUFFIX + e.getMessage());
			throw e;
		} finally {
			MDC.clear();
		}
	}

	private void addCallIdToMDC(String callId) {
		if (callId == null || callId.isEmpty()) {
			callId = randomUUID().toString();
		}
		MDC.put(CALL_ID, callId);
	}

	private void addConsumerIdToMDC(String consumerId) {
		if (consumerId != null && !consumerId.isEmpty()) {
			MDC.put(CONSUMER_ID, consumerId);
		}
	}

	private Boolean isTilleggsopplysningerNull(Journalpost.Tilleggsopplysninger tilleggsopplysninger) {
		return isNull(tilleggsopplysninger) || isBlank(tilleggsopplysninger.getNokkel());
	}
}