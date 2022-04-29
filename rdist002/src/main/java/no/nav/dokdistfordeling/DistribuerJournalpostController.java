package no.nav.dokdistfordeling;


import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import no.nav.dokdistfordeling.consumer.saf.SafJournalpostQueryService;
import no.nav.dokdistfordeling.consumer.saf.journalpost.Journalpost;
import no.nav.dokdistfordeling.exception.functional.BrukerManglerTilgangTilDokumentFunctionalException;
import no.nav.dokdistfordeling.exception.functional.DokkatGetDokumenttypeInfoFunctionalException;
import no.nav.dokdistfordeling.exception.functional.InvalidMappingToEnumFunctionalException;
import no.nav.dokdistfordeling.exception.functional.PersonErDoedUkjentAdresseException;
import no.nav.dokdistfordeling.exception.functional.SafJournalpostQueryUnauthorizedException;
import no.nav.dokdistfordeling.exception.functional.UkjentAdresseException;
import no.nav.dokdistfordeling.exception.functional.ValidationException;
import no.nav.dokdistfordeling.exception.technical.SafJournalpostIkkeFunnetTechnicalException;
import no.nav.dokdistfordeling.metrics.Monitor;
import no.nav.dokdistfordeling.springdoc.SwaggerRestDistribuerJournalpost;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

import static java.util.Objects.isNull;
import static no.nav.dokdistfordeling.constants.Constants.CALL_ID;
import static no.nav.dokdistfordeling.constants.Constants.CONSUMER_ID;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.springframework.http.HttpStatus.GONE;

@RestController
@RequestMapping("rest/v1/")
@Tag(name="distribuerJournalpost API", description = "Tilbyr distribusjon av journalposter")
@Slf4j
public class DistribuerJournalpostController {

	private final DistribuerJournalpostService distribuerJournalpostService;
	private final SafJournalpostQueryService safJournalpostQueryService;
	private final Rdist002ValidationUtil rdist002ValidationUtil;

	@Autowired
	public DistribuerJournalpostController(DistribuerJournalpostService distribuerJournalpostService,
										   SafJournalpostQueryService safJournalpostQueryService) {
		this.distribuerJournalpostService = distribuerJournalpostService;
		this.safJournalpostQueryService = safJournalpostQueryService;
		this.rdist002ValidationUtil = new Rdist002ValidationUtil();
	}

	@SwaggerRestDistribuerJournalpost
	@PostMapping(value = "/distribuerjournalpost")
	@Monitor(value = "dok_metric", process = "rdist002", extraTags = {"process", "rdist002"}, histogram = true)
	public ResponseEntity<DistribuerJournalpostResponseTo> distribuerJournalpost(@RequestBody DistribuerJournalpostRequestTo distribuerJournalpostRequestTo,
																				 @Parameter(hidden = true) @RequestHeader(value = HttpHeaders.AUTHORIZATION) String authorizationHeader,
																				 @Parameter(description = "Nav-CallId - teknisk sporingsid") @RequestHeader(value = "Nav-CallId", required = false) String navCallId,
																				 @Parameter(description = "Nav-Consumer-Id - teknisk sporingsinfo om konsument") @RequestHeader(value = "Nav-Consumer-Id", required = false) String navConsumerId) {
		addCallIdToMDC(navCallId);
		addConsumerIdToMDC(navConsumerId);
		log.info("rdist002 har mottatt kall for journalpostId={}", distribuerJournalpostRequestTo.getJournalpostId());

		try {
			rdist002ValidationUtil.validateRequest(distribuerJournalpostRequestTo);
			Journalpost journalpost = safJournalpostQueryService.hentJournalpost(distribuerJournalpostRequestTo.getJournalpostId(), authorizationHeader);

			if(!isTilleggsopplysningerNull(journalpost.getTilleggsopplysninger())) {
				final var bestillingsId = journalpost.getTilleggsopplysninger().getVerdi();
				log.info("Journalpost med journalpostId={} og bestillingsId={} er allerede distribuert", distribuerJournalpostRequestTo.getJournalpostId(), bestillingsId);
				return ResponseEntity.status(HttpStatus.CONFLICT)
						.body(new DistribuerJournalpostResponseTo(bestillingsId));
			}

			DistribuerJournalpostResponseTo response = new DistribuerJournalpostResponseTo(
					distribuerJournalpostService.distribuerForsendelse(distribuerJournalpostRequestTo, journalpost));
			return ResponseEntity.ok().body(response);
		} catch (ValidationException | UkjentAdresseException e) {
			log.warn("rdist002 - validering av distribusjonsforespørsel for journalpostId={} feilet, feilmelding: {}", distribuerJournalpostRequestTo.getJournalpostId(), e.getMessage());
			throw new ValidationException(String.format("Validering av distribusjonsforespørsel for journalpostId=%s feilet. %s", distribuerJournalpostRequestTo.getJournalpostId(), e.getMessage()));
		} catch (PersonErDoedUkjentAdresseException e) {
			log.warn("rdist002 - Mottaker er død og har ukjent adresse. status={}, journalpostId={} feilet,feilmelding: {}", GONE, distribuerJournalpostRequestTo.getJournalpostId(), e.getMessage());
			throw e;
		} catch (SafJournalpostIkkeFunnetTechnicalException e) {
			log.warn("rdist002 - journalpost med journalpostId={} ble ikke funnet, feilmelding: {}", distribuerJournalpostRequestTo.getJournalpostId(), e.getMessage());
			throw e;
		} catch (SafJournalpostQueryUnauthorizedException e) {
			log.warn("rdist002 - utilstrekkelig tilgang til journalpost med journalpostid={}, feilmelding: {}", distribuerJournalpostRequestTo.getJournalpostId(), e.getMessage());
			throw new SafJournalpostQueryUnauthorizedException(String.format("Bruker har ikke tilgang til journalpost med journalpostId=%s", distribuerJournalpostRequestTo.getJournalpostId()));
		} catch (BrukerManglerTilgangTilDokumentFunctionalException e) {
			log.warn("rdist002 - bruker har ikke tilgang til noen av dokumentvariantene på journalposten med journalpostId={}, feilmelding: {}", distribuerJournalpostRequestTo.getJournalpostId(), e.getMessage());
			throw new BrukerManglerTilgangTilDokumentFunctionalException(String.format("Bruker har ikke tilgang til noen av dokumentvariantene på journalposten med journalpostId=%s", distribuerJournalpostRequestTo.getJournalpostId()));
		} catch (DokkatGetDokumenttypeInfoFunctionalException e) {
			log.warn("rdist002 - Ugyldig dokumenttypeid på hoveddokumentet for journalpost med journalpostid={}, feilmelding: {}", distribuerJournalpostRequestTo.getJournalpostId(), e.getMessage());
			throw new DokkatGetDokumenttypeInfoFunctionalException(String.format("Ugyldig dokumenttypeid på hoveddokumentet for journalpost med journalpostid=%s", distribuerJournalpostRequestTo.getJournalpostId()));
		} catch (InvalidMappingToEnumFunctionalException e) {
			log.warn("rdist002 - Uventet verdi ble forsøkt mappet til enum, for journalpost med journalpostId={}, feilmelding: {}", distribuerJournalpostRequestTo.getJournalpostId(), e.getMessage());
			throw new InvalidMappingToEnumFunctionalException(String.format("Uventet verdi ble forsøkt mappet til enum, for journalpost med journalpostId=%s, feilmelding: %s", distribuerJournalpostRequestTo.getJournalpostId(), e.getMessage()));
		} catch (Exception e) {
			log.warn("rdist002 - feilet ved distribusjon av journalpost med journalpostId={}, feilmelding: {}", distribuerJournalpostRequestTo.getJournalpostId(), e.getMessage());
			throw e;
		} finally {
			MDC.clear();
		}
	}

	private void addCallIdToMDC(String callId) {
		if (callId == null || callId.isEmpty()) {
			callId = UUID.randomUUID().toString();
		}
		MDC.put(CALL_ID, callId);
	}

	private void addConsumerIdToMDC(String consumerId) {
		if (consumerId != null && !consumerId.isEmpty()) {
			MDC.put(CONSUMER_ID, consumerId);
		}
	}

	private Boolean isTilleggsopplysningerNull(Journalpost.Tilleggsopplysninger tilleggsopplysninger) {
		return  isNull(tilleggsopplysninger) || isBlank(tilleggsopplysninger.getNokkel());
	}
}