package no.nav.dokdistfordeling.consumer.bestemdistribusjonskanal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author Sigurd Midttun, Visma Consulting.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class DokDistKanalResponseTo {
	private String distribusjonsKanal;
}
