package no.nav.dokdistfordeling.consumer.bestemdistribusjonskanal;

import no.nav.dokdistfordeling.kodeverk.DistribusjonsKanalCode;
import no.nav.dokdistfordeling.kodeverk.AktoerTypeCode;
import org.springframework.stereotype.Component;

/**
 * @author Sigurd Midttun, Visma Consulting.
 */
@Component
public class BestemDistribusjonskanalImpl implements BestemDistribusjonskanal {

	private BestemDokdistkanalRestConsumer bestemDokdistkanalRestConsumer;

	public BestemDistribusjonskanalImpl(BestemDokdistkanalRestConsumer bestemDokdistkanalRestConsumer) {
		this.bestemDokdistkanalRestConsumer = bestemDokdistkanalRestConsumer;
	}

	public DistribusjonsKanalCode bestemKanal(String mottakerId, String dokumentTypeId, AktoerTypeCode aktoerTypeCode, String brukerId) {
		return bestemDokdistkanalRestConsumer.bestemKanal(DokDistKanalRequestTo.builder()
				.mottakerId(mottakerId)
				.dokumentTypeId(dokumentTypeId)
				.mottakerType(aktoerTypeCode)
				.brukerId(brukerId)
				.build());

	}
}
