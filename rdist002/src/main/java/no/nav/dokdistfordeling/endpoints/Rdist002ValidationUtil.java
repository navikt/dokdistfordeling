package no.nav.dokdistfordeling.endpoints;

import static no.nav.dokdistfordeling.endpoints.HentDokumenterFraJoarkMapper.NORSK_POSTADRESSE;
import static no.nav.dokdistfordeling.endpoints.HentDokumenterFraJoarkMapper.UTENLANDSK_POSTADRESSE;
import static no.nav.dokdistfordeling.util.ValidationUtil.assertNotNull;
import static no.nav.dokdistfordeling.util.ValidationUtil.assertNotNullOrEmpty;
import static no.nav.dokdistfordeling.util.ValidationUtil.assertParameterIsAsExpected;

import no.nav.dokdistfordeling.consumer.saf.journalpost.AvsenderMottaker;
import no.nav.dokdistfordeling.consumer.saf.journalpost.Bruker;
import no.nav.dokdistfordeling.consumer.saf.journalpost.DokumentInfo;
import no.nav.dokdistfordeling.consumer.saf.journalpost.Journalpost;
import no.nav.dokdistfordeling.exception.functional.ValidationException;
import no.nav.dokdistfordeling.kodeverk.BrukerIdType;
import no.nav.dokdistfordeling.kodeverk.Journalstatus;
import no.nav.dokdistfordeling.kodeverk.Variantformat;
import no.nav.dokdistfordeling.melding.qdist012.Aktoer;
import no.nav.dokdistfordeling.melding.qdist012.Samhandler;
import no.nav.tjeneste.domene.brevogarkiv.arkiverdokumentproduksjon.v1.informasjon.arkiverdokumentproduksjon.JournalpostType;

public class Rdist002ValidationUtil {

	private static final String UTGAAENDE = JournalpostType.U.name();
	private static final String FERDIGSTILT = Journalstatus.FERDIGSTILT.name();

	public void validateRequest(DistribuerJournalpostRequestTo distribuerJournalpostRequestTo) {
		try {
			assertNotNullOrEmpty("journalpostId", distribuerJournalpostRequestTo.getJournalpostId());
			assertNotNullOrEmpty("bestillendeFagsystem", distribuerJournalpostRequestTo.getBestillendeFagsystem());
			assertNotNullOrEmpty("dokumentProdapp", distribuerJournalpostRequestTo.getDokumentProdApp());
		} catch (ValidationException e) {
			throw new ValidationException(String.format("validering av distribuerJournalpostRequest feilet grunnet: %s", e.getMessage()), e);
		}
	}

	public void validateAdresse(DistribuerJournalpostRequestTo.AdresseTo adresseTo, Aktoer mottaker) {

		try {
			if (mottaker instanceof Samhandler) {
				assertNotNull(DistribuerJournalpostRequestTo.AdresseTo.class, adresseTo);
			}

			if (adresseTo != null) {
				assertNotNullOrEmpty("land", adresseTo.getLand());

				switch (adresseTo.getAdresseType()) {
					case NORSK_POSTADRESSE:
						assertNotNullOrEmpty("poststed", adresseTo.getPoststed());
						assertNotNullOrEmpty("postnummer", adresseTo.getPostnummer());
						break;
					case UTENLANDSK_POSTADRESSE:
						assertNotNullOrEmpty("adresselinje1", adresseTo.getAdresselinje1());
						break;
					default:
						throw new ValidationException(String.format("AdresseType må være enten norskPostadresse eller utenlandskPostadresse, mottok %s", adresseTo.getAdresseType()));
				}
			}
		} catch (ValidationException e) {
			throw new ValidationException("Validering av adresse for mottaker feilet.", e);
		}
	}

	public void validateJournalpostAndDokumenter(Journalpost journalpost) {
		try {
			assertParameterIsAsExpected("journalposttype", journalpost.getJournalposttype().name(), UTGAAENDE);
			assertParameterIsAsExpected("journalpoststatus", journalpost.getJournalstatus().name(), FERDIGSTILT);

			assertNotNull(Bruker.class, journalpost.getBruker());
			assertNotNullOrEmpty("brukerId", journalpost.getBruker().getId());
			assertNotNull(BrukerIdType.class, journalpost.getBruker().getType());

			assertNotNull(AvsenderMottaker.class, journalpost.getAvsenderMottaker());
			assertNotNullOrEmpty("mottakerId", journalpost.getAvsenderMottaker().getId());

			validateHovedDokumentInfo(journalpost.getDokumenter().iterator().next());


			journalpost.getDokumenter().forEach(this::validateDokumentInfo);

		} catch (ValidationException e) {
			throw new ValidationException(String.format("Validering av journalposten feilet grunnet: %s", e.getMessage()), e);
		}
	}

	public void validateHovedDokumentInfo(DokumentInfo dokumentInfo) {
		assertNotNullOrEmpty("tittel", dokumentInfo.getTittel());
		assertNotNullOrEmpty("brevkode", dokumentInfo.getBrevkode());
	}

	public void validateDokumentInfo(DokumentInfo dokumentInfo) {
		assertParameterIsAsExpected("dokumentstatus", dokumentInfo.getDokumentstatus().name(), FERDIGSTILT);

		if (dokumentInfo.getDokumentvarianter().stream().noneMatch(dokInfo -> dokInfo.isSaksbehandlerHarTilgang() && (dokInfo.getVariantformat() == Variantformat.ARKIV || dokInfo.getVariantformat() == Variantformat.SLADDET))) {
			throw new ValidationException("ingen variantformater av dokumentet med tilgang for saksbehandler ble funnet.");
		}
	}
}
