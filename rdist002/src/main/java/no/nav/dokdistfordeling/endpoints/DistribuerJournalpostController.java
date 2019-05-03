package no.nav.dokdistfordeling.endpoints;


import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.Authorization;
import lombok.extern.slf4j.Slf4j;
import no.nav.dokdistfordeling.endpoints.swagger.SwaggerRestDistribuerJournalpost;
import no.nav.dokdistfordeling.exception.functional.BrukerManglerTilgangTilDokumentFunctionalException;
import no.nav.dokdistfordeling.exception.functional.SafJournalpostIkkeFunnetFunctionalException;
import no.nav.dokdistfordeling.exception.functional.SafJournalpostQueryUnauthorizedException;
import no.nav.dokdistfordeling.exception.functional.ValidationException;
import no.nav.dokdistfordeling.exception.technical.DokkatGetDokumenttypeInfoTechnicalException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
	public ResponseEntity<DistribuerJournalpostResponseTo> distribuerJournalpost(@RequestBody DistribuerJournalpostRequestTo distribuerJournalpostRequestTo,
																				 @ApiParam(hidden = true) @RequestHeader(value = HttpHeaders.AUTHORIZATION) String authorizationHeader) {
		log.info("rdist002 har mottatt kall for journalpostId={}", distribuerJournalpostRequestTo.getJournalpostId());

		try {
			DistribuerJournalpostResponseTo response = new DistribuerJournalpostResponseTo(distribuerJournalpostService.distribuerForsendelse(distribuerJournalpostRequestTo, authorizationHeader));
			return ResponseEntity.ok().body(response);

		} catch (ValidationException e) {
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

		} catch (DokkatGetDokumenttypeInfoTechnicalException e) {
			log.warn("rdist002 - Ugyldig dokumenttypeid på hoveddokumentet for journalpost med journalpostid={}, feilmelding: {}", distribuerJournalpostRequestTo.getJournalpostId(), e.getMessage());
			throw new DokkatGetDokumenttypeInfoTechnicalException(String.format("Ugyldig dokumenttypeid på hoveddokumentet for journalpost med journalpostid=%s", distribuerJournalpostRequestTo.getJournalpostId()));

		} catch (Exception e) {
			log.warn("rdist002 - feilet ved distribusjon av journalpost med journalpostId={}, feilmelding: {}", distribuerJournalpostRequestTo.getJournalpostId(), e.getMessage());
			throw e;
		}
	}
}