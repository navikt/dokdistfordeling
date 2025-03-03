package no.nav.dokdistfordeling.domain;

import lombok.Builder;
import no.nav.dokdistfordeling.kodeverk.DistribusjonstidspunktCode;
import no.nav.dokdistfordeling.kodeverk.DistribusjonstypeCode;
import no.nav.dokdistfordeling.kodeverk.TvingKanal;

@Builder
public record DistribuerJournalpost(String journalpostId,
									String batchId,
									String bestillendeFagsystem,
									Adresse adresse,
									String dokumentProdApp,
									DistribusjonstypeCode distribusjonstype,
									DistribusjonstidspunktCode distribusjonstidspunkt,
									boolean tvingSentralPrint,
									TvingKanal tvingKanal) {
}
