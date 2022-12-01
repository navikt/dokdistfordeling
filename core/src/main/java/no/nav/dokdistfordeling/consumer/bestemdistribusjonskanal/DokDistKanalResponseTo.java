package no.nav.dokdistfordeling.consumer.bestemdistribusjonskanal;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class DokDistKanalResponseTo {
	String distribusjonsKanal;
}
