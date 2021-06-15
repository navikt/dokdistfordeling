package no.nav.dokdistfordeling.consumer.bestemdistribusjonskanal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

/**
 * @author Sigurd Midttun, Visma Consulting.
 */
@Data
@Builder
@AllArgsConstructor
public class DokDistKanalRequest {
	private String mottakerId;
	private String dokumentTypeId;
	private String mottakerType;
	private String brukerId;
	private boolean erArkivert;
	private String tema;
}
