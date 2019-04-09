package no.nav.dokdistfordeling.consumer.bestemdistribusjonskanal;

import no.nav.dokdistfordeling.kodeverk.DistribusjonsKanalCode;
import no.nav.dokdistfordeling.kodeverk.AktoerTypeCode;

/**
 * @author Sigurd Midttun, Visma Consulting.
 */
public interface BestemDistribusjonskanal {

	DistribusjonsKanalCode bestemKanal(String mottakerId, String dokumentTypeId, AktoerTypeCode aktoerTypeCode, String brukerId);

}
