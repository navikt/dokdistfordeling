package no.nav.dokdistfordeling;

import lombok.extern.slf4j.Slf4j;
import no.nav.dokdistfordeling.consumer.bestemdistribusjonskanal.BestemDokdistkanalRestConsumer;
import no.nav.dokdistfordeling.consumer.bestemdistribusjonskanal.DokDistKanalRequest;
import no.nav.dokdistfordeling.consumer.pdl.PdlGraphQLConsumer;
import no.nav.dokdistfordeling.consumer.saf.journalpost.Journalpost;
import no.nav.dokdistfordeling.exception.functional.PdlHentFolkeregisteridentForAktoerIdFunctionalException;
import no.nav.dokdistfordeling.kodeverk.DistribusjonsKanalCode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static no.nav.dokdistfordeling.constants.Constants.DEFAULT_UTGAAENDE_DOKUMENTTYPE_ID;
import static no.nav.dokdistfordeling.kodeverk.BrukerIdType.AKTOERID;
import static no.nav.dokdistfordeling.kodeverk.DistribusjonsKanalCode.PRINT;
import static org.apache.logging.log4j.util.Strings.isEmpty;

@Slf4j
@Component
public class HentBestemDokdistKanalService {

	private static final String ORGANISASJON = "ORGANISASJON";
	private static final String PERSON = "PERSON";
	private static final String SAMHANDLER_PREFIX = "SAMHANDLER";
	private static final String SAMHANDLER_UKJENT = "SAMHANDLER_UKJENT";

	private final BestemDokdistkanalRestConsumer bestemDokdistkanal;
	private final PdlGraphQLConsumer pdlGraphQLConsumer;

	@Autowired
	public HentBestemDokdistKanalService(BestemDokdistkanalRestConsumer bestemDokdistkanalRestConsumer, PdlGraphQLConsumer pdlGraphQLConsumer) {
		this.bestemDokdistkanal = bestemDokdistkanalRestConsumer;
		this.pdlGraphQLConsumer = pdlGraphQLConsumer;
	}

	public DistribusjonsKanalCode bestemDistribusjonskanal(Journalpost journalpost, boolean harAdresse) {
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
				.mottakerType(getMottakerType(journalpost.getAvsenderMottaker()))
				.erArkivert(true)
				.tema(journalpost.getTema())
				.build();

		return bestemDokdistkanal.bestemKanal(request);
	}

	private String determineMottakerId(String mottakerId){
		return isEmpty(mottakerId) ? "0" : mottakerId;
	}

	private String hentIdent(Journalpost.Bruker bruker) {
		return AKTOERID.equals(bruker.getType()) ? pdlGraphQLConsumer.hentFolkeregisteridentForAktoerId(bruker.getId()) : bruker.getId();
	}

	private String getMottakerType(Journalpost.AvsenderMottaker avsenderMottaker) {
		if(avsenderMottaker.getType() == null || isEmpty(avsenderMottaker.getType().name())){
			return SAMHANDLER_UKJENT;
		}
		return switch (avsenderMottaker.getType()) {
			case FNR -> PERSON;
			case ORGNR -> ORGANISASJON;
			case UKJENT -> SAMHANDLER_UKJENT;
			default -> SAMHANDLER_PREFIX + "_" + avsenderMottaker.getType();
		};
	}
}
