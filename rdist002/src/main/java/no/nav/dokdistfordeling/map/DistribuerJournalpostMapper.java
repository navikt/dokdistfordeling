package no.nav.dokdistfordeling.map;

import no.nav.dokdistfordeling.domain.DistribuerJournalpost;
import no.nav.dokdistfordeling.domain.Postadresse;
import no.nav.dokdistfordeling.kodeverk.DistribusjonstidspunktCode;
import no.nav.dokdistfordeling.kodeverk.DistribusjonstypeCode;
import no.nav.dokdistfordeling.kodeverk.ForsendelseMetadataType;
import no.nav.dokdistfordeling.kodeverk.TvingKanal;
import no.nav.dokdistfordeling.to.DistribuerJournalpostRequestTo;

import java.util.Base64;

import static org.apache.commons.lang3.StringUtils.isBlank;

public class DistribuerJournalpostMapper {

	private DistribuerJournalpostMapper() {
	}

	public static DistribuerJournalpost map(DistribuerJournalpostRequestTo request) {
		return new DistribuerJournalpost(
				Long.parseLong(request.getJournalpostId()),
				request.getBatchId(),
				request.getBestillendeFagsystem(),
				mapPostadresse(request.getAdresse()),
				request.getDokumentProdApp(),
				DistribusjonstypeCode.valueOf(request.getDistribusjonstype()),
				DistribusjonstidspunktCode.valueOf(request.getDistribusjonstidspunkt()),
				request.isTvingSentralPrint(),
				mapTvingKanal(request.getTvingKanal()),
				encodeToBase64(request.getForsendelseMetadata()),
				mapForsendelseMetadataType(request));
	}

	private static Postadresse mapPostadresse(DistribuerJournalpostRequestTo.AdresseTo adresseTo) {
		if (adresseTo == null) {
			return null;
		}

		return new Postadresse(
				adresseTo.getAdressetype(),
				adresseTo.getPostnummer(),
				adresseTo.getPoststed(),
				trimNotBlankOrNull(adresseTo.getAdresselinje1()),
				trimNotBlankOrNull(adresseTo.getAdresselinje2()),
				trimNotBlankOrNull(adresseTo.getAdresselinje3()),
				adresseTo.getLand());
	}

	private static TvingKanal mapTvingKanal(String tvingKanal) {
		return tvingKanal == null ? null : TvingKanal.valueOf(tvingKanal);
	}

	private static String trimNotBlankOrNull(String adresselinje) {
		return isBlank(adresselinje) ? null : adresselinje.trim();
	}

	private static String encodeToBase64(String forsendelseMetadata) {
		return isBlank(forsendelseMetadata) ? null : Base64.getEncoder().encodeToString(forsendelseMetadata.getBytes());
	}

	private static ForsendelseMetadataType mapForsendelseMetadataType(DistribuerJournalpostRequestTo distribuerJournalpost) {
		return distribuerJournalpost.getForsendelseMetadataType() == null ? null : ForsendelseMetadataType.valueOf(distribuerJournalpost.getForsendelseMetadataType());
	}
}