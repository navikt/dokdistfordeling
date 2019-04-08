package no.nav.dokdistfordeling.consumer.bestemdistribusjonskanal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import no.nav.dokdistfordeling.kodeverk.AktoerTypeCode;

/**
 * @author Sigurd Midttun, Visma Consulting.
 */
@Data
@AllArgsConstructor
@Builder
public class DokDistKanalRequestTo {
	private String mottakerId;
	private String dokumentTypeId;
	private AktoerTypeCode mottakerType;
	private String brukerId;
}
