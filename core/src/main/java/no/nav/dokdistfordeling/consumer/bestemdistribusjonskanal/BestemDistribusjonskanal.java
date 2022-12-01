package no.nav.dokdistfordeling.consumer.bestemdistribusjonskanal;

import no.nav.dokdistfordeling.kodeverk.DistribusjonsKanalCode;

public interface BestemDistribusjonskanal {

	DistribusjonsKanalCode bestemKanal(DokDistKanalRequest dokDistKanalRequest);

}
