package no.nav.dokdistfordeling;

import static no.nav.dokdistfordeling.constants.ValidationConstants.FERDIGSTILT;
import static no.nav.dokdistfordeling.HentDokumenterFraJoarkMapper.NORSK_POSTADRESSE;
import static no.nav.dokdistfordeling.HentDokumenterFraJoarkMapper.UTENLANDSK_POSTADRESSE;
import static no.nav.dokdistfordeling.util.ValidationUtil.assertNotNull;
import static no.nav.dokdistfordeling.util.ValidationUtil.assertNotNullOrEmpty;
import static no.nav.dokdistfordeling.util.ValidationUtil.assertParameterIsAsExpected;

import no.nav.dokdistfordeling.consumer.saf.journalpost.Journalpost;
import no.nav.dokdistfordeling.exception.functional.BrukerManglerTilgangTilDokumentFunctionalException;
import no.nav.dokdistfordeling.exception.functional.ValidationException;
import no.nav.dokdistfordeling.kodeverk.BrukerIdType;
import no.nav.dokdistfordeling.kodeverk.Variantformat;
import no.nav.meldinger.virksomhet.dokdistfordeling.qdist012.Aktoer;
import no.nav.meldinger.virksomhet.dokdistfordeling.qdist012.Samhandler;
import no.nav.tjeneste.domene.brevogarkiv.arkiverdokumentproduksjon.v1.informasjon.arkiverdokumentproduksjon.JournalpostType;

public class Rdist002ValidationUtil {

	private static final String UTGAAENDE = JournalpostType.U.name();

	public void validateRequest(DistribuerJournalpostRequestTo distribuerJournalpostRequestTo) {
		assertNotNullOrEmpty("journalpostId", distribuerJournalpostRequestTo.getJournalpostId());
		assertNotNullOrEmpty("bestillendeFagsystem", distribuerJournalpostRequestTo.getBestillendeFagsystem());
		assertNotNullOrEmpty("dokumentProdapp", distribuerJournalpostRequestTo.getDokumentProdApp());
	}

	public void validateAdresse(DistribuerJournalpostRequestTo.AdresseTo adresseTo, Aktoer mottaker) {
		if (mottaker instanceof Samhandler && adresseTo == null) {
			throw new ValidationException("For mottaker av type samhandler kan ikke adresse være null");
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
	}

	public void validateJournalpostAndDokumenter(Journalpost journalpost) {
		assertNotNull(JournalpostType.class, journalpost.getJournalposttype());
		assertParameterIsAsExpected("journalposttype", journalpost.getJournalposttype().name(), UTGAAENDE);
		assertParameterIsAsExpected("journalpoststatus", journalpost.getJournalstatus(), FERDIGSTILT);

		assertNotNull(Journalpost.Bruker.class, journalpost.getBruker());
		assertNotNullOrEmpty("brukerId", journalpost.getBruker().getId());
		assertNotNull(BrukerIdType.class, journalpost.getBruker().getType());

		assertNotNull(Journalpost.AvsenderMottaker.class, journalpost.getAvsenderMottaker());
		assertNotNullOrEmpty("mottakerId", journalpost.getAvsenderMottaker().getId());

		validateHovedDokumentInfo(journalpost.getDokumenter().iterator().next());

		journalpost.getDokumenter().forEach(this::validateDokumentInfo);
	}

	private void validateHovedDokumentInfo(Journalpost.DokumentInfo dokumentInfo) {
		assertNotNullOrEmpty("tittel", dokumentInfo.getTittel());
		assertNotNullOrEmpty("brevkode", dokumentInfo.getBrevkode());
	}

	private void validateDokumentInfo(Journalpost.DokumentInfo dokumentInfo) {
		assertParameterIsAsExpected("dokumentstatus", dokumentInfo.getDokumentstatus(), FERDIGSTILT);

		if (dokumentInfo.getDokumentvarianter().stream().noneMatch(dokInfo -> dokInfo.isSaksbehandlerHarTilgang() && (Variantformat.ARKIV.equals(dokInfo.getVariantformat()) || Variantformat.SLADDET.equals(dokInfo.getVariantformat())))) {
			throw new BrukerManglerTilgangTilDokumentFunctionalException("ingen variantformater av dokumentet med tilgang for saksbehandler ble funnet.");
		}
	}
}
