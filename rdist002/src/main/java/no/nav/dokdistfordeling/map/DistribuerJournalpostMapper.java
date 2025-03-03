package no.nav.dokdistfordeling.map;

import no.nav.dokdistfordeling.domain.Adresse;
import no.nav.dokdistfordeling.domain.DistribuerJournalpost;
import no.nav.dokdistfordeling.kodeverk.DistribusjonstidspunktCode;
import no.nav.dokdistfordeling.kodeverk.DistribusjonstypeCode;
import no.nav.dokdistfordeling.kodeverk.TvingKanal;
import no.nav.dokdistfordeling.to.DistribuerJournalpostRequestTo;

public class DistribuerJournalpostMapper {

	private DistribuerJournalpostMapper() {
	}

	public static DistribuerJournalpost map(DistribuerJournalpostRequestTo request) {
		return new DistribuerJournalpost(
				request.getJournalpostId(),
				request.getBatchId(),
				request.getBestillendeFagsystem(),
				mapAdresse(request.getAdresse()),
				request.getDokumentProdApp(),
				DistribusjonstypeCode.valueOf(request.getDistribusjonstype()),
				DistribusjonstidspunktCode.valueOf(request.getDistribusjonstidspunkt()),
				request.isTvingSentralPrint(),
				mapTvingKanal(request.getTvingKanal()));
	}

	private static Adresse mapAdresse(DistribuerJournalpostRequestTo.AdresseTo adresseTo) {
		if (adresseTo == null) {
			return null;
		}

		return new Adresse(
				adresseTo.getAdressetype(),
				adresseTo.getPostnummer(),
				adresseTo.getPoststed(),
				trimOrNull(adresseTo.getAdresselinje1()),
				trimOrNull(adresseTo.getAdresselinje2()),
				trimOrNull(adresseTo.getAdresselinje3()),
				adresseTo.getLand());
	}

	private static TvingKanal mapTvingKanal(String tvingKanal) {
		return tvingKanal == null ? null : TvingKanal.valueOf(tvingKanal);
	}

	private static String trimOrNull(String s) {
		return s == null ? null : s.trim();
	}
}