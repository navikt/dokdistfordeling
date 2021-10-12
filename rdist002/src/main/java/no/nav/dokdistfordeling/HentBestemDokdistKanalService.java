package no.nav.dokdistfordeling;

import lombok.extern.slf4j.Slf4j;
import no.nav.dokdistfordeling.consumer.bestemdistribusjonskanal.BestemDokdistkanalRestConsumer;
import no.nav.dokdistfordeling.consumer.bestemdistribusjonskanal.DokDistKanalRequest;
import no.nav.dokdistfordeling.consumer.pdl.PdlGraphQLConsumer;
import no.nav.dokdistfordeling.consumer.saf.journalpost.Journalpost;
import no.nav.dokdistfordeling.kodeverk.DistribusjonsKanalCode;
import org.springframework.stereotype.Component;

import javax.inject.Inject;

import static no.nav.dokdistfordeling.constants.Constants.DEFAULT_UTGAAENDE_DOKUMENTTYPE_ID;
import static no.nav.dokdistfordeling.kodeverk.AvsenderMottakerIdType.FNR;
import static no.nav.dokdistfordeling.kodeverk.AvsenderMottakerIdType.ORGNR;
import static no.nav.dokdistfordeling.kodeverk.BrukerIdType.AKTOERID;

@Slf4j
@Component
public class HentBestemDokdistKanalService {

	private static final String ORGANISASJON = "ORGANISASJON";
	private static final String PERSON = "PERSON";
	private static final String SAMHANDLER = "SAMHANDLER";

	private BestemDokdistkanalRestConsumer bestemDokdistkanal;
	private PdlGraphQLConsumer pdlGraphQLConsumer;

	@Inject
	public HentBestemDokdistKanalService(BestemDokdistkanalRestConsumer bestemDokdistkanalRestConsumer, PdlGraphQLConsumer pdlGraphQLConsumer) {
		this.bestemDokdistkanal = bestemDokdistkanalRestConsumer;
		this.pdlGraphQLConsumer = pdlGraphQLConsumer;
	}

	public DistribusjonsKanalCode bestemDistribusjonskanal(Journalpost journalpost) {
		String personnummer = hentIdent(journalpost.getBruker());

		DokDistKanalRequest request = DokDistKanalRequest.builder()
				.dokumentTypeId(DEFAULT_UTGAAENDE_DOKUMENTTYPE_ID)
				.brukerId(personnummer)
				.mottakerId(journalpost.getAvsenderMottaker().getId())
				.mottakerType(getMottakerType(journalpost.getAvsenderMottaker()))
				.erArkivert(true)
				.tema(journalpost.getTema())
				.build();


		return bestemDokdistkanal.bestemKanal(request);

	}

	private String hentIdent(Journalpost.Bruker bruker) {
		return AKTOERID.equals(bruker.getType()) ? pdlGraphQLConsumer.hentFolkeregisteridentForAktoerId(bruker.getId()) : bruker.getId();
	}

	private String getMottakerType(Journalpost.AvsenderMottaker avsenderMottaker) {
		if (FNR.equals(avsenderMottaker.getType()) || ORGNR.equals(avsenderMottaker.getType())) {
			return FNR.equals(avsenderMottaker.getType()) ? PERSON : ORGANISASJON;
		} else {
			return SAMHANDLER;
		}
	}
}
