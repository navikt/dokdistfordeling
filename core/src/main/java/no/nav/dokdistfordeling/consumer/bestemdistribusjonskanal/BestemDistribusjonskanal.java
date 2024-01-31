package no.nav.dokdistfordeling.consumer.bestemdistribusjonskanal;

import no.nav.dokdistfordeling.kodeverk.DistribusjonKanalCode;

public interface BestemDistribusjonskanal {

	DistribusjonKanalCode bestemKanal(DokDistKanalRequest dokDistKanalRequest);

}
