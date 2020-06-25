package no.nav.dokdistfordeling;


import static no.nav.dokdistfordeling.constants.Constants.CALL_ID;
import static no.nav.dokdistfordeling.constants.Constants.CONSUMER_ID;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.Authorization;
import lombok.extern.slf4j.Slf4j;
import no.nav.dokdistfordeling.exception.functional.BrukerManglerTilgangTilDokumentFunctionalException;
import no.nav.dokdistfordeling.exception.functional.DokkatGetDokumenttypeInfoFunctionalException;
import no.nav.dokdistfordeling.exception.functional.InvalidMappingToEnumFunctionalException;
import no.nav.dokdistfordeling.exception.functional.PersonErDoedUkjentAdresseException;
import no.nav.dokdistfordeling.exception.functional.SafJournalpostIkkeFunnetFunctionalException;
import no.nav.dokdistfordeling.exception.functional.SafJournalpostQueryUnauthorizedException;
import no.nav.dokdistfordeling.exception.functional.UkjentAdresseException;
import no.nav.dokdistfordeling.exception.functional.ValidationException;
import no.nav.dokdistfordeling.metrics.Monitor;
import no.nav.dokdistfordeling.swagger.SwaggerRestDistribuerJournalpost;
import org.slf4j.MDC;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("rest/v1/")
@Api(tags = "distribuerJournalpost API", description = "Tilbyr distribusjon av journalposter")
@Slf4j
public class DistribuerJournalpostController {

	private DistribuerJournalpostService distribuerJournalpostService;

	public DistribuerJournalpostController(DistribuerJournalpostService distribuerJournalpostService) {
		this.distribuerJournalpostService = distribuerJournalpostService;
	}

	@ApiOperation(value = "Bestiller distribusjon av en journalpost.", authorizations = {@Authorization(value = "apiKey")})
	@SwaggerRestDistribuerJournalpost
	@PostMapping(value = "/distribuerjournalpost")
	@Monitor(value = "dok_metric", process = "rdist002", extraTags = {"process", "rdist002"}, histogram = true)
	public ResponseEntity<DistribuerJournalpostResponseTo> distribuerJournalpost(@RequestBody DistribuerJournalpostRequestTo distribuerJournalpostRequestTo,
																				 @ApiParam(hidden = true) @RequestHeader(value = HttpHeaders.AUTHORIZATION) String authorizationHeader,
																				 @ApiParam(value = "Nav-CallId - teknisk sporingsid") @RequestHeader(value = "Nav-CallId", required = false) String navCallId,
																				 @ApiParam(value = "Nav-Consumer-Id - teknisk sporingsinfo om konsument") @RequestHeader(value = "Nav-Consumer-Id", required = false) String navConsumerId) {
		addCallIdToMDC(navCallId);
		addConsumerIdToMDC(navConsumerId);
		log.info("rdist002 har mottatt kall for journalpostId={}", distribuerJournalpostRequestTo.getJournalpostId());

		try {
			DistribuerJournalpostResponseTo response = new DistribuerJournalpostResponseTo(distribuerJournalpostService.distribuerForsendelse(distribuerJournalpostRequestTo, authorizationHeader));
			return ResponseEntity.ok().body(response);
		} catch (ValidationException | PersonErDoedUkjentAdresseException | UkjentAdresseException e) {
			log.warn("rdist002 - validering av distribusjonsforespørsel for journalpostId={} feilet, feilmelding: {}", distribuerJournalpostRequestTo.getJournalpostId(), e.getMessage());
			throw new ValidationException(String.format("validering av distribusjonsforespørsel for journalpostId=%s feilet, feilmelding=%s", distribuerJournalpostRequestTo.getJournalpostId(), e.getMessage()));
		} catch (SafJournalpostIkkeFunnetFunctionalException e) {
			log.warn("rdist002 - journalpost med journalpostId={} ble ikke funnet, feilmelding: {}", distribuerJournalpostRequestTo.getJournalpostId(), e.getMessage());
			throw new SafJournalpostIkkeFunnetFunctionalException(String.format("journalpost med journalpostId=%s ble ikke funnet", distribuerJournalpostRequestTo.getJournalpostId()));
		} catch (SafJournalpostQueryUnauthorizedException e) {
			log.warn("rdist002 - utilstrekkelig tilgang til journalpost med journalpostid={}, feilmelding: {}", distribuerJournalpostRequestTo.getJournalpostId(), e.getMessage());
			throw new SafJournalpostQueryUnauthorizedException(String.format("bruker har ikke tilgang til journalpost med journalpostId=%s", distribuerJournalpostRequestTo.getJournalpostId()));
		} catch (BrukerManglerTilgangTilDokumentFunctionalException e) {
			log.warn("rdist002 - bruker har ikke tilgang til noen av dokumentvariantene på journalposten med journalpostId={}, feilmelding: {}", distribuerJournalpostRequestTo.getJournalpostId(), e.getMessage());
			throw new BrukerManglerTilgangTilDokumentFunctionalException(String.format("bruker har ikke tilgang til noen av dokumentvariantene på journalposten med journalpostId=%s", distribuerJournalpostRequestTo.getJournalpostId()));
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
}