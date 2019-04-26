package no.nav.dokdistfordeling.endpoints;


import lombok.extern.slf4j.Slf4j;
import no.nav.dokdistfordeling.exception.functional.SafJournalpostIkkeFunnetFunctionalException;
import no.nav.dokdistfordeling.exception.functional.SafJournalpostQueryUnauthorizedException;
import no.nav.dokdistfordeling.exception.functional.ValidationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("rest/v1")
@Slf4j
public class DistribuerJournalpostController {

	private DistribuerJournalpostService distribuerJournalpostService;

	public DistribuerJournalpostController(DistribuerJournalpostService distribuerJournalpostService) {
		this.distribuerJournalpostService = distribuerJournalpostService;
	}

	@PostMapping(value = "/distribuerjournalpost", consumes = MediaType.APPLICATION_JSON_UTF8_VALUE, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
	@ResponseBody
	public ResponseEntity<DistribuerJournalpostResponseTo> distribuerJournalpost(@RequestBody DistribuerJournalpostRequestTo distribuerJournalpostRequestTo,
																				 @RequestHeader(value = HttpHeaders.AUTHORIZATION) String authorizationHeader) {
		log.info("rdist002 har mottatt kall for journalpostId={}", distribuerJournalpostRequestTo.getJournalpostId());

		try {
			DistribuerJournalpostResponseTo response = new DistribuerJournalpostResponseTo(distribuerJournalpostService.distribuerForsendelse(distribuerJournalpostRequestTo, authorizationHeader));
			return ResponseEntity.ok().body(response);

		} catch (ValidationException e) {
			log.warn("rdist002 - validering av distribusjonsforespørsel for journalpostId={} feilet, feilmelding: {}", distribuerJournalpostRequestTo.getJournalpostId(), e.getMessage());
			throw new ValidationException(String.format("rdist002 - validering av distribusjonsforespørsel for journalpostId=%s feilet, feilmelding: %s", distribuerJournalpostRequestTo.getJournalpostId(), e.getMessage()));

		} catch (SafJournalpostIkkeFunnetFunctionalException e) {
			log.warn("rdist002 - journalpost med journalpostId={} ble ikke funnet, feilmelding: {}", distribuerJournalpostRequestTo.getJournalpostId(), e.getMessage());
			throw new SafJournalpostIkkeFunnetFunctionalException(String.format("rdist002 - journalpost med journalpostId=%s ble ikke funnet, feilmelding: %s", distribuerJournalpostRequestTo.getJournalpostId(), e.getMessage()));

		} catch (SafJournalpostQueryUnauthorizedException e) {
			log.warn("rdist002 - utilstrekkelig tilgang til journalpost med journalpostid={}, feilmelding: {}", distribuerJournalpostRequestTo.getJournalpostId(), e.getMessage());
			throw new SafJournalpostQueryUnauthorizedException(String.format("rdist002 - utilstrekkelig tilgang til journalpost med journalpostid=%s, feilmelding: %s", distribuerJournalpostRequestTo.getJournalpostId(), e.getMessage()));

		} catch (Exception e) {

			log.warn("rdist002 - feilet ved distribusjon av journalpost med journalpostId={}, feilmelding: {}", distribuerJournalpostRequestTo.getJournalpostId(), e.getMessage());
			throw e;
		}
	}
}