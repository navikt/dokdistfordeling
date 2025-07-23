package no.nav.dokdistfordeling;


import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import no.nav.dokdistfordeling.consumer.saf.journalpost.SafJournalpostQueryService;
import no.nav.dokdistfordeling.consumer.saf.journalpost.Journalpost;
import no.nav.dokdistfordeling.domain.DistribuerJournalpost;
import no.nav.dokdistfordeling.exception.functional.ValidationException;
import no.nav.dokdistfordeling.map.DistribuerJournalpostMapper;
import no.nav.dokdistfordeling.springdoc.SwaggerRestDistribuerJournalpost;
import no.nav.dokdistfordeling.to.DistribuerJournalpostRequestTo;
import no.nav.dokdistfordeling.to.DistribuerJournalpostResponseTo;
import no.nav.dokdistfordeling.util.SafeLoggingUtil;
import org.slf4j.MDC;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

import static java.util.UUID.randomUUID;
import static no.nav.dokdistfordeling.constants.Constants.CALL_ID;
import static no.nav.dokdistfordeling.constants.Constants.CONSUMER_ID;
import static no.nav.dokdistfordeling.validate.DistribuerJournalpostRequestValidator.validateDistribuerJournalpostRequest;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.HttpStatus.CONFLICT;

@Slf4j
@RestController
@RequestMapping("rest/v1/")
@Tag(name = "distribuerJournalpost API", description = "Tilbyr distribusjon av journalposter")
public class DistribuerJournalpostController {

	private final SafJournalpostQueryService safJournalpostQueryService;
	private final DistribuerJournalpostService distribuerJournalpostService;


	public DistribuerJournalpostController(SafJournalpostQueryService safJournalpostQueryService,
										   DistribuerJournalpostService distribuerJournalpostService) {
		this.safJournalpostQueryService = safJournalpostQueryService;
		this.distribuerJournalpostService = distribuerJournalpostService;
	}

	@SwaggerRestDistribuerJournalpost
	@PostMapping(value = "/distribuerjournalpost")
	public ResponseEntity<DistribuerJournalpostResponseTo> distribuerJournalpost(
			@RequestBody DistribuerJournalpostRequestTo distribuerJournalpostRequestTo,
			@Parameter(hidden = true) @RequestHeader(value = AUTHORIZATION) String authorizationHeader,
			@Parameter(description = "Nav-CallId - teknisk sporingsid") @RequestHeader(value = "Nav-CallId", required = false) String navCallId,
			@Parameter(description = "Nav-Consumer-Id - teknisk sporingsinfo om konsument") @RequestHeader(value = "Nav-Consumer-Id", required = false) String navConsumerId) {

		addCallIdToMDC(navCallId);
		addConsumerIdToMDC(navConsumerId);

		String journalpostId = SafeLoggingUtil.removeUnsafeChars(distribuerJournalpostRequestTo.getJournalpostId());
		log.info("rdist002 har mottatt kall for journalpostId={}", journalpostId);

		try {
			validateDistribuerJournalpostRequest(distribuerJournalpostRequestTo);
			Journalpost journalpost = safJournalpostQueryService.hentJournalpost(distribuerJournalpostRequestTo.getJournalpostId(), Optional.of(authorizationHeader));

			if (journalpost.erDistribuert()) {
				final var bestillingsId = journalpost.getTilleggsopplysninger().getVerdi();
				log.info("Journalpost med journalpostId={} og bestillingsId={} er allerede distribuert", journalpostId, bestillingsId);
				return ResponseEntity.status(CONFLICT)
						.body(new DistribuerJournalpostResponseTo(bestillingsId));
			}

			DistribuerJournalpost distribuerJournalpost = DistribuerJournalpostMapper.map(distribuerJournalpostRequestTo);

			String bestillingsId = distribuerJournalpostService.distribuerForsendelse(distribuerJournalpost, journalpost);

			DistribuerJournalpostResponseTo response = new DistribuerJournalpostResponseTo(bestillingsId);

			log.info("rdist002 har distribuert journalpost med journalpostId={} og bestillingsId={}", journalpostId, bestillingsId);

			return ResponseEntity.ok().body(response);
		} catch (Exception e) {
			log.warn("rdist002 distribusjon feilet for journalpostId={}. Feilmelding={}", journalpostId, e.getMessage(), e);
			if (e instanceof ValidationException validationException) {
				throw new ValidationException("Validering av distribusjonsforespørsel feilet med feilmelding: " + validationException.getMessage());
			} else {
				throw e;
			}
		} finally {
			MDC.clear();
		}
	}

	private void addCallIdToMDC(String callId) {
		if (isNotBlank(callId)) {
			MDC.put(CALL_ID, callId);
		}

		MDC.put(CALL_ID, randomUUID().toString());
	}

	private void addConsumerIdToMDC(String consumerId) {
		if (isNotBlank(consumerId)) {
			MDC.put(CONSUMER_ID, consumerId);
		}
	}
}