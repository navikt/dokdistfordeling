package no.nav.dokdistfordeling.consumer.bestemdistribusjonskanal;

import lombok.Builder;
import no.nav.dokdistfordeling.kodeverk.DistribusjonKanalCode;

@Builder
public record BestemDistribusjonskanalResponse (DistribusjonKanalCode distribusjonskanal,
												String regel,
												String regelBegrunnelse){
}
