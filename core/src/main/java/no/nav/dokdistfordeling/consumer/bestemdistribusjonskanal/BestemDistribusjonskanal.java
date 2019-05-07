package no.nav.dokdistfordeling.consumer.bestemdistribusjonskanal;

import no.nav.dokdistfordeling.kodeverk.DistribusjonsKanalCode;

/**
 * @author Sigurd Midttun, Visma Consulting.
 */
public interface BestemDistribusjonskanal {

	DistribusjonsKanalCode bestemKanal(DokDistKanalRequest dokDistKanalRequest);

}
