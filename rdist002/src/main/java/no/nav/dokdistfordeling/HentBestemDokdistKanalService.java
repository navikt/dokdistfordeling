package no.nav.dokdistfordeling;

import lombok.extern.slf4j.Slf4j;
import no.nav.dokdistfordeling.consumer.bestemdistribusjonskanal.BestemDokdistkanalRestConsumer;
import no.nav.dokdistfordeling.consumer.bestemdistribusjonskanal.DokDistKanalRequest;
import no.nav.dokdistfordeling.consumer.pdl.PdlGraphQLConsumer;
import no.nav.dokdistfordeling.consumer.saf.journalpost.Journalpost;
import no.nav.dokdistfordeling.exception.functional.PdlHentFolkeregisteridentForAktoerIdFunctionalException;
import no.nav.dokdistfordeling.kodeverk.DistribusjonKanalCode;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;

import static no.nav.dokdistfordeling.constants.Constants.DEFAULT_UTGAAENDE_DOKUMENTTYPE_ID;
import static no.nav.dokdistfordeling.kodeverk.BrukerIdType.AKTOERID;
import static no.nav.dokdistfordeling.kodeverk.DistribusjonKanalCode.PRINT;
import static no.nav.dokdistfordeling.kodeverk.Variantformat.SLADDET;
import static org.apache.logging.log4j.util.Strings.isEmpty;

@Slf4j
@Component
public class HentBestemDokdistKanalService {

	private final BestemDokdistkanalRestConsumer bestemDokdistkanal;
	private final PdlGraphQLConsumer pdlGraphQLConsumer;

	public HentBestemDokdistKanalService(BestemDokdistkanalRestConsumer bestemDokdistkanalRestConsumer, PdlGraphQLConsumer pdlGraphQLConsumer) {
		this.bestemDokdistkanal = bestemDokdistkanalRestConsumer;
		this.pdlGraphQLConsumer = pdlGraphQLConsumer;
	}

	public DistribusjonKanalCode bestemDistribusjonskanal(Journalpost journalpost, boolean harAdresse) {
		String personnummer;
		try {
			personnummer = hentIdent(journalpost.getBruker());
		} catch (PdlHentFolkeregisteridentForAktoerIdFunctionalException e) {
			if (harAdresse) {
				log.info("Returnerer PRINT som distribusjonskanal etter at mapping fra aktørid til fnr feilet for person med oppgitt adresse.");
				return PRINT;
			} else {
				throw e;
			}
		}

		DokDistKanalRequest request = DokDistKanalRequest.builder()
				.dokumentTypeId(DEFAULT_UTGAAENDE_DOKUMENTTYPE_ID)
				.brukerId(personnummer)
				.mottakerId(determineMottakerId(journalpost.getAvsenderMottaker().getId()))
				.erArkivert(true)
				.tema(journalpost.getTema())
				.forsendelseStoerrelse(getFilstoerrelseMB(journalpost))
				.build();

		return bestemDokdistkanal.bestemKanal(request);
	}

	private String determineMottakerId(String mottakerId) {
		return isEmpty(mottakerId) ? "0" : mottakerId;
	}

	private String hentIdent(Journalpost.Bruker bruker) {
		return AKTOERID.equals(bruker.getType()) ? pdlGraphQLConsumer.hentFolkeregisteridentForAktoerId(bruker.getId()) : bruker.getId();
	}

	private int getFilstoerrelseMB(Journalpost journalpost) {
		return journalpost.getDokumenter().stream()
				.map(Journalpost.DokumentInfo::getDokumentvarianter)
				.map(HentBestemDokdistKanalService::getSladdetOrArkivFilstoerrelse)
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

	private static Comparator<Journalpost.Dokumentvariant> sortSladdetFirstComparator = (dokA, dokB) -> {
		if (dokA.getVariantformat() == dokB.getVariantformat()) {
			return 0;
		} else if (dokA.getVariantformat() == SLADDET) {
			return -1;
		} else {
			return 1;
		}
	};
}
