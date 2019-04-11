package no.nav.dokdistfordeling.consumer.bestemdistribusjonskanal;

import lombok.Builder;
import lombok.Value;

/**
 * @author Sigurd Midttun, Visma Consulting.
 */
@Value
@Builder
public class DokDistKanalResponseTo {
	private String distribusjonsKanal;
}
