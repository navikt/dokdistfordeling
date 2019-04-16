package no.nav.dokdistfordeling.endpoints;

import static org.apache.commons.lang3.StringUtils.isBlank;

import no.nav.dokdistfordeling.config.jms.HentDokumenterFraJoarkProducer;
import no.nav.dokdistfordeling.consumer.saf.SafJournalpostQueryService;
import no.nav.dokdistfordeling.consumer.saf.journalpost.DokumentInfo;
import no.nav.dokdistfordeling.consumer.saf.journalpost.Journalpost;
import no.nav.dokdistfordeling.exception.functional.ValidationException;
import no.nav.dokdistfordeling.kodeverk.Dokumentstatus;
import no.nav.dokdistfordeling.kodeverk.Journalstatus;
import no.nav.dokdistfordeling.qdist012.HentDokumenterFraJoark;
import no.nav.tjeneste.domene.brevogarkiv.arkiverdokumentproduksjon.v1.informasjon.arkiverdokumentproduksjon.JournalpostType;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
public class DistribuerJournalpostService {

	private static final String NORSK_POSTADRESSE = "norskPostadresse";
	private static final String UTENLANDSK_POSTADRESSE = "utenlandskPostadresse";

	private SafJournalpostQueryService safJournalpostQueryService;
	private HentDokumenterFraJoarkProducer hentDokumenterFraJoarkProducer;


	public DistribuerJournalpostService(SafJournalpostQueryService safJournalpostQueryService) {
		this.safJournalpostQueryService = safJournalpostQueryService;
	}

	public String distribuerForsendelse(DistribuerJournalpostRequestTo distribuerJournalpostRequestTo, String authorizationHeader) {

		// steg 1, validering av request
		validateRequest(distribuerJournalpostRequestTo);

		// steg 2, hent journalpost fra saf
		Journalpost journalpost = safJournalpostQueryService.hentJournalpost(distribuerJournalpostRequestTo.getJournalpostId(), authorizationHeader);

		validateJournalpost(journalpost);
		// steg 3, validering av journalpost
		// steg 4, kontroller dokument på journalpost
		// steg 5, kontroller dokumenttype

		// todo steg 6, bestillJournalpotsDistribusjon

		hentDokumenterFraJoarkProducer.produce(new HentDokumenterFraJoark()); // todo populate

		String bestillingsId = "this has to be received back, but how?";
		//todo steg 7, returner bestillingsid til bestiller
		return bestillingsId;
	}


	private void validateRequest(DistribuerJournalpostRequestTo distribuerJournalpostRequestTo) {
		try {
			assertNotNullOrEmpty(distribuerJournalpostRequestTo.getJournalpostId(), "journalpostId");
			assertNotNullOrEmpty(distribuerJournalpostRequestTo.getBestillendeFagsystem(), "bestillendeFagsystem");
			assertNotNullOrEmpty(distribuerJournalpostRequestTo.getDokumentProdApp(), "dokumentProdapp");

			// todo assert adresse avhenger av om mottaker er samhandler. Hvordan skal vi vite det om ikke eksplisitt i input?
			DistribuerJournalpostRequestTo.AdresseTo adresse = distribuerJournalpostRequestTo.getAdresse();

			assertNotNullOrEmpty(distribuerJournalpostRequestTo.getAdresse().getLand(), "adresse.land");

			switch (distribuerJournalpostRequestTo.getAdresse().getAdresseType()) {
				case NORSK_POSTADRESSE:
					assertNotNullOrEmpty(adresse.getPoststed(), "adresse.poststed for norsk postadresse");
					assertNotNullOrEmpty(adresse.getPostnummer(), "adresse.postnummer for norsk postadresse");
					break;
				case UTENLANDSK_POSTADRESSE:
					assertNotNullOrEmpty(adresse.getAdresselinje1(), "adresse.adresselinje1 for utenlands postadresse");
					break;
				default:
					throw new IllegalArgumentException(String.format("AdresseType må være enten \"norskPostadresse\" eller \"utenlandskPostadresse\", mottok %s", adresse.getAdresseType()));
			}

		} catch (IllegalArgumentException e) {
			throw new ValidationException("Validering av distribuerJournalpostRequest feilet.", e);
		}
	}

	private void validateJournalpost(Journalpost journalpost) {
		try {
			forParameterAssertEquals("journalposttype", journalpost.getJournalposttype().name(), JournalpostType.U.name());
			forParameterAssertEquals("journalpoststatus", journalpost.getJournalstatus().name(), Journalstatus.FERDIGSTILT.name());
			assertNotNullOrEmpty(journalpost.getBruker(), "bruker");
			assertNotNullOrEmpty(journalpost.getAvsenderMottaker(), "avsenderMottaker");

			validateHovedDokumentInfo(journalpost.getDokumenter().iterator().next());

			journalpost.getDokumenter().forEach(this::validateVedleggDokumentInfo);

		} catch (IllegalArgumentException e) {
			throw new ValidationException("Validering av distribuerJournalpostRequest feilet.", e);
		}
	}

	private void validateHovedDokumentInfo(DokumentInfo dokumentInfo) {
		assertNotNullOrEmpty(dokumentInfo.getTittel(), "dokumentinfo.tittel");
		assertNotNullOrEmpty(dokumentInfo.getBrevkode(), "dokumentinfo.brevkode");
	}

	private void validateVedleggDokumentInfo(DokumentInfo dokumentInfo) {
		forParameterAssertEquals("dokumentinfo.dokumentstatus", dokumentInfo.getDokumentstatus().name(), Dokumentstatus.FERDIGSTILT.name());
//		forParameterAssertEquals("dokumentinfo.dokumentvariant", dokumentInfo.getDokumentvarianter().get(0).getVariantformat().name(), Variantformat.ARKIV.name());
		// todo await more documentation.
	}

	private void assertNotNullOrEmpty(Object value, String parameter) {
		if (Objects.isNull(value) || (value instanceof String && isBlank((String) value))) {
			throw new IllegalArgumentException(String.format("Input mangler påkrevd parameter \"%s\"", parameter));
		}
	}

	private void forParameterAssertEquals(String parameterName, String value, String expected) {
		if (!value.equals(expected)) {
			throw new IllegalArgumentException(String.format("%s er ikke som forventet, fikk: \"%s\", men forventet \"%s\"", parameterName, value, expected));
		}
	}
}
