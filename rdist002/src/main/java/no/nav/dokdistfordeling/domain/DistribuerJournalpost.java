package no.nav.dokdistfordeling.domain;

import lombok.Builder;
import no.nav.dokdistfordeling.kodeverk.DistribusjonstidspunktCode;
import no.nav.dokdistfordeling.kodeverk.DistribusjonstypeCode;
import no.nav.dokdistfordeling.kodeverk.ForsendelseMetadataType;
import no.nav.dokdistfordeling.kodeverk.TvingKanal;

@Builder
public record DistribuerJournalpost(
		Long journalpostId,
		String batchId,
		String bestillendeFagsystem,
		Postadresse postadresse,
		String dokumentProdApp,
		DistribusjonstypeCode distribusjonstype,
		DistribusjonstidspunktCode distribusjonstidspunkt,
		boolean tvingSentralPrint,
		TvingKanal tvingKanal,
		String forsendelseMetadata,
		ForsendelseMetadataType forsendelseMetadataType) {

	public boolean harPostadresse() {
		return this.postadresse != null;
	}
}
