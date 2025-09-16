package no.nav.dokdistfordeling;

import lombok.extern.slf4j.Slf4j;
import no.nav.dokdistfordeling.consumer.dokdistkanal.BestemDistribusjonskanalRequest;
import no.nav.dokdistfordeling.consumer.dokdistkanal.DokdistkanalConsumer;
import no.nav.dokdistfordeling.consumer.saf.journalpost.Journalpost;
import no.nav.dokdistfordeling.domain.DistribuerJournalpost;
import no.nav.dokdistfordeling.kodeverk.DistribusjonKanalCode;
import no.nav.dokdistfordeling.kodeverk.TvingKanal;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;

import static no.nav.dokdistfordeling.constants.Constants.DEFAULT_UTGAAENDE_DOKUMENTTYPE_ID;
import static no.nav.dokdistfordeling.kodeverk.DistribusjonKanalCode.PRINT;
import static no.nav.dokdistfordeling.kodeverk.DistribusjonKanalCode.TRYGDERETTEN;
import static no.nav.dokdistfordeling.kodeverk.Variantformat.SLADDET;
import static org.apache.commons.lang3.StringUtils.capitalize;
import static org.apache.logging.log4j.util.Strings.isBlank;
import static org.apache.logging.log4j.util.Strings.isEmpty;

@Slf4j
@Component
public class BestemDistribusjonskanalService {

	private final DokdistkanalConsumer dokdistkanalConsumer;

	public BestemDistribusjonskanalService(DokdistkanalConsumer dokdistkanalConsumer) {
		this.dokdistkanalConsumer = dokdistkanalConsumer;
	}

	public DistribusjonKanalCode bestemDistribusjonskanal(DistribuerJournalpost distribuerJournalpost,
														  Journalpost journalpost,
														  String personnummer) {

		DistribusjonKanalCode tvingKanal = utledTvingKanal(distribuerJournalpost);

		if (tvingKanal != null) {
			return tvingKanal;
		}

		if (personnummer == null) {
			return PRINT;
		}

		BestemDistribusjonskanalRequest request = BestemDistribusjonskanalRequest.builder()
				.dokumenttypeId(DEFAULT_UTGAAENDE_DOKUMENTTYPE_ID)
				.brukerId(personnummer)
				.mottakerId(utledMottakerId(journalpost.getAvsenderMottaker().getId()))
				.erArkivert(true)
				.tema(journalpost.getTema())
				.forsendelseStoerrelse(getFilstoerrelseMB(journalpost))
				.antallDokumenter(journalpost.getDokumenter().size())
				.build();

		BestemDistribusjonskanalRequest bestemDistribusjonskanalRequest =
				isBlank(distribuerJournalpost.forsendelseMetadataType()) ? request : request.withForsendelseMetadataType(distribuerJournalpost.forsendelseMetadataType());

		return dokdistkanalConsumer.bestemDistribusjonskanal(bestemDistribusjonskanalRequest);
	}

	private static DistribusjonKanalCode utledTvingKanal(DistribuerJournalpost distribuerJournalpost) {
		boolean tvingSentralPrint = distribuerJournalpost.tvingSentralPrint();
		TvingKanal tvingKanal = distribuerJournalpost.tvingKanal();

		if (tvingSentralPrint) {
			log.info("tvingSentralPrint er satt til true i input. Forsendelsen vil bli sendt til print.");
			return PRINT;
		}

		if (tvingKanal != null) {
			log.info("tvingKanal er satt til {} i input. Forsendelsen vil bli sendt til {}.", tvingKanal, capitalize(tvingKanal.name()));

			return switch (tvingKanal) {
				case TvingKanal.PRINT -> PRINT;
				case TvingKanal.TRYGDERETTEN -> TRYGDERETTEN;
			};
		}
		return null;
	}

	private String utledMottakerId(String mottakerId) {
		return isEmpty(mottakerId) ? "0" : mottakerId;
	}

	private int getFilstoerrelseMB(Journalpost journalpost) {
		return journalpost.getDokumenter().stream()
				.map(Journalpost.DokumentInfo::getDokumentvarianter)
				.map(BestemDistribusjonskanalService::getSladdetOrArkivFilstoerrelse)
				.mapToInt(Integer::intValue)
				.sum() / (1024 * 1024);
	}

	private static Integer getSladdetOrArkivFilstoerrelse(List<Journalpost.Dokumentvariant> dokumentvariants) {
		return dokumentvariants.stream()
				.sorted(sortSladdetFirstComparator)
				.map(Journalpost.Dokumentvariant::getFilstoerrelse)
				.findFirst()
				.orElse(0);
	}

	private static final Comparator<Journalpost.Dokumentvariant> sortSladdetFirstComparator = (dokA, dokB) -> {
		if (dokA.getVariantformat() == dokB.getVariantformat()) {
			return 0;
		} else if (dokA.getVariantformat() == SLADDET) {
			return -1;
		} else {
			return 1;
		}
	};
}
