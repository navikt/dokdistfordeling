package no.nav.dokdistfordeling.endpoints;


import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("rest/v1")
public class DistribuerJournalpostController {

	private DistribuerJournalpostService distribuerJournalpostService;

	public DistribuerJournalpostController(DistribuerJournalpostService distribuerJournalpostService) {
		this.distribuerJournalpostService = distribuerJournalpostService;
	}

	@PostMapping(value = "/distribuerjournalpost", consumes = MediaType.APPLICATION_JSON_UTF8_VALUE, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
	@ResponseBody
	public DistribuerJournalpostResponseTo distribuerJournalpost(@RequestBody DistribuerJournalpostRequestTo distribuerJournalpostRequestTo,
																 @RequestHeader(value = HttpHeaders.AUTHORIZATION) String authorizationHeader) {
		return new DistribuerJournalpostResponseTo(distribuerJournalpostService.distribuerForsendelse(distribuerJournalpostRequestTo, authorizationHeader));
	}
}